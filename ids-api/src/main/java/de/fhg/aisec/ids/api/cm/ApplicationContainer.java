package de.fhg.aisec.ids.api.cm;

/**
 * Bean representing an "Application Container" (aka a docker container).
 * 
 * @author julian.schuette@aisec.fraunhofer.de
 *
 */
public class ApplicationContainer {
	private String id;
	private String image;
	private String created;
	private String status;
	private String ports;
	private String names;
	private String size;
	private String uptime;
	
	public ApplicationContainer(String id, String image, String created, String status, String ports,
			String names, String size, String uptime) {
		super();
		this.id = id;
		this.image = image;
		this.created = created;
		this.status = status;
		this.ports = ports;
		this.names = names;
		this.size = size;
		this.uptime = uptime;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getUptime() {
		return uptime;
	}

	public void setUptime(String uptime) {
		this.uptime = uptime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPorts() {
		return ports;
	}

	public void setPorts(String ports) {
		this.ports = ports;
	}

	public String getNames() {
		return names;
	}

	public void setNames(String names) {
		this.names = names;
	}

	@Override
	public String toString() {
		return "ApplicationContainer [id=" + id + ", image=" + image + ", created=" + created + ", status=" + status
				+ ", ports=" + ports + ", names=" + names + ", size=" + size + ", uptime=" + uptime + "]";
	}

	
	
}
