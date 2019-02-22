package traffic.vehicle;

import java.util.HashMap;
import java.util.Random;

import common.Settings;
import traffic.light.LightColor;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.RoadUtil;
import traffic.vehicle.SlowdownFactor;
import traffic.vehicle.LaneChangeDirection;

/**
 * MOBIL model for lane-changing.
 *
 */
public class MOBIL {

	/**
	 * Calculate the advantage gain in terms of acceleration rate for a vehicle
	 * if it changes lane
	 *
	 */
	static double getPotentialAdvatangeGainOfThisVehicleInTargetLane(final double newAcc, final double oldAcc) {
		return newAcc - oldAcc;
	}

	Vehicle backVehicleInTargetLane = null;

	Vehicle frontVehicleInTargetLane = new Vehicle();

	Random random = new Random();
	VehicleUtil vehicleUtil;
	Vehicle impedingObject = new Vehicle();
	IDM idm;

	public MOBIL() {
		vehicleUtil = new VehicleUtil();
		idm = new IDM();
	}

	/**
	 * Gets the potential acceleration of vehicle based on a slow-down factor.
	 * First, the impeding object for this factor is found. Next, the
	 * acceleration is computed based on the impeding object.
	 *
	 */
	double computeAccelerationWithImpedingObject(final Vehicle vehicle, final Lane targetLane,
			final SlowdownFactor factor) {
		vehicleUtil.updateImpedingObject(vehicle, vehicle.indexLegOnRoute, targetLane.laneNumber, impedingObject,
				factor);
		return idm.computeAcceleration(vehicle, impedingObject);
	}

	/**
	 * Makes decision about lane-changing.
	 *
	 * @return One of the possible lane-changing decisions.
	 */
	public LaneChangeDirection decideLaneChange(final Vehicle vehicle) {
		LaneChangeDirection decision = LaneChangeDirection.SAME;
		double overallGainForChangeTowardsRoadside = 0, overallGainForChangeAwayFromRoadside = 0;

		if (isSafeToChange(vehicle, LaneChangeDirection.TOWARDS_ROADSIDE)) {
			if ((vehicle == vehicle.lane.getFrontVehicleInLane()) && ((vehicle.lane.edge.lightColor == LightColor.KEEP_RED)
					|| (vehicle.lane.edge.lightColor == LightColor.GYR_R))) {
				// Do not attempt lane-change for the vehicle that is the closest one to a red light
				overallGainForChangeTowardsRoadside = 0;
			} else {
				final double newAccByChangeTowardsRoadside = getPotentialAccelerationOfThisVehicleInTargetLane(vehicle,
						vehicle.lane.edge.getLane(vehicle.lane.laneNumber - 1));
				overallGainForChangeTowardsRoadside = getPotentialAdvatangeGainOfThisVehicleInTargetLane(
						newAccByChangeTowardsRoadside, vehicle.acceleration)
						- getPotentialDisadvantageGainOfBackVehicleInTargetLane(vehicle);
			}
			overallGainForChangeTowardsRoadside += getAdditionalIncentive(vehicle,
					LaneChangeDirection.TOWARDS_ROADSIDE);
		}
		if (isSafeToChange(vehicle, LaneChangeDirection.AWAY_FROM_ROADSIDE)) {
			if ((vehicle == vehicle.lane.getFrontVehicleInLane()) && ((vehicle.lane.edge.lightColor == LightColor.KEEP_RED)
					|| (vehicle.lane.edge.lightColor == LightColor.GYR_R))) {
				// Do not attempt lane-change for the vehicle that is the closest one to a red light
				overallGainForChangeAwayFromRoadside = 0;
			} else {
				final double newAccByChangeAwayFromRoadside = getPotentialAccelerationOfThisVehicleInTargetLane(vehicle,
						vehicle.lane.edge.getLane(vehicle.lane.laneNumber + 1));
				overallGainForChangeAwayFromRoadside = getPotentialAdvatangeGainOfThisVehicleInTargetLane(
						newAccByChangeAwayFromRoadside, vehicle.acceleration)
						- getPotentialDisadvantageGainOfBackVehicleInTargetLane(vehicle);
			}
			overallGainForChangeAwayFromRoadside += getAdditionalIncentive(vehicle,
					LaneChangeDirection.AWAY_FROM_ROADSIDE);
		}

		if ((overallGainForChangeAwayFromRoadside > 0)
				&& ((overallGainForChangeAwayFromRoadside - overallGainForChangeTowardsRoadside) > 0)) {
			decision = LaneChangeDirection.AWAY_FROM_ROADSIDE;
		} else if ((overallGainForChangeTowardsRoadside > 0)
				&& ((overallGainForChangeTowardsRoadside - overallGainForChangeAwayFromRoadside) > 0)) {
			decision = LaneChangeDirection.TOWARDS_ROADSIDE;
		} else if ((overallGainForChangeAwayFromRoadside > 0) && (overallGainForChangeTowardsRoadside > 0)) {
			if (random.nextBoolean()) {
				decision = LaneChangeDirection.AWAY_FROM_ROADSIDE;
			} else {
				decision = LaneChangeDirection.TOWARDS_ROADSIDE;
			}
		}

		return decision;
	}

