package traffic.vehicle;

import java.util.ArrayList;
import java.util.Random;

import traffic.road.Edge;
import traffic.road.Lane;
import traffic.routing.RouteLeg;

public class Vehicle {
	public String id = "";
	public ArrayList<RouteLeg> routeLegs = new ArrayList<>(1000);
	public VehicleType type = null;
	public double headPosition = 0;
	public Lane lane = null;
	public double speed = 0;
	public double length = 0;
	public double acceleration = 0;
	public int indexLegOnRoute = 0;
	public boolean active = false;
	public double timeRouteStart = 0;
	public double earliestTimeToLeaveParking = 0;
	public double timeTravel = 0;
	public double timeJamStart = 0;
	public int numReRoute = 0;
	public boolean isExternal = false;
	public boolean isForeground = false;
	public double distToImpedingObject = 10000;
	public double spdOfImpedingObject = 0;
	public double timeOfLastLaneChange = 0;
	public boolean isRoadBlockedAhead = false;
	/**
	 * The ID of the latest light group. This vehicle will ignore other traffic
	 * lights in the same group if it passes one of the lights in the group.
	 */
	public long idLightGroupPassed = -1;
	public DriverProfile driverProfile = DriverProfile.NORMAL;
	/**
	 * This is for highlighting vehicles affected by emergency vehicles on GUI
	 */
	public boolean isAffectedByPriorityVehicle = false;

	public Edge edgeBeforeTurnRight = null;
	public Edge edgeBeforeTurnLeft = null;
	LaneChange laneChange = new LaneChange();

	/**
	 * This method tries to find a start position for a vehicle such that the
	 * vehicle will be unlikely to collide with an existing vehicle. For
	 * simplicity, this method only checks the current route leg and the two
	 * adjacent legs. Therefore it is not guaranteed that the new position is
	 * safe, especially when all the three legs are very short.
	 */
	public double getStartPositionInLane0() {
		Edge currentEdge = routeLegs.get(indexLegOnRoute).edge;

		double headPosSpaceFront = currentEdge.length;

		if (indexLegOnRoute + 1 < routeLegs.size()) {
			RouteLeg legToCheck = routeLegs.get(indexLegOnRoute + 1);
			Lane laneToCheck = legToCheck.edge.getFirstLane();
			if (laneToCheck.getVehicleCount() > 0) {
				Vehicle vehicleToCheck = laneToCheck.getLastVehicleInLane();
				double endPosOfLastVehicleOnNextLeg = vehicleToCheck.headPosition + currentEdge.length
						- vehicleToCheck.length;
				if (endPosOfLastVehicleOnNextLeg < headPosSpaceFront) {
					headPosSpaceFront = endPosOfLastVehicleOnNextLeg;
				}
			}
		}

		double headPosSpaceBack = 0;

		if (indexLegOnRoute > 0) {
			RouteLeg legToCheck = routeLegs.get(indexLegOnRoute - 1);
			Lane laneToCheck = legToCheck.edge.getFirstLane();
			if (laneToCheck.getVehicleCount() > 0) {
				Vehicle vehicleToCheck = laneToCheck.getFrontVehicleInLane();
				double headPosOfFirstVehicleOnPreviousLeg = -(laneToCheck.edge.length - vehicleToCheck.headPosition);
				if (headPosSpaceBack - length < headPosOfFirstVehicleOnPreviousLeg) {
					headPosSpaceBack = headPosOfFirstVehicleOnPreviousLeg + length;
				}
			}
		}

		if (headPosSpaceFront <= headPosSpaceBack)
			return -1;

		final ArrayList<double[]> gaps = new ArrayList<>();
		if (currentEdge.getFirstLane().getVehicleCount() > 0) {

			double gapFront = headPosSpaceFront;
			for (Vehicle vehicleToCheck : currentEdge.getFirstLane().getVehicles()) {
				if (gapFront - length > vehicleToCheck.headPosition) {
					gaps.add(new double[] { gapFront, vehicleToCheck.headPosition + length });
				}
				gapFront = vehicleToCheck.headPosition - vehicleToCheck.length;
				if (gapFront < headPosSpaceBack) {
					break;
				}
			}
		} else {
			gaps.add(new double[] { headPosSpaceFront, headPosSpaceBack });
		}

		if (gaps.size() == 0) {
			return -1;
		} else {
			Random random = new Random();
			// Pick a random position within a random gap
			final double[] gap = gaps.get(random.nextInt(gaps.size()));
			final double pos = gap[0] - (random.nextDouble() * (gap[0] - gap[1]));
			return pos;
		}
	}

	/**
	 * Moves vehicle to parking.
	 */
	public void park(final boolean isNewVehicle, final double timeNow) {
		speed = 0;
		acceleration = 0;
		routeLegs.get(indexLegOnRoute).edge.addParkedVehicle(this);
		if (isNewVehicle) {
			earliestTimeToLeaveParking = timeRouteStart + routeLegs.get(0).stopover;
		} else {
			earliestTimeToLeaveParking = timeNow + routeLegs.get(indexLegOnRoute).stopover;
		}
		if (lane != null) {
			lane.removeVehicle(this);
			lane = null;
		}
	}

	/**
	 * Moves vehicle from parking area onto roads.
	 */
	public boolean startFromParking() {
		final RouteLeg leg = routeLegs.get(indexLegOnRoute);
		final Edge edge = leg.edge;
		final Lane lane = edge.getFirstLane();// Start from the lane closest to roadside
		final double pos = getStartPositionInLane0();
		if (pos >= 0) {
			edge.removeParkedVehicle(this);
			this.lane = lane;
			headPosition = pos;
			speed = 0;
			lane.addVehicleToLane(this);
			return true;
		} else {
			return false;
		}
	}

	public void changeLane(final double timeNow){
		if (!((lane == null) || !active || (type == VehicleType.TRAM)
				|| ((timeNow - timeOfLastLaneChange) < driverProfile.minLaneChangeTimeGap))) {

			LaneChangeDirection laneChangeDecision = LaneChangeDirection.SAME;
			laneChangeDecision = laneChange.decideLaneChange(this);

			if (laneChangeDecision != LaneChangeDirection.SAME) {

				// Cancel priority lanes
				if (type == VehicleType.PRIORITY) {
					VehicleUtil.setPriorityLanes(this, false);
				}

				timeOfLastLaneChange = timeNow;
				final Lane currentLane = lane;
				Lane nextLane = null;
				if (laneChangeDecision == LaneChangeDirection.AWAY_FROM_ROADSIDE) {
					nextLane = currentLane.edge.getLaneAwayFromRoadside(currentLane);
				} else if (laneChangeDecision == LaneChangeDirection.TOWARDS_ROADSIDE) {
					nextLane = currentLane.edge.getLaneTowardsRoadside(currentLane);
				}
				currentLane.removeVehicle(this);
				nextLane.addVehicleToLane(this);
				lane = nextLane;

				// Set priority lanes
				if (type == VehicleType.PRIORITY) {
					VehicleUtil.setPriorityLanes(this, true);
				}
			}
		}
	}
}
