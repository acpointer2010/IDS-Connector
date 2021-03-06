package de.fhg.aisec.ids.cm.impl.docker;

import java.io.ByteArrayOutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.Decision;
import de.fhg.aisec.ids.api.cm.Direction;
import de.fhg.aisec.ids.api.cm.Protocol;
import de.fhg.aisec.ids.cm.impl.StreamGobbler;

/**
 * ContainerManager implementation for Docker containers.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
public class DockerCM implements ContainerManager {
	private static final Logger LOG = LoggerFactory.getLogger(DockerCM.class);
	private static final String DOCKER_CLI = "docker";	//Name of docker cli executable
			
	@Override
	public List<ApplicationContainer> list(boolean onlyRunning) {
		List<ApplicationContainer> result = new ArrayList<ApplicationContainer>();
		ByteArrayOutputStream bbErr = new ByteArrayOutputStream();
		ByteArrayOutputStream bbStd = new ByteArrayOutputStream();
		try {
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add(DOCKER_CLI);
			cmd.add("ps");
			cmd.add("--no-trunc");
			if (!onlyRunning) {
				cmd.add("--all");
			}
			cmd.add("--format");
			cmd.add("{{.ID}}@@{{.Image}}@@{{.CreatedAt}}@@{{.RunningFor}}@@{{.Ports}}@@{{.Status}}@@{{.Size}}@@{{.Names}}");
			
			ProcessBuilder pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(cmd);
			Process p = pb.start();
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), bbErr);
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), bbStd);
			errorGobbler.start();
			outputGobbler.start();
			p.waitFor(30, TimeUnit.SECONDS);
			errorGobbler.close();
			outputGobbler.close();
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}
		String[] lines = bbStd.toString().split("\n");
		for (String line:lines) {
			String[] columns = line.split("@@");
			if (columns.length!=8) {
				LOG.error("Unexpected number of columns in docker ps: " + columns.length + ": " + line);
				break;
			}
			String id = columns[0];
			String image = columns[1];
			String created = columns[2];
			String uptime = columns[3];
			String ports = columns[4];
			String status = columns[5];
			String size = columns[6];
			String names = columns[7];
			result.add(new ApplicationContainer(id, image, created, status, ports, names, size, uptime));
		}
		return result;
	}


	@Override
	public void wipe(final String containerID) {
		try {
			LOG.info("Wiping containerID " + containerID);
			ProcessBuilder pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "rm","-f", containerID));
			Process p = pb.start();
			p.waitFor(60, TimeUnit.SECONDS);

			LOG.info("Wiping image and containers related to containerID " + containerID);
			pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "rmi", "-f", "`docker ps -a --format \"{{.Image}}\" -f id="+containerID+"`"));
			p = pb.start();
			p.waitFor(60, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}
	}

	@Override
	public void startContainer(final String containerID) {
		try {
			ProcessBuilder pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "start", containerID));
			Process p = pb.start();
			p.waitFor(660, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}
	}

	@Override
	public void stopContainer(final String containerID) {
		try {
			ProcessBuilder pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "stop", containerID));
			Process p = pb.start();
			p.waitFor(660, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}
	}


	@Override
	public void restartContainer(final String containerID) {
		try {
			ProcessBuilder pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "restart", containerID));
			Process p = pb.start();
			p.waitFor(660, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}
	}

	@Override
	public Optional<String> pullImage(final String imageID) {
		try {
			// Pull image from std docker registry
			LOG.info("Pulling container image " + imageID);
			ProcessBuilder pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "pull", imageID));
			Process p = pb.start();
			p.waitFor(600, TimeUnit.SECONDS);

			// Instantly create a container from that image, but do not start it yet.
			LOG.info("Creating container instance from image " + imageID);
			String containerID = defaultContainerName(imageID);
			pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "create", "-P", "--label", "created="+Instant.now().toEpochMilli(), "--name", containerID, imageID));
			p = pb.start();
			p.waitFor(600, TimeUnit.SECONDS);
			return Optional.<String>of(containerID);
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}
		return Optional.<String>empty();
	}

	/**
	 * Returns the default containerName that will be given to a container of image "imageID".
	 * 
	 * For example, an imageID "shiva1029/weather" will be turned into "weather-shiva1029".
	 * 
	 * @param imageID
	 * @return
	 */
	private String defaultContainerName(String imageID) {
		if (imageID.indexOf('/') > -1 && imageID.indexOf('/')<imageID.length()-1) {
			String name = imageID.substring(imageID.indexOf('/')+1);
			String rest = imageID.replace(name, "").replace('/', '-');
			rest = rest.substring(0, rest.length()-2);
			return name + "-" + rest;
		} else {
			return imageID;
		}
	}


	/**
	 * Returns true if Docker is supported.
	 * 
	 * @return
	 */
	public static boolean isSupported() {
		try {
			//Attempt to invoke some docker command. If it fails, return false
			Process p = new ProcessBuilder().inheritIO().command(Arrays.asList(DOCKER_CLI, "info")).start();
			p.waitFor(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
			return false;
		}		
		return true;
	}

	@Override
	public String getMetadata(String containerID) {
		StringBuilder sb = new StringBuilder();
		ByteArrayOutputStream bbErr = new ByteArrayOutputStream();
		ByteArrayOutputStream bbStd = new ByteArrayOutputStream();
		try {
			ProcessBuilder pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "inspect", containerID));
			Process p = pb.start();
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), bbErr);
			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), bbStd);
			errorGobbler.start();
			outputGobbler.start();
			p.waitFor(30, TimeUnit.SECONDS);
			errorGobbler.close();
			outputGobbler.close();
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}

		// Parse JSON output from "docker inspect" and return labels.
		String[] lines = bbStd.toString().split("\n");
		for (String line:lines) {
			System.out.println(line);
		}
		return sb.toString();
	}

	@Override
	public void setIpRule(String containerID, Direction direction, int srcPort, int dstPort, String srcDstRange,
			Protocol protocol, Decision decision) {
		// TODO Not implemented yet
		
	}

	@Override
	public String inspectContainer(final String containerID) {
		//TODO Implement me.
		return null;
	}

//
//	@Override
//	public String getCml() {
//		StringBuilder sb = new StringBuilder();
//		ByteArrayOutputStream bbErr = new ByteArrayOutputStream();
//		ByteArrayOutputStream bbStd = new ByteArrayOutputStream();
//		try {
//			ProcessBuilder pb = new ProcessBuilder().redirectInput(Redirect.INHERIT).command(Arrays.asList(DOCKER_CLI, "--version"));
//			Process p = pb.start();
//			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), bbErr);
//			StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), bbStd);
//			errorGobbler.start();
//			outputGobbler.start();
//			p.waitFor(30, TimeUnit.SECONDS);
//			errorGobbler.close();
//			outputGobbler.close();
//		} catch (Exception e) {
//			LOG.error(e.getMessage(),e);
//		}
//
//		// Parse JSON output from "docker --version" and return labels.
//		String[] lines = bbStd.toString().split("\n");
//		for (String line:lines) {
//			System.out.println(line);
//		}
//		return sb.toString();
//	}

}
