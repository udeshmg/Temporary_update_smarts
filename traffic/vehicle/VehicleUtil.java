package traffic.vehicle;

import java.awt.geom.Line2D;

import common.Settings;
import traffic.light.LightColor;
import traffic.light.TrafficLightTiming;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.road.RoadType;
import traffic.road.RoadUtil;
import traffic.routing.RouteLeg;
import traffic.vehicle.SlowdownFactor;

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

		final double headLon = v.lane.lonStart + (headToEdgeRatio * v.lane.lonLength);
		final double headLat = v.lane.latStart + (headToEdgeRatio * v.lane.latLength);
		final double tailLon = v.lane.lonStart + (tailToEdgeRatio * v.lane.lonLength);
		final double tailLat = v.lane.latStart + (tailToEdgeRatio * v.lane.latLength);

		final double[] coords = { headLon, headLat, tailLon, tailLat };
		return coords;
	}



	public static double getAverageSpeedOfTrip(final Vehicle vehicle) {
		// Get total road length in the trip
		double length = 0;
		for (final RouteLeg leg : vehicle.routeLegs) {
			length += leg.edge.length;
		}
		// Return average speed
		return (length / vehicle.timeTravel) * 3.6;
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

	/**
	 * Set or cancel priority lanes within a certain distance.
	 */
	public static void setPriorityLanes(final Vehicle vehicle, final boolean isPriority) {
		double examinedDist = 0;
		int indexLegOnRoute = vehicle.indexLegOnRoute;
		int laneNumber = vehicle.lane.laneNumber;
		Edge edge = vehicle.lane.edge;
		while ((examinedDist < Settings.lookAheadDistance) && (indexLegOnRoute < (vehicle.routeLegs.size() - 1))) {
			final Edge targetEdge = vehicle.routeLegs.get(indexLegOnRoute).edge;
			if (!isPriority) {
				// Cancel priority status for all the lanes in the edge
				for (Lane lane : targetEdge.getLanes()) {
					lane.isPriority = false;
				}
			} else {
				// Set priority for the lane that will be used by the vehicle
				laneNumber = RoadUtil.getLaneNumberForTargetEdge(targetEdge, edge, laneNumber);
				targetEdge.getLane(laneNumber).isPriority = true;
			}
			examinedDist += targetEdge.length;
			indexLegOnRoute++;
			edge = targetEdge;
		}
	}



}
