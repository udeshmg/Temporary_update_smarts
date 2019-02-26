package traffic.vehicle;

import common.Settings;
import traffic.light.LightColor;
import traffic.light.TrafficLightTiming;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.RoadUtil;

/**
 * This class computes ideal acceleration based on various types of impeding
 * objects, such as traffic lights, front cars, etc. As safety is of priority,
 * the lowest acceleration based on all the factors is the final acceleration
 * value. For each factor, the acceleration is computed based on IDM model.
 *
 */
public class IDM {

	VehicleUtil vehicleUtil;

	public IDM() {
		vehicleUtil = new VehicleUtil();
	}

	/**
	 * Calculates the acceleration of vehicle based on its relation to an
	 * impeding object.
	 *
	 */
	public double computeAcceleration(final Vehicle vehicle, final ImpedingObject impedingObject) {
		/*
		 * Actual bumper-to-bumper distance from the end of the front vehicle to
		 * the head of this vehicle. Value is in meters.
		 */
		final double s = impedingObject.headPosition - impedingObject.length - vehicle.headPosition;

		/*
		 * Current speed of this vehicle
		 */
		final double v = vehicle.speed;

		/*
		 * Difference between the speed of this vehicle and the speed of the
		 * front vehicle
		 */
		final double dV = vehicle.speed - impedingObject.speed;

		/*
		 * Desired dynamic distance
		 */
		final double sS = vehicle.driverProfile.IDM_s0 + (v * vehicle.driverProfile.IDM_T)
				+ ((v * dV) / (2 * Math.sqrt(vehicle.driverProfile.IDM_a * vehicle.driverProfile.IDM_b)));
		/*
		 * Desired speed
		 */
		// Must be within vehicle capability and road speed limit
		double v0 = vehicle.lane.edge.freeFlowSpeed;
		if (v0 > vehicle.type.maxSpeed) {
			v0 = vehicle.type.maxSpeed;
		}
		/*
		 * Acceleration exponent
		 */
		final double delta = 4;
		/*
		 * Acceleration
		 */
		final double acceleration = vehicle.driverProfile.IDM_a * (1 - Math.pow(v / v0, delta) - Math.pow(sS / s, 2));

		return acceleration;
	}

	/**
	 * Gets the potential acceleration of vehicle based on a slow-down factor.
	 * First, the impeding object for this factor is found. Next, the
	 * acceleration is computed based on the impeding object.
	 */
	public double computeAccelerationWithImpedingObject(final Vehicle vehicle, final ImpedingObject impedingObject,
			final Lane targetLane, final SlowdownFactor factor) {
		updateImpedingObject(vehicle, vehicle.indexLegOnRoute, targetLane.laneNumber, impedingObject,
				factor);
		return computeAcceleration(vehicle, impedingObject);
	}

	/**
	 * Gets the lower acceleration between two values. Also updates the
	 * information about impeding object corresponding to the lowest
	 * acceleration.
	 *
	 *
	 */
	double getLowerAccelerationAndUpdateSlowdownFactor(final Vehicle vehicle, final ImpedingObject impedingObject,
			final double acc1, final double acc2) {
		if (acc1 > acc2) {
			vehicle.distToImpedingObject = impedingObject.headPosition - impedingObject.length - vehicle.headPosition;
			vehicle.spdOfImpedingObject = impedingObject.speed;
			return acc2;
		} else {
			return acc1;
		}
	}

