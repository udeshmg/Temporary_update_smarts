package traffic.vehicle;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

import common.Settings;
import processor.worker.Fellow;
import processor.worker.Simulation;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.road.RoadUtil;
import traffic.routing.RouteLeg;
import traffic.routing.Routing;
import traffic.vehicle.carfollow.CarFollow;
import traffic.vehicle.carfollow.SlowdownFactor;
import traffic.vehicle.lanechange.LaneChange;
import traffic.vehicle.lanechange.LaneChangeDirection;
import traffic.vehicle.lanechange.MOBILInput;

public class Vehicle {
	public String id = "";
	public int vid = -1;
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

	private double cityMeteringSpeed = Double.MAX_VALUE;
	private double lastSpeedChangeTime = 0;
	private double startHeadPosition = 0;
	private SlowdownFactor recentSlowDownFactor = null;
	private IntersectionDecision decision = null;

	/**
	 * This method tries to find a start position for a vehicle such that the
	 * vehicle will be unlikely to collide with an existing vehicle. For
	 * simplicity, this method only checks the current route leg and the two
	 * adjacent legs. Therefore it is not guaranteed that the new position is
	 * safe, especially when all the three legs are very short.
	 */
	public double getStartPositionInLane0() {
		Edge currentEdge = routeLegs.get(indexLegOnRoute).edge;

		double headPosSpaceFront = currentEdge.length - currentEdge.getEndIntersectionSize();

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

		double headPosSpaceBack = getNonCollidingHeadPosSpaceBack(currentEdge) + length;

		headPosSpaceFront = headPosSpaceFront - driverProfile.IDM_s0;
		headPosSpaceBack = headPosSpaceBack + driverProfile.IDM_s0;

		if (headPosSpaceFront <= headPosSpaceBack)
			return -1;

		final ArrayList<double[]> gaps = new ArrayList<>();
		if (currentEdge.getFirstLane().getVehicleCount() > 0) {

			double gapFront = headPosSpaceFront;
			for (Vehicle vehicleToCheck : currentEdge.getFirstLane().getVehicles()) {
				double safePosFromLaneVehicle = vehicleToCheck.headPosition +
						vehicleToCheck.getUnsafeDistanceForVehiclesFromParking() + driverProfile.IDM_s0;
				if (gapFront - length > safePosFromLaneVehicle) {
					gaps.add(new double[] { gapFront, safePosFromLaneVehicle + length });
				}
				gapFront = vehicleToCheck.headPosition - (vehicleToCheck.length + driverProfile.IDM_s0);
				if (gapFront < headPosSpaceBack) {
					break;
				}
			}
			if(gapFront > headPosSpaceBack){
				gaps.add(new double[] { gapFront, headPosSpaceBack });
			}
		} else {
			gaps.add(new double[] { headPosSpaceFront, headPosSpaceBack });
		}
		double minGapBack = currentEdge.getStartIntersectionSize() + driverProfile.IDM_s0 + length;
		double minGapFront = currentEdge.getEndIntersectionSize() + driverProfile.IDM_s0;
		List<double[]> validGaps = getValidGaps(gaps, currentEdge.length, minGapBack, minGapFront);
		if (validGaps.size() == 0) {
			return -1;
		} else {
			Random random = new Random();
			// Pick a random position within a random gap
			final double[] gap = validGaps.get(random.nextInt(validGaps.size()));

			final double pos = gap[1] + (random.nextDouble() * (gap[0] - gap[1]) * Settings.startPosOffset);
			return pos;
		}
	}

	public double getNonCollidingHeadPosSpaceBack(Edge current){
		Node incomingJunc = current.startNode;
		List<Edge> inEdges = incomingJunc.inwardEdges;
		double maxHeadPos = current.getStartIntersectionSize();
		for (Edge inEdge : inEdges) {
			Lane lane = inEdge.getLane(0);
			Vehicle first = lane.getFrontVehicleInLane();
			if(first != null) {
				Edge next = first.getNextEdge();
				if (next != null && next.index == current.index) {
					double remainingDist = getUnsafeDistanceForVehiclesFromParking() - (inEdge.length - first.headPosition);
					if (maxHeadPos < remainingDist) {
						maxHeadPos = remainingDist;
					}
				}
			}
		}
		return maxHeadPos;
	}

	public double getUnsafeDistanceForVehiclesFromParking(){
		return speed * driverProfile.IDM_T;
	}


