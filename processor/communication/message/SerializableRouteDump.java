package processor.communication.message;

import java.util.ArrayList;

public class SerializableRouteDump {
	public String vehicleId = "";
	public int vid;
	public String type = "";
	public double startTime;
	public ArrayList<SerializableRouteDumpPoint> routeDumpPoints = new ArrayList<>();
	public String driverProfile = "";

	public SerializableRouteDump() {

	}

	public SerializableRouteDump(final String vehicleId, final int vid, final String type, final double startTime,
			final ArrayList<SerializableRouteDumpPoint> routeDumpPoints, final String driverProfile) {
		super();
		this.vehicleId = vehicleId;
		this.vid = vid;
		this.type = type;
		this.startTime = startTime;
		this.routeDumpPoints = routeDumpPoints;
		this.driverProfile = driverProfile;
	}

}
