package traffic.vehicle;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import common.Settings;
import processor.worker.Fellow;
import processor.worker.Simulation;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.RoadUtil;
import traffic.routing.RouteLeg;
import traffic.routing.Routing;
import traffic.vehicle.carfollow.CarFollow;
import traffic.vehicle.lanechange.LaneChange;
import traffic.vehicle.lanechange.LaneChangeDirection;
import traffic.vehicle.lanechange.MOBILInput;

public class Vehicle {
	public String id = "";
	private List<RouteLeg> routeLegs = new ArrayList<>(1000);
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
	private LaneChange laneChange = new LaneChange();
	private CarFollow carFollow = new CarFollow();
	private boolean finished = false;
	private boolean reachedFellow = false;

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
	public void startFromParking(double timeNow) {
		if (active && (lane == null) && (timeNow >= earliestTimeToLeaveParking)) {
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
			}
		}
	}

	public void changeLane(final double timeNow){
		if (!((lane == null) || !active || (type == VehicleType.TRAM)
				|| ((timeNow - timeOfLastLaneChange) < driverProfile.minLaneChangeTimeGap))) {

			LaneChangeDirection laneChangeDecision = LaneChangeDirection.SAME;
			MOBILInput mobilInput = new MOBILInput();
			laneChangeDecision = laneChange.decideLaneChange(mobilInput, this);

			if (laneChangeDecision != LaneChangeDirection.SAME) {

				// Cancel priority lanes
				setPriorityLanes(false);

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
				setPriorityLanes(true);
			}
		}
	}

	public void updateRoadBlockInfo() {
		double examinedDist = 0;
		int indexLegOnRouteBeingChecked = indexLegOnRoute;
		while (indexLegOnRouteBeingChecked <= (routeLegs.size() - 1)) {
			final Edge edgeBeingChecked = routeLegs.get(indexLegOnRouteBeingChecked).edge;
			if (edgeBeingChecked.isBlocked()) {
				isRoadBlockedAhead = true;
				return;
			}
			examinedDist += routeLegs.get(indexLegOnRouteBeingChecked).edge.length;
			// Proceeds to the next leg on route if look-ahead distance is not exhausted
			if (((examinedDist - headPosition) < Settings.lookAheadDistance)
					&& (indexLegOnRouteBeingChecked < (routeLegs.size() - 1))) {
				indexLegOnRouteBeingChecked++;
			} else {
				break;
			}
		}
		isRoadBlockedAhead = false;
	}

	public void findEdgeBeforeNextTurn() {
		double examinedDist = 0;
		edgeBeforeTurnLeft = null;
		edgeBeforeTurnRight = null;
		int indexLegOnRouteBeingChecked = indexLegOnRoute;
		while (indexLegOnRouteBeingChecked < (routeLegs.size() - 1)) {
			final Edge e1 = routeLegs.get(indexLegOnRouteBeingChecked).edge;
			final Edge e2 = routeLegs.get(indexLegOnRouteBeingChecked + 1).edge;

			if (e1.startNode == e2.endNode) {
				// Vehicle is going to make U-turn
				edgeBeforeTurnRight = e1;
			} else if (!e1.name.equals(e2.name) || (e1.type != e2.type)) {
				final Line2D.Double e1Seg = new Line2D.Double(e1.startNode.lon, e1.startNode.lat * Settings.lonVsLat,
						e1.endNode.lon, e1.endNode.lat * Settings.lonVsLat);
				final int ccw = e1Seg.relativeCCW(e2.endNode.lon, e2.endNode.lat * Settings.lonVsLat);
				if (ccw < 0) {
					edgeBeforeTurnLeft = e1;
				} else if (ccw > 0) {
					edgeBeforeTurnRight = e1;
				}
			}

			if ((edgeBeforeTurnLeft != null) || (edgeBeforeTurnRight != null)) {
				break;
			}

			examinedDist += e1.length;
			if (((examinedDist - headPosition) < Settings.lookAheadDistance)
					&& (indexLegOnRouteBeingChecked < (routeLegs.size() - 1))) {
				indexLegOnRouteBeingChecked++;
			} else {
				break;
			}
		}
	}

	public void moveForward(double timeNow){
		if(active) {
			// Reset priority vehicle effect flag
			isAffectedByPriorityVehicle = false;
			// Update information regarding turning
			findEdgeBeforeNextTurn();
			// Find impeding objects and compute acceleration based on the objects
			acceleration = carFollow.computeAccelerationBasedOnImpedingObjects(this);
			// Update vehicle speed, which must be between 0 and free-flow speed
			speed += acceleration / Settings.numStepsPerSecond;
			if (speed > lane.edge.freeFlowSpeed) {
				speed = lane.edge.freeFlowSpeed;
			}
			if (speed < 0) {
				speed = 0;
			}
			// Vehicle cannot collide with its impeding object
			final double distToImpedingObjectAtNextStep = distToImpedingObject
					+ ((spdOfImpedingObject - speed) / Settings.numStepsPerSecond);
			if (distToImpedingObjectAtNextStep < driverProfile.IDM_s0) {
				speed = 0;
				acceleration = 0;
			}

			/*
			 * Move forward in the current lane
			 */
			headPosition += speed / Settings.numStepsPerSecond;

			/*
			 * Reset jam start time if vehicle is not in jam
			 */
			if (speed > Settings.congestionSpeedThreshold) {
				timeJamStart = timeNow;
			}

			// Check whether road is explicitly blocked on vehicle's route
			updateRoadBlockInfo();
		}
	}

	public void blockAtTramStop(){
		if (active && lane!= null && type == VehicleType.TRAM) {
			final double brakingDist = VehicleUtil.getBrakingDistance(this);
			double examinedDist = 0;
			for (int j = indexLegOnRoute; j < routeLegs.size(); j++) {
				final Edge edge = routeLegs.get(j).edge;
				examinedDist += edge.length;
				if (edge.endNode.tramStop && ((examinedDist - headPosition) < (2 * brakingDist))
						&& ((examinedDist - headPosition) > brakingDist) && (edge.timeNoTramStopping <= 0)
						&& (edge.timeTramStopping <= 0)) {
					edge.timeTramStopping = Settings.periodOfTrafficWaitForTramAtStop;
					break;
				}
				if ((examinedDist - headPosition) > Settings.lookAheadDistance) {
					break;
				}
			}
		}
	}

	public void updateLane(Lane lane){
		if(this.lane != null) {
			this.lane.removeVehicle(this);
		}
		this.lane = lane;
		if(lane != null) {
			lane.addVehicleToLane(this);
		}
	}

	/**
	 * Set or cancel priority lanes within a certain distance.
	 */
	public void setPriorityLanes(final boolean isPriority) {
		if (type == VehicleType.PRIORITY) {
			double examinedDist = 0;
			int laneNumber = lane.laneNumber;
			Edge edge = lane.edge;
			while ((examinedDist < Settings.lookAheadDistance) && (indexLegOnRoute < (routeLegs.size() - 1))) {
				final Edge targetEdge = routeLegs.get(indexLegOnRoute).edge;
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

	public Edge getCurrentEdge(){
		RouteLeg routeLeg = getCurrentLeg();
		if(routeLeg != null) {
			return routeLeg.edge;
		}
		return null;
	}

	public RouteLeg getCurrentLeg(){
		if(indexLegOnRoute > -1 && indexLegOnRoute < routeLegs.size()) {
			return routeLegs.get(indexLegOnRoute);
		}
		return null;
	}

	public RouteLeg getRouteLeg(int indexOfRouteLeg){
		if(indexOfRouteLeg > -1 && indexOfRouteLeg < routeLegs.size()) {
			return routeLegs.get(indexOfRouteLeg);
		}
		return null;
	}

	public Edge getRouteLegEdge(int indexOfRouteLeg){
		RouteLeg routeLeg = getRouteLeg(indexOfRouteLeg);
		if(routeLeg != null){
			return routeLeg.edge;
		}
		return null;
	}

	public List<RouteLeg> getRouteLegs(){
		return Collections.unmodifiableList(routeLegs);
	}

	public void setRouteLegs(List<RouteLeg> routeLegs){
		this.routeLegs = routeLegs;
	}

	public int getRouteLegCount(){
		return routeLegs.size();
	}

	public boolean isFinished() {
		return finished;
	}

	public void markAsFinished() {
		this.finished = true;
	}

	public void reRoute(double timeNow, Routing routingAlgorithm){
		/*
		 * Re-route vehicle in certain situations
		 */
		if ((type != VehicleType.TRAM) && (Settings.isAllowReroute)) {
			boolean reRoute = false;
			// Reroute happens if vehicle has moved too slowly for too long or the road is blocked ahead
			if (indexLegOnRoute < (getRouteLegs().size() - 1)) {
				if ((timeNow - timeJamStart) > driverProfile.minRerouteTimeGap || isRoadBlockedAhead) {
					reRoute = true;
				}
			}

			if (reRoute) {
				// Cancel priority lanes
				setPriorityLanes(false);

				// Reroute vehicle
				reRoute(routingAlgorithm);

				// Reset jam start time
				timeJamStart = timeNow;
				// Increment reroute count
				numReRoute++;
				// Limit number of re-route for internal vehicle
				if ((numReRoute > Settings.maxNumReRouteOfInternalVehicle) && !isExternal) {
					markAsFinished();
				}
			}
		}
	}

	/**
	 * Create a new route from a given vehicle's current edge to its
	 * destination. The new route must be different to the old route.
	 */
	private void reRoute(Routing routingAlgorithm) {

		List<RouteLeg> oldRoute = getRouteLegs();
		int currentIndexOnOldRoute = indexLegOnRoute;

		// No re-route if vehicle is on last leg
		if (currentIndexOnOldRoute >= oldRoute.size() - 1) {
			return;
		}

		// Copy earlier parts of old route to new route
		ArrayList<RouteLeg> newRoute = new ArrayList<RouteLeg>();
		for (int i = 0; i <= currentIndexOnOldRoute; i++) {
			newRoute.add(oldRoute.get(i));
		}

		// Try a few times for computing new route.
		for (int i = 0; i < 3; i++) {
			ArrayList<RouteLeg> partialRoute = routingAlgorithm.createCompleteRoute(oldRoute.get(currentIndexOnOldRoute + 1).edge,
					oldRoute.get(oldRoute.size() - 1).edge, type);
			// The next leg on the old route cannot be the next leg on the new route!
			if (partialRoute != null && partialRoute.get(0).edge != oldRoute.get(currentIndexOnOldRoute + 1).edge) {
				newRoute.addAll(partialRoute);
				setRouteLegs(newRoute);
				break;
			}
		}

	}

	public boolean isReachedFellow() {
		return reachedFellow;
	}

	public void markAsReachedFellow() {
		this.reachedFellow = true;
	}

	public void moveToNextLink(double timeNow, List<Fellow> connectedFellows){
		if (active) {
			double overshootDist = headPosition - lane.edge.length;

			if (overshootDist >= 0) {

				// Cancel priority lanes
				setPriorityLanes(false);

				while ((indexLegOnRoute < getRouteLegCount()) && (overshootDist >= 0)) {
					// Update head position
					headPosition -= lane.edge.length;
					// Update route leg
					indexLegOnRoute++;

					// Check whether vehicle finishes trip
					if (active && (indexLegOnRoute >= getRouteLegCount())) {
						markAsFinished();
						if (isForeground) {
							timeTravel = timeNow - timeRouteStart;
						}
						break;
					}
					// Locate the new lane of vehicle. If the specified lane does not exist (e.g., moving from primary road to secondary road), change to the one with the highest lane number
					final RouteLeg nextLeg = getRouteLeg(indexLegOnRoute);
					final Edge nextEdge = nextLeg.edge;
					Lane newLane = null;
					if (nextEdge.getLaneCount() <= lane.laneNumber) {
						newLane = nextEdge.getLane(nextEdge.getLaneCount() - 1);
					} else {
						newLane = nextEdge.getLane(lane.laneNumber);
					}
					updateLane(newLane);
					// Remember the cluster of traffic lights
					if (nextEdge.startNode.idLightNodeGroup != 0) {
						idLightGroupPassed = nextEdge.startNode.idLightNodeGroup;
					}
					// Update the overshoot distance of vehicle
					overshootDist -= nextEdge.length;
					// Check whether vehicle reaches fellow worker
					if (Simulation.reachFellow(connectedFellows, this)) {
						markAsReachedFellow();
						break;
					}
					// Park vehicle as plan if vehicle remains on the same
					// worker
					if (nextLeg.stopover > 0) {
						park(false, timeNow);
						break;
					}
				}

				// Set priority lanes
				setPriorityLanes(true);
			}
		}
	}
}