	/**
	 * Get additional incentive of changing. A positive incentive encourages
	 * change. A negative incentive discourages change.
	 */
	double getAdditionalIncentive(final Vehicle vehicle, final LaneChangeDirection direction) {

		double incentive = 0;
		final Edge currentEdge = vehicle.routeLegs.get(vehicle.indexLegOnRoute).edge;

		if (direction == LaneChangeDirection.TOWARDS_ROADSIDE) {
			// Encourage change for making a turn			
			if (Settings.isDriveOnLeft && vehicle.edgeBeforeTurnLeft != null
					&& vehicle.lane.laneNumber >= currentEdge.numLeftLanes) {
				incentive = 5;
			} else if (!Settings.isDriveOnLeft && vehicle.edgeBeforeTurnRight != null
					&& vehicle.lane.laneNumber >= currentEdge.numRightLanes) {
				incentive = 5;
			}
			// Encourage change for giving way to priority vehicle based on emergency strategy
			if (RoadUtil.isEdgeOnPathOfPriorityVehicle(vehicle.lane.edge) && (vehicle.type != VehicleType.PRIORITY)
					&& (Settings.emergencyStrategy != EmergencyStrategy.Flexible)) {
				incentive = 10;
			}

		} else if (direction == LaneChangeDirection.AWAY_FROM_ROADSIDE) {
			// Encourage change for making a turn
			if (Settings.isDriveOnLeft && vehicle.edgeBeforeTurnRight != null
					&& (vehicle.lane.laneNumber < (currentEdge.getLaneCount() - currentEdge.numRightLanes))) {
				incentive = 5;
			} else if (!Settings.isDriveOnLeft && vehicle.edgeBeforeTurnLeft != null
					&& (vehicle.lane.laneNumber < (currentEdge.getLaneCount() - currentEdge.numLeftLanes))) {
				incentive = 5;
			}
		}

		return incentive;

	}

	double getLowerAcceleration(final Vehicle vehicle, final double acc1, final double acc2,
			final SlowdownFactor factor) {
		if (acc1 > acc2) {
			return acc2;
		} else {
			return acc1;
		}
	}

	/**
	 * Gets the potential acceleration of vehicle if it changes to the given
	 * lane at this moment.
	 *
	 *
	 */
	double getPotentialAccelerationOfThisVehicleInTargetLane(final Vehicle vehicle, final Lane lane) {
		return computeAccelerationWithImpedingObject(vehicle, lane, SlowdownFactor.FRONT);
	}

	/**
	 * Calculate the disadvantage in terms of acceleration rate for the back
	 * vehicle in the target lane, assuming the given vehicle changes to the
	 * target lane at this moment.
	 *
	 */
	double getPotentialDisadvantageGainOfBackVehicleInTargetLane(final Vehicle vehicle) {
		if (backVehicleInTargetLane == null) {
			return vehicle.driverProfile.MOBIL_a_thr;
		} else {
			final double currentAccBackVehicleTargetLane = backVehicleInTargetLane.acceleration;
			final double nextAccBackVehicleTargetLane = idm.computeAcceleration(backVehicleInTargetLane, vehicle);
			return (vehicle.driverProfile.MOBIL_p * (currentAccBackVehicleTargetLane - nextAccBackVehicleTargetLane))
					+ vehicle.driverProfile.MOBIL_a_thr;
		}
	}