	/**
	 * Accelerations based on certain slow-down factors are computed. The lowest
	 * value is the final potential acceleration as safety is of priority.
	 *
	 */
	public double updateBasedOnAllFactors(final Vehicle vehicle) {
		final ImpedingObject impedingObject = new ImpedingObject();
		double lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, 10000,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.FRONT));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.TRAM));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.LIGHT));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.CONFLICT));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.TURN));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane, SlowdownFactor.LANEBLOCK));
		lowestAcceleration = getLowerAccelerationAndUpdateSlowdownFactor(vehicle, impedingObject, lowestAcceleration,
				computeAccelerationWithImpedingObject(vehicle, impedingObject, vehicle.lane,
						SlowdownFactor.PRIORITY_VEHICLE));

		return lowestAcceleration;
	}

	/**
	 * Find impeding object based on a certain factor. Impeding object's head
	 * position is ahead of the given vehicle's head position. Impeding object
	 * may not be in the same lane of the given vehicle when this method is
	 * called during lane-changing.
	 *
	 * @param vehicle
	 *            The vehicle whose route is used in the search
	 * @param indexLegOnRouteBeingChecked
	 *            Indicate the route leg from which search is started
	 * @param laneNumber
	 *            Indicate the lane where impeding object is searched. If the
	 *            search expands to multiple edges, only the lanes with the same
	 *            lane number are considered
	 * @param impedingObj
	 *            The virtual vehicle object that stores the properties of the
	 *            impeding object
	 * @param factor
	 *            Type of the impeding object, e.g., traffic lights
	 */
	public void updateImpedingObject(final Vehicle vehicle, int indexLegOnRouteBeingChecked, final int laneNumber,
									 final ImpedingObject impedingObj, final SlowdownFactor factor) {
		double examinedDist = 0;
		impedingObj.headPosition = -1; // Initialize front vehicle's position.
		while ((impedingObj.headPosition < 0) && (indexLegOnRouteBeingChecked <= (vehicle.getRouteLegCount() - 1))) {
			if (factor == SlowdownFactor.FRONT) {
				updateImpedingObject_Front(vehicle, examinedDist, indexLegOnRouteBeingChecked, laneNumber, impedingObj);
			} else if (factor == SlowdownFactor.TRAM) {
				updateImpedingObject_Tram(vehicle, examinedDist,
						vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked), impedingObj);
			} else if (factor == SlowdownFactor.LIGHT) {
				updateImpedingObject_Light(vehicle, examinedDist,
						vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked), impedingObj);
			} else if (factor == SlowdownFactor.CONFLICT) {
				updateImpedingObject_Conflict(vehicle, examinedDist, indexLegOnRouteBeingChecked, impedingObj);
			} else if (factor == SlowdownFactor.LANEBLOCK) {
				updateImpedingObject_LaneBlock(vehicle, examinedDist, indexLegOnRouteBeingChecked, impedingObj);
			} else if (factor == SlowdownFactor.TURN) {
				updateImpedingObject_Turn(vehicle, examinedDist, indexLegOnRouteBeingChecked, impedingObj);
			} else if (factor == SlowdownFactor.PRIORITY_VEHICLE) {
				updateImpedingObject_PriorityVehicle(vehicle, examinedDist, indexLegOnRouteBeingChecked, impedingObj);
			}
			if (impedingObj.headPosition < 0) {
				examinedDist += vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked).length;
				// Proceeds to the next leg on route if look-ahead distance is
				// not exhausted
				if (((examinedDist - vehicle.headPosition) < Settings.lookAheadDistance)
						&& (indexLegOnRouteBeingChecked < (vehicle.getRouteLegCount() - 1))) {
					indexLegOnRouteBeingChecked++;
				} else {
					// If no impeding object is found within look-ahead
					// distance, returns a virtual one that moves fast at long
					// distance
					impedingObj.speed = 100;
					impedingObj.headPosition = vehicle.headPosition + 10000;
					impedingObj.type = VehicleType.VIRTUAL_STATIC;
					impedingObj.length = 0;
					break;
				}
			}
		}

		// Make sure there is a virtual impeding object
		if (impedingObj.headPosition < 0) {
			impedingObj.speed = 100;
			impedingObj.headPosition = vehicle.headPosition + 10000;
			impedingObj.type = VehicleType.VIRTUAL_STATIC;
			impedingObj.length = 0;
		}

	}

	/**
	 * Find impeding object that is a vehicle traveling towards an upcoming
	 * intersection on the route of the given vehicle. If there are multiple
	 * conflicting vehicles, the impeding object is the vehicle that will arrive
	 * the intersection earlier than other conflicting vehicles.
	 *
	 */
	void updateImpedingObject_Conflict(final Vehicle vehicle, final double examinedDist,
									   final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj) {
		/*
		 * Search for traffic from possible conflicting approaches at the end of
		 * the current edge.
		 */
		if (indexLegOnRouteBeingChecked < (vehicle.getRouteLegCount() - 1)) {
			final Edge targetEdge = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
			final Edge nextEdge = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked + 1);

			double earliestTime = 10000;

			// Gets the earliest time that conflicting traffic arrives at
			// intersection
			for (final Edge e : RoadUtil.getConflictingEdges(targetEdge, nextEdge)) {
				for (final Lane lane : e.getLanes()) {
					if (lane.getVehicleCount() > 0) {
						final Vehicle firstV = lane.getFrontVehicleInLane();
						if (firstV.speed > 0) {
							final double arrivalTime = (e.length - firstV.headPosition) / firstV.speed;
							if (arrivalTime < earliestTime) {
								earliestTime = arrivalTime;
							}
						}
					}
				}
			}

			// Give way to another road being used by priority vehicles
			boolean isGiveWayToPriorityVehicle = false;
			if (!vehicle.lane.isPriority) {
				for (final Edge e : targetEdge.endNode.inwardEdges) {
					if (e == targetEdge) {
						continue;
					}
					if (vehicle.lane.edge.name.equals(e.name) && (vehicle.lane.edge.type == e.type)) {
						continue;
					}
					if (e.isEdgeOnPathOfPriorityVehicle()) {
						isGiveWayToPriorityVehicle = true;
						vehicle.isAffectedByPriorityVehicle = true;
						break;
					}
				}
			}

			/*
			 * If the nearest time that a conflicting vehicle arrives at the
			 * intersection is too close, this vehicle must stop at the
			 * intersection to prevent collision.
			 */
			if ((!vehicle.lane.isPriority && (earliestTime < Settings.minTimeSafeToCrossIntersection))
					|| isGiveWayToPriorityVehicle) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = (examinedDist + targetEdge.length + vehicle.driverProfile.IDM_s0) - 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
			}
		}
	}

	/**
	 * Find the front vehicle which impedes the movement of the given vehicle.
	 * Note that the front vehicle can be in a different lane from the given
	 * vehicle. For example, if this method is called during lane-changing, the
	 * front vehicle can be in a lane on the left or right of the current lane
	 * of the given vehicle.
	 */
	void updateImpedingObject_Front(final Vehicle vehicle, final double examinedDist,
									final int indexLegOnRouteBeingChecked, int laneNumber, final ImpedingObject slowdownObj) {

		final Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
		// Adjust lane number based on continuity of lane
		int laneNumberBeingChecked = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
				laneNumber);

		// Returns the closest impeding object, whose head position is in front of the given vehicle.
		final Lane laneBeingChecked = edgeBeingChecked.getLane(laneNumberBeingChecked);
		final Vehicle frontVehicle = laneBeingChecked.getClosestFrontVehicleInLane(vehicle, examinedDist);
		if (frontVehicle != null) {
			slowdownObj.speed = frontVehicle.speed;
			slowdownObj.headPosition = examinedDist + frontVehicle.headPosition;
			slowdownObj.type = frontVehicle.type;
			slowdownObj.length = frontVehicle.length;
			// Do not cross the intersection that is immediately behind the front vehicle if the front vehicle is too slow and is too close to the intersection
			if (RoadUtil.hasIntersectionAtEdgeStart(frontVehicle.lane.edge)
					&& (vehicle.lane.edge != frontVehicle.lane.edge)
					&& (slowdownObj.speed < Settings.intersectionSpeedThresholdOfFront)
					&& (frontVehicle.headPosition - frontVehicle.length <= vehicle.driverProfile.IDM_s0
					+ vehicle.length)//The current vehicle cannot stop between the intersection and the front vehicle due to limited space
					&& (frontVehicle.headPosition - frontVehicle.length >= 0)//Only consider the situation where front vehicle has passed the intersection in whole
					&& (VehicleUtil.getBrakingDistance(vehicle) <= (examinedDist - vehicle.headPosition))) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = examinedDist - 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
			}
		} else if (laneBeingChecked.endPositionOfLatestVehicleLeftThisWorker < laneBeingChecked.edge.length) {
			// A front vehicle may be running on a different worker. Therefore we check vehicle position sent from other workers.
			slowdownObj.speed = laneBeingChecked.speedOfLatestVehicleLeftThisWorker;
			slowdownObj.headPosition = examinedDist + laneBeingChecked.endPositionOfLatestVehicleLeftThisWorker;
			slowdownObj.type = VehicleType.VIRTUAL_STATIC;
			slowdownObj.length = 0;
		}

	}

	/**
	 * Find impeding object caused by priority vehicle, e.g. ambulance.
	 */
	void updateImpedingObject_PriorityVehicle(final Vehicle vehicle, final double examinedDist,
											  final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj) {
		if (vehicle.type == VehicleType.PRIORITY) {
			return;
		}

		final Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
		// Adjust lane number based on continuity of lane
		int laneNumber = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
				vehicle.lane.laneNumber);

		// Stop vehicle if the emergency strategy requires non-priority vehicles to pull off
		for (int i = laneNumber + 1; i < edgeBeingChecked.getLaneCount(); i++) {
			if (edgeBeingChecked.getLane(i).isPriority
					&& (Settings.emergencyStrategy == EmergencyStrategy.NonEmergencyPullOffToRoadside)) {
				final double brakingDist = VehicleUtil.getBrakingDistance(vehicle);
				slowdownObj.headPosition = vehicle.headPosition + brakingDist;
				slowdownObj.length = 0;
				slowdownObj.speed = 0;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
			}
		}

		// Set flag that this vehicle is affected by priority vehicle
		if ((vehicle.type != VehicleType.PRIORITY) && edgeBeingChecked.isEdgeOnPathOfPriorityVehicle()) {
			vehicle.isAffectedByPriorityVehicle = true;
		}
	}

	/**
	 * Find impeding object that is a blocked lane.
	 */
	void updateImpedingObject_LaneBlock(final Vehicle vehicle, final double examinedDist,
										final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj) {
		if (indexLegOnRouteBeingChecked < vehicle.getRouteLegCount()) {
			final Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
			// Adjust lane number based on continuity of lane
			int laneNumber = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
					vehicle.lane.laneNumber);
			final Lane targetLane = edgeBeingChecked.getLane(laneNumber);
			if (targetLane.isBlocked) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = (examinedDist
						+ vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked).length + vehicle.driverProfile.IDM_s0)
						- 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
			}
		}
	}

	/**
	 * Find impeding object that is a traffic light.
	 */
	void updateImpedingObject_Light(final Vehicle vehicle, final double examinedDist, final Edge targetEdge,
									final ImpedingObject slowdownObj) {
		if (vehicle.type == VehicleType.PRIORITY) {
			// Priority vehicle ignores any traffic light
			return;
		}
		/*
		 * Checks traffic light at the end of the target lane's edge.
		 */
		if (Settings.trafficLightTiming != TrafficLightTiming.NONE) {

			// Ignore other lights if vehicle already passed one of the lights
			// in the same group
			if (targetEdge.endNode.idLightNodeGroup == vehicle.idLightGroupPassed) {
				return;
			}

			// Flags the event that vehicle is within certain distance to light
			if (targetEdge.endNode.light && (((examinedDist + targetEdge.length)
					- vehicle.headPosition) < Settings.trafficLightDetectionDistance)) {
				targetEdge.isDetectedVehicleForLight = true;
			}

			boolean stopAtLight = false;
			if ((targetEdge.lightColor == LightColor.GYR_R) || (targetEdge.lightColor == LightColor.KEEP_RED)) {
				stopAtLight = true;
			} else if (targetEdge.lightColor == LightColor.GYR_Y) {
				if (VehicleUtil.getBrakingDistance(vehicle) <= ((examinedDist + targetEdge.length) - vehicle.headPosition)) {
					stopAtLight = true;
				}
			}

			if (stopAtLight) {
				slowdownObj.speed = 0;
				// The position may be put further out from edge end to avoid car stop before
				// entering an edge.
				if (targetEdge.length <= vehicle.driverProfile.IDM_s0) {
					slowdownObj.headPosition = (examinedDist + targetEdge.length + vehicle.driverProfile.IDM_s0)
							- 0.00001;
				} else {
					slowdownObj.headPosition = examinedDist + targetEdge.length;
				}
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
			}
		}
	}

	/**
	 * Find impeding object related to tram stops. A tram stop is located at the
	 * end of an edge. The edge has a count-down timer, which is triggered when
	 * a tram approaches the tram stop. If the count-down is in process, the
	 * tram cannot pass the tram stop.
	 *
	 * Tram stop also affects other vehicles that move in parallel lanes/edges
	 * besides tram track. Note that other vehicles must stop behind tram, as
	 * required by road rule.
	 *
	 * In OpenStreetMap data, tram tracks are separated from other roads. Hence
	 * there are many roads parallel to tram tracks. The tram edges that are
	 * parallel to an edge are identified during pre-processing.
	 */
	void updateImpedingObject_Tram(final Vehicle vehicle, final double examinedDist, final Edge targetEdge,
								   final ImpedingObject slowdownObj) {

		if ((vehicle.type == VehicleType.TRAM) && (targetEdge.timeTramStopping > 0)) {
			slowdownObj.speed = 0;
			slowdownObj.headPosition = (examinedDist + targetEdge.length + vehicle.driverProfile.IDM_s0) - 0.00001;
			slowdownObj.type = VehicleType.VIRTUAL_STATIC;
			slowdownObj.length = 0;
			return;
		}

		if ((vehicle.type != VehicleType.TRAM) && Settings.isAllowTramRule) {

			if (targetEdge.timeTramStopping > 0) {
				slowdownObj.speed = 0;
				slowdownObj.headPosition = (((examinedDist + targetEdge.length) - VehicleType.TRAM.length)
						+ vehicle.driverProfile.IDM_s0) - 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.length = 0;
				return;
			}

			else {
				final Edge parallelTramEdgeWithTramStop = targetEdge.parallelTramEdgeWithTramStop;
				if ((parallelTramEdgeWithTramStop != null) && (parallelTramEdgeWithTramStop.timeTramStopping > 0)) {
					slowdownObj.speed = 0;
					slowdownObj.headPosition = (((examinedDist + targetEdge.distToTramStop) - VehicleType.TRAM.length)
							+ vehicle.driverProfile.IDM_s0) - 0.00001;
					slowdownObj.type = VehicleType.VIRTUAL_STATIC;
					slowdownObj.length = 0;
					return;
				}
			}
		}

	}

	/**
	 * Find impeding object that is an intersection where the given vehicle
	 * needs to make a turn, including a U-turn. The speed and position of the
	 * impeding object will allow the given vehicle to slow down and pass the
	 * intersection.
	 *
	 *
	 */
	void updateImpedingObject_Turn(final Vehicle vehicle, final double examinedDist,
								   final int indexLegOnRouteBeingChecked, final ImpedingObject slowdownObj) {
		Edge edgeBeingChecked = vehicle.getRouteLegEdge(indexLegOnRouteBeingChecked);
		if (edgeBeingChecked == vehicle.edgeBeforeTurnLeft || edgeBeingChecked == vehicle.edgeBeforeTurnRight) {
			// Block vehicle if vehicle's lane cannot be used for turning
			int laneNumberBeingChecked = RoadUtil.getLaneNumberForTargetEdge(edgeBeingChecked, vehicle.lane.edge,
					vehicle.lane.laneNumber);
			boolean isNeedToBlock = false;
			if (Settings.isDriveOnLeft) {
				if (edgeBeingChecked == vehicle.edgeBeforeTurnLeft
						&& laneNumberBeingChecked >= edgeBeingChecked.numLeftLanes
						&& !edgeBeingChecked.isAllLanesOnLeftBlocked(laneNumberBeingChecked)) {
					isNeedToBlock = true;
				} else if (edgeBeingChecked == vehicle.edgeBeforeTurnRight
						&& laneNumberBeingChecked < edgeBeingChecked.getLaneCount() - edgeBeingChecked.numRightLanes
						&& !edgeBeingChecked.isAllLanesOnRightBlocked(laneNumberBeingChecked)) {
					isNeedToBlock = true;
				}
			} else {
				if (edgeBeingChecked == vehicle.edgeBeforeTurnLeft
						&& laneNumberBeingChecked < edgeBeingChecked.getLaneCount() - edgeBeingChecked.numLeftLanes
						&& !edgeBeingChecked.isAllLanesOnLeftBlocked(laneNumberBeingChecked)) {
					isNeedToBlock = true;
				} else if (edgeBeingChecked == vehicle.edgeBeforeTurnRight
						&& laneNumberBeingChecked >= edgeBeingChecked.numRightLanes
						&& !edgeBeingChecked.isAllLanesOnRightBlocked(laneNumberBeingChecked)) {
					isNeedToBlock = true;
				}
			}
			if (isNeedToBlock) {
				slowdownObj.headPosition = (examinedDist + edgeBeingChecked.length + vehicle.driverProfile.IDM_s0)
						- 0.00001;
				slowdownObj.type = VehicleType.VIRTUAL_STATIC;
				slowdownObj.speed = 0;
				slowdownObj.length = 0;
			} else {
				slowdownObj.headPosition = examinedDist + edgeBeingChecked.length
						+ (VehicleType.VIRTUAL_SLOW.maxSpeed * 3);//The virtual object is a few seconds ahead
				slowdownObj.type = VehicleType.VIRTUAL_SLOW;
				slowdownObj.speed = VehicleType.VIRTUAL_SLOW.maxSpeed;
				slowdownObj.length = 0;
			}
		}
	}
}
