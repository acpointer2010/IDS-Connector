package de.fhg.aisec.ids.cm;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.aisec.ids.api.cm.ApplicationContainer;
import de.fhg.aisec.ids.api.cm.ContainerManager;
import de.fhg.aisec.ids.api.cm.Decision;
import de.fhg.aisec.ids.api.cm.Direction;
import de.fhg.aisec.ids.api.cm.NoContainerExistsException;
import de.fhg.aisec.ids.api.cm.Protocol;
import de.fhg.aisec.ids.cm.impl.docker.DockerCM;
import de.fhg.aisec.ids.cm.impl.trustx.TrustXCM;

/**
 * Main entry point of the Container Management Layer.
 * 
 * This class is mainly a facade for the actual CML implementation, which can either be Docker or trust-X.
 * 
 * @author Julian Schütte (julian.schuette@aisec.fraunhofer.de)
 *
 */
@Component(enabled=true, immediate=true, name="ids-cml")

public class ContainerManagerService implements ContainerManager {
	private static final Logger LOG = LoggerFactory.getLogger(ContainerManagerService.class);
	private ContainerManager containerManager = null;

	@Activate
	protected void activate() {
		LOG.info("Activating Container Manager");
		// When activated, try to set container management instance
		Optional<ContainerManager> cm = getDefaultCM();
		if (cm.isPresent()) {
			LOG.info("Default container management is " + cm.get());
			containerManager = cm.get();
		} else {
			LOG.info("There is no supported container management");
		}
		
	}
	
	@Deactivate
	protected void deactivate(ComponentContext cContext, Map<String, Object> properties) {
		containerManager = null;		
	}
	
	
	private Optional<ContainerManager> getDefaultCM() {
		Optional<ContainerManager> result = Optional.<ContainerManager>empty();
		if (TrustXCM.isSupported()) {
			result = Optional.of(new TrustXCM());
		} else if (DockerCM.isSupported()) {
			result = Optional.of(new DockerCM());
		} else {
			LOG.warn("No supported container management layer found");
		}
		return result;
	}

	@Override
	public List<ApplicationContainer> list(boolean onlyRunning) {
		return containerManager.list(onlyRunning);
	}

	@Override
	public void wipe(String containerID) {
		try {
			containerManager.wipe(containerID);
		} catch (NoContainerExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void startContainer(String containerID) {
		try {
			containerManager.startContainer(containerID);
		} catch (NoContainerExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stopContainer(String containerID) {
		try {
			containerManager.stopContainer(containerID);
		} catch (NoContainerExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void restartContainer(String containerID) {
		try {
			containerManager.restartContainer(containerID);
		} catch (NoContainerExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Optional<String> pullImage(String imageID) {
		try {
			return containerManager.pullImage(imageID);
		} catch (NoContainerExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String inspectContainer(String containerID) {
		try {
			return containerManager.inspectContainer(containerID);
		} catch (NoContainerExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public Object getMetadata(String containerID) {
		try {
			return containerManager.getMetadata(containerID);
		} catch (NoContainerExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void setIpRule(String containerID, Direction direction, int srcPort, int dstPort, String srcDstRange,
			Protocol protocol, Decision decision) {
		containerManager.setIpRule(containerID, direction, srcPort, dstPort, srcDstRange, protocol, decision);
	}
}