	public List<double[]> getValidGaps(List<double[]> gaps, double laneLength, double minGapBack, double minGapFront){
		double effectiveLength = laneLength - minGapBack - minGapFront;
		List<double[]> validGaps = new ArrayList<>();
		for (double[] gap : gaps) {
			if (gap[1] <= (minGapBack) + (Settings.startGapOffset) * effectiveLength) {
				validGaps.add(gap);
			}
		}
		return validGaps;
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
	public void startFromParking() {
		final RouteLeg leg = routeLegs.get(indexLegOnRoute);
		final Edge edge = leg.edge;
		final Lane lane = edge.getFirstLane();// Start from the lane closest to roadside
		final double pos = getStartPositionInLane0();
		if (pos >= 0) {
			this.lane = lane;
			headPosition = pos;
			startHeadPosition = headPosition;
			speed = 0;
			lane.addVehicleToLane(this);
			edge.setNextVehicleToGetIntoTheLane(null);
		}
	}

	public boolean isStartFromParking(double timeNow){
		return active && (lane == null) && (timeNow >= earliestTimeToLeaveParking);
	}

	public void changeLane(final double timeNow){
		if (!((lane == null) || !active || (type == VehicleType.TRAM)
				|| ((timeNow - timeOfLastLaneChange) < driverProfile.minLaneChangeTimeGap))) {

			LaneChangeDirection laneChangeDecision = LaneChangeDirection.SAME;
			MOBILInput mobilInput = new MOBILInput(lane, getRouteInLookAheadDistance(), type);
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

	public List<RouteLeg> getRouteInLookAheadDistance(){
		List<RouteLeg> legsAhead = new ArrayList<>();
		double examinedDist = 0;
		for (int i = indexLegOnRoute; i < routeLegs.size(); i++) {
			final Edge e1 = routeLegs.get(i).edge;
			examinedDist += e1.length;
			if((examinedDist - headPosition) < Settings.lookAheadDistance){
				legsAhead.add(routeLegs.get(i));
			}else{
				break;
			}
		}
		return legsAhead;
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

			if(speed > 0){
				lastSpeedChangeTime = timeNow;
			}

			/*
			 * Reset jam start time if vehicle is not in jam
			 */
			if (speed > Settings.congestionSpeedThreshold) {
				timeJamStart = timeNow;
			}

			// Check whether road is explicitly blocked on vehicle's route
			updateRoadBlockInfo();
			takeIntersectionDecision();
		}
	}

	public void takeIntersectionDecision(){
		Edge current = lane.edge;
		if(hasNextEdge()) {
			if (headPosition > current.length - current.endNode.getIntersectionSize(current.startNode)) {
				Lane next = getLaneInNextEdge(getNextEdge());
				decision = new IntersectionDecision(lane, next);
			}else if(headPosition > current.startNode.getIntersectionSize(current.endNode) + length){
				decision = null;
			}
		}
	}

	public IntersectionDecision getDecision() {
		return decision;
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
						//if (isForeground) {
							timeTravel = timeNow - timeRouteStart;
						//}
						break;
					}
					// Locate the new lane of vehicle. If the specified lane does not exist (e.g., moving from primary road to secondary road), change to the one with the highest lane number
					final RouteLeg nextLeg = getRouteLeg(indexLegOnRoute);
					final Edge nextEdge = nextLeg.edge;
					Lane newLane = decision.getEndLane();

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

	public Lane getLaneInNextEdge(Edge nextEdge){
		if(nextEdge.endNode == lane.edge.startNode){
			return nextEdge.getLane(0);
		}else {
			Lane newLane = null;
			if (nextEdge.getLaneCount() <= lane.laneNumber) {
				newLane = nextEdge.getLane(nextEdge.getLaneCount() - 1);
			} else {
				newLane = nextEdge.getLane(lane.laneNumber);
			}
			return newLane;
		}
	}

	public void setCityMeteringSpeed(double cityMeteringSpeed) {
		this.cityMeteringSpeed = cityMeteringSpeed;
	}

	public double getCityMeteringSpeed() {
		return cityMeteringSpeed;
	}

	public double getLastSpeedChangeTime() {
		return lastSpeedChangeTime;
	}

	public Point2D getCurrentPosition(){
		Edge current = getCurrentEdge();
		if(current != null){
			double ratio = headPosition/current.length;
			Point2D p = new Point2D.Double(current.startNode.lon + ratio * (current.endNode.lon - current.startNode.lon),
					current.startNode.lat + ratio * (current.endNode.lat - current.startNode.lat));
			return p;
		}
		return null;
	}

	public double getActualTravelTime(){
		return timeTravel;
	}

	public double getBestTravelTime(){
		double bestTT = 0;
		for (int i = 0; i < routeLegs.size(); i++) {
			Edge edge = routeLegs.get(i).edge;
			if(i == 0){
				bestTT += ((edge.length - startHeadPosition)/edge.freeFlowSpeed);
			}else{
				bestTT += (edge.length/edge.freeFlowSpeed);
			}
		}
		return bestTT;
	}

	public double getRouteLength(){
		double length = 0;
		for (int i = 0; i < routeLegs.size(); i++) {
			Edge edge = routeLegs.get(i).edge;
			if(i == 0){
				length += (edge.length - startHeadPosition);
			}else{
				length += edge.length;
			}
		}
		return length;
	}

	public Edge getNextEdge(){
		if(hasNextEdge()) {
			return routeLegs.get(indexLegOnRoute + 1).edge;
		}else{
			return null;
		}
	}

	public boolean hasNextEdge(){
		return indexLegOnRoute < routeLegs.size() - 1;
	}

	public Edge getPreviousEdge(){
		if(indexLegOnRoute > 0) {
			return routeLegs.get(indexLegOnRoute - 1).edge;
		}else{
			return null;
		}
	}

	public String getRecentSlowDownFactor() {
		return String.valueOf(recentSlowDownFactor);
	}

	public void setRecentSlowDownFactor(SlowdownFactor recentSlowDownFactor) {
		this.recentSlowDownFactor = recentSlowDownFactor;
	}

	public class IntersectionDecision{
		private Lane startLane;
		private Lane endLane;

		public IntersectionDecision(Lane startLane, Lane endLane) {
			this.startLane = startLane;
			this.endLane = endLane;
		}

		public Lane getStartLane() {
			return startLane;
		}

		public Lane getEndLane() {
			return endLane;
		}
	}
}
