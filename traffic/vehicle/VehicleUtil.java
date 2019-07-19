package traffic.vehicle;

import common.Settings;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadType;

/**
 * This class finds impeding objects based on various factors, e.g., traffic
 * lights, front vehicles, conflicting traffic at intersection, etc.
 */
public class VehicleUtil {

	/**
	 * Compute the GPS coordinates of the head and end of a given vehicle
	 */
	public static double[] calculateCoordinates(final Vehicle v) {
		final double headToEdgeRatio = v.headPosition / v.lane.edge.length;
		final double tailToEdgeRatio = (v.headPosition - v.length) / v.lane.edge.length;

		final double lonLength = v.lane.lonEnd - v.lane.lonStart;
		final double latLength = v.lane.latEnd - v.lane.latStart;

		final double headLon = v.lane.lonStart + (headToEdgeRatio * lonLength);
		final double headLat = v.lane.latStart + (headToEdgeRatio * latLength);
		final double tailLon = v.lane.lonStart + (tailToEdgeRatio * lonLength);
		final double tailLat = v.lane.latStart + (tailToEdgeRatio * latLength);

		final double[] coords = { headLon, headLat, tailLon, tailLat };
		return coords;
	}






	/**
	 * Check whether a vehicle can travel from one node to another through an
	 * edge
	 */
	public static boolean canGoThrough(final Node nodeStart, final Node nodeEnd, final VehicleType vehicleType) {
		for (final Edge e : nodeStart.outwardEdges) {
			if (e.endNode == nodeEnd) {
				if (e.type == RoadType.tram) {
					if ((vehicleType == VehicleType.PRIORITY) && !Settings.isAllowPriorityVehicleUseTramTrack) {
						return false;
					} else if ((vehicleType != VehicleType.PRIORITY) && (vehicleType != VehicleType.TRAM)) {
						return false;
					}
				}
				if (!e.isBlocked()) {
					return true;
				}
			}
		}
		return false;
	}


	/**
	 * Get the braking distance for stopping a vehicle completely.
	 */
	public static double getBrakingDistance(final Vehicle vehicle) {
		return (vehicle.speed * vehicle.speed) / 2.0 / vehicle.driverProfile.IDM_b;
	}





}
