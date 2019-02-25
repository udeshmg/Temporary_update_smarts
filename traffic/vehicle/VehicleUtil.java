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

	public static void findEdgeBeforeNextTurn(final Vehicle vehicle) {
		double examinedDist = 0;
		vehicle.edgeBeforeTurnLeft = null;
		vehicle.edgeBeforeTurnRight = null;
		int indexLegOnRouteBeingChecked = vehicle.indexLegOnRoute;
		while (indexLegOnRouteBeingChecked < (vehicle.routeLegs.size() - 1)) {
			final Edge e1 = vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge;
			final Edge e2 = vehicle.routeLegs.get(indexLegOnRouteBeingChecked + 1).edge;

			if (e1.startNode == e2.endNode) {
				// Vehicle is going to make U-turn
				vehicle.edgeBeforeTurnRight = e1;
			} else if (!e1.name.equals(e2.name) || (e1.type != e2.type)) {
				final Line2D.Double e1Seg = new Line2D.Double(e1.startNode.lon, e1.startNode.lat * Settings.lonVsLat,
						e1.endNode.lon, e1.endNode.lat * Settings.lonVsLat);
				final int ccw = e1Seg.relativeCCW(e2.endNode.lon, e2.endNode.lat * Settings.lonVsLat);
				if (ccw < 0) {
					vehicle.edgeBeforeTurnLeft = e1;
				} else if (ccw > 0) {
					vehicle.edgeBeforeTurnRight = e1;
				}
			}

			if ((vehicle.edgeBeforeTurnLeft != null) || (vehicle.edgeBeforeTurnRight != null)) {
				break;
			}

			examinedDist += e1.length;
			if (((examinedDist - vehicle.headPosition) < Settings.lookAheadDistance)
					&& (indexLegOnRouteBeingChecked < (vehicle.routeLegs.size() - 1))) {
				indexLegOnRouteBeingChecked++;
			} else {
				break;
			}
		}
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



	public static void updateRoadBlockInfoForVehicle(Vehicle vehicle) {
		double examinedDist = 0;
		int indexLegOnRouteBeingChecked = vehicle.indexLegOnRoute;
		while (indexLegOnRouteBeingChecked <= (vehicle.routeLegs.size() - 1)) {
			final Edge edgeBeingChecked = vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge;
			if (edgeBeingChecked.isBlocked()) {
				vehicle.isRoadBlockedAhead = true;
				return;
			}
			examinedDist += vehicle.routeLegs.get(indexLegOnRouteBeingChecked).edge.length;
			// Proceeds to the next leg on route if look-ahead distance is not exhausted
			if (((examinedDist - vehicle.headPosition) < Settings.lookAheadDistance)
					&& (indexLegOnRouteBeingChecked < (vehicle.routeLegs.size() - 1))) {
				indexLegOnRouteBeingChecked++;
			} else {
				break;
			}
		}
		vehicle.isRoadBlockedAhead = false;
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
