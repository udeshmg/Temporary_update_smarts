package processor.communication.message;

import common.Settings;
import processor.server.NodeInfo;
import processor.server.RouteLoader;
import processor.server.WorkerMeta;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SerializableExternalVehicle {
	public boolean foreground;
	public String id;
	public double startTime;
	public String vehicleType;
	public String driverProfile;
	public double numberRepeatPerSecond;
	public ArrayList<SerializableRouteLeg> route = new ArrayList<>();

	public SerializableExternalVehicle() {

	}

	public SerializableExternalVehicle(final boolean foreground, final String id, final double startTime,
			final String vehicleType, final String driverProfile, final double repeatRate,
			final ArrayList<SerializableRouteLeg> route) {
		super();
		this.foreground = foreground;
		this.id = id;
		this.startTime = startTime;
		this.vehicleType = vehicleType;
		this.driverProfile = driverProfile;
		numberRepeatPerSecond = repeatRate;
		this.route = route;
	}

	public static SerializableExternalVehicle createFromString(String vehicle, RoadNetwork roadNetwork, ArrayList<NodeInfo> idMappers, RouteLoader.NodeIdComparator nodeIdComparator){
		final String[] fields = vehicle.split(Settings.delimiterItem);
		final boolean foreground = Boolean.parseBoolean(fields[0]);
		final String id = fields[1];
		final double start_time = Double.parseDouble(fields[2]);
		final String type = fields[3];
		final String driverProfile = fields[4];
		final double repeatRate = Double.parseDouble(fields[5]);
		final ArrayList<SerializableRouteLeg> route = getRouteFromString(fields[6], roadNetwork, idMappers, nodeIdComparator);
		return new SerializableExternalVehicle(foreground, id, start_time, type,
				driverProfile, repeatRate, route);
	}

	static ArrayList<SerializableRouteLeg> getRouteFromString(final String routeString, RoadNetwork roadNetwork, ArrayList<NodeInfo> idMappers, RouteLoader.NodeIdComparator nodeIdComparator) {

		final String[] routeLegs = routeString.split(Settings.delimiterSubItem);
		final ArrayList<SerializableRouteLeg> route = new ArrayList<>();
		for (int i = 0; i < (routeLegs.length - 1); i++) {
			final String[] currentLegDetails = routeLegs[i].split("#");
			final String[] nextLegDetails = routeLegs[i + 1].split("#");
			final long osmIdNd1 = Long.parseLong(currentLegDetails[0]);
			final int mapperIndexNd1 = Collections.binarySearch(idMappers, new NodeInfo(osmIdNd1, -1),
					nodeIdComparator);
			// Stop processing further if node cannot be found
			if (mapperIndexNd1 < 0) {
				System.out.println("Cannot find node: " + osmIdNd1 + ". ");
				break;
			}
			final int nodeIndexNd1 = idMappers.get(mapperIndexNd1).getIndex();
			final Node nd1 = roadNetwork.nodes.get(nodeIndexNd1);

			// Get the edge and add it to a list
			final long osmIdNd2 = Long.parseLong(nextLegDetails[0]);

			for (final Edge e : nd1.outwardEdges) {
				if (e.endNode.osmId == osmIdNd2) {
					route.add(new SerializableRouteLeg(e.index, Double.parseDouble(currentLegDetails[1])));
					break;
				}
			}
		}
		return route;
	}

}