	/**
	 * Check whether it's safe to change lane now.
	 *
	 *
	 */
	boolean isSafeToChange(final Vehicle vehicle, final LaneChangeDirection direction) {
		// Do not allow tram to change lane
		if (vehicle.type == VehicleType.TRAM) {
			return false;
		}

		Lane targetLane = null;
		if (Settings.isDriveOnLeft) {
			// Drive on LEFT
			if (direction == LaneChangeDirection.TOWARDS_ROADSIDE) {
				if (vehicle.lane.laneNumber == 0) {
					// Vehicle is already in the left-most lane
					return false;
				} else if ((vehicle.edgeBeforeTurnLeft == null)
						&& (vehicle.lane.laneNumber <= vehicle.lane.edge.numLeftOnlyLanes) && !vehicle.lane.isBlocked) {
					// Cannot move to left-only lane if vehicle will not turn left, unless the current lane is blocked
					return false;
				} else if ((vehicle.edgeBeforeTurnRight != null)
						&& ((vehicle.lane.edge.getLaneCount()
								- (vehicle.lane.laneNumber - 1) > vehicle.lane.edge.numRightLanes))
						&& !vehicle.lane.isBlocked) {
					// Cannot leave a lane allowing right-turn if vehicle will turn right, unless the current lane is blocked
					return false;
				} else if (vehicle.lane.edge.isAllLanesOnLeftBlocked(vehicle.lane.laneNumber)) {
					// Cannot move to left as all lanes on the left are blocked
					return false;
				} else {
					targetLane = vehicle.lane.edge.getLane(vehicle.lane.laneNumber - 1);
				}
			} else if (direction == LaneChangeDirection.AWAY_FROM_ROADSIDE) {
				if (vehicle.lane.laneNumber == (vehicle.lane.edge.getLaneCount() - 1)) {
					// Vehicle is already in the right-most lane
					return false;
				} else if ((vehicle.edgeBeforeTurnRight == null)
						&& ((vehicle.lane.edge.getLaneCount()
								- (vehicle.lane.laneNumber + 1)) <= vehicle.lane.edge.numRightOnlyLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot move to right-only lane if vehicle will not turn right, unless the current lane is blocked
					return false;
				} else if ((vehicle.edgeBeforeTurnLeft != null)
						&& ((vehicle.lane.laneNumber + 1) >= vehicle.lane.edge.numLeftLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot leave a lane allowing left-turn if vehicle will turn left, unless the current lane is blocked
					return false;
				} else if (vehicle.lane.edge.isAllLanesOnRightBlocked(vehicle.lane.laneNumber)) {
					// Cannot move to right as all lanes on the right are blocked
					return false;
				} else {
					targetLane = vehicle.lane.edge.getLane(vehicle.lane.laneNumber + 1);
				}
			}
		} else {
			// Drive on RIGHT
			if (direction == LaneChangeDirection.TOWARDS_ROADSIDE) {
				if (vehicle.lane.laneNumber == 0) {
					// Vehicle is already in the right-most lane
					return false;
				} else if ((vehicle.edgeBeforeTurnRight == null)
						&& (vehicle.lane.laneNumber <= vehicle.lane.edge.numRightOnlyLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot move to right-only lane if vehicle will not turn right, unless the current lane is blocked
					return false;
				} else if ((vehicle.edgeBeforeTurnLeft != null)
						&& ((vehicle.lane.edge.getLaneCount()
								- (vehicle.lane.laneNumber - 1) > vehicle.lane.edge.numLeftLanes))
						&& !vehicle.lane.isBlocked) {
					// Cannot leave a lane allowing left-turn if vehicle will turn left, unless the current lane is blocked
					return false;
				} else if (vehicle.lane.edge.isAllLanesOnRightBlocked(vehicle.lane.laneNumber)) {
					// Cannot move to right as all lanes on the right are blocked
					return false;
				} else {
					targetLane = vehicle.lane.edge.getLane(vehicle.lane.laneNumber - 1);
				}
			} else if (direction == LaneChangeDirection.AWAY_FROM_ROADSIDE) {
				if (vehicle.lane.laneNumber == (vehicle.lane.edge.getLaneCount() - 1)) {
					// Vehicle is already in the left-most lane
					return false;
				} else if ((vehicle.edgeBeforeTurnLeft == null)
						&& ((vehicle.lane.edge.getLaneCount()
								- (vehicle.lane.laneNumber + 1)) <= vehicle.lane.edge.numLeftOnlyLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot move to left-only lane if vehicle will not turn left, unless the current lane is blocked
					return false;
				} else if ((vehicle.edgeBeforeTurnRight != null)
						&& ((vehicle.lane.laneNumber + 1) >= vehicle.lane.edge.numRightLanes)
						&& !vehicle.lane.isBlocked) {
					// Cannot leave a lane allowing right-turn if vehicle will turn right, unless the current lane is blocked
					return false;
				} else if (vehicle.lane.edge.isAllLanesOnLeftBlocked(vehicle.lane.laneNumber)) {
					// Cannot move to left as all lanes on the left are blocked
					return false;
				} else {
					targetLane = vehicle.lane.edge.getLane(vehicle.lane.laneNumber + 1);
				}
			}
		}

		// Cannot change if front vehicle in target lane is too close
		frontVehicleInTargetLane = targetLane.getClosestFrontVehicleInLane(vehicle, 0);
		if ((frontVehicleInTargetLane != null) && ((frontVehicleInTargetLane.headPosition
				- frontVehicleInTargetLane.length - vehicle.headPosition) < vehicle.driverProfile.IDM_s0)) {
			return false;
		}

		// Cannot change if back vehicle in target lane is too close
		backVehicleInTargetLane = targetLane.getClosestBackVehicleInLane(vehicle);
		if ((backVehicleInTargetLane != null) && ((vehicle.headPosition - vehicle.length
				- backVehicleInTargetLane.headPosition) < vehicle.driverProfile.IDM_s0)) {
			return false;
		}

		// Cannot change if back vehicle in target lane cannot safely decelerate
		if (backVehicleInTargetLane == null) {
			// No back vehicle: safe
			return true;
		} else {
			final double newAccBackVehicleTargetLane = idm.computeAcceleration(backVehicleInTargetLane, vehicle);
			// Deceleration of back vehicle in target lane would be too significant: unsafe
			return newAccBackVehicleTargetLane > (-1 * vehicle.driverProfile.MOBIL_b_save);
		}
	}
}
