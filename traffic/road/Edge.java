package traffic.road;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import common.Settings;
import traffic.light.LightColor;
import traffic.vehicle.Vehicle;

/**
 * Edge is a basic element in road network. A network consists of a number of
 * nodes connected by edges.
 *
 */
public class Edge {
	int importedStartNodeIndex;
	int importedEndNodeIndex;
	public Node startNode;
	public Node endNode;
	public RoadType type;
	public String name;
	/**
	 * Free flow speed of vehicles
	 */
	public double freeFlowSpeed;

	/**
	 * Number of vehicles that can be filled into this edge based on average
	 * vehicle speed.
	 */
	public double capacity = 0;
	/**
	 * Whether this edge is in a roundabout.
	 */
	public boolean isRoundabout;
	/**
	 * Index of this edge in the whole network.
	 */
	public int index;
	/**
	 * Length of this edge in meters.
	 */
	public double length;
	/**
	 * Collection of the lanes belonging to this edge.
	 */
	private ArrayList<Lane> lanes = new ArrayList<>();
	/**
	 * Remaining time that the tram stops at the station on this edge.
	 */
	public double timeTramStopping = 0;
	/**
	 * Remaining time that the tram stop should be ignored. This can be used for
	 * preventing vehicles to wait at tram station for consecutive periods.
	 */
	public double timeNoTramStopping = 0;
	/**
	 * Color of traffic light at the end of this edge.
	 */
	public LightColor lightColor = LightColor.GYR_G;

	/**
	 * Tram edge parallel to this edge and is with tram stop.
	 */
	public Edge parallelTramEdgeWithTramStop = null;
	/**
	 * Distance between the start node of this edge to the end node of the
	 * parallel tram edge with tram stop. The tram stop is at the end node of
	 * the edge.
	 */
	public double distToTramStop;
	/**
	 * Collection of vehicles that are parked along this edge.
	 */
	private ArrayList<Vehicle> parkedVehicles = new ArrayList<>(100);
	/**
	 * List of tram routes passing this edge.
	 */
	public ArrayList<String> tramRoutesRef = new ArrayList<>();
	/**
	 * List of bus routes passing this edge.
	 */
	public ArrayList<String> busRoutesRef = new ArrayList<>();
	/**
	 * Angle of edge
	 */
	public double angleOutward, angleInward;
	/**
	 * Vehicle speed extracted from external traffic data
	 */
	public double currentSpeed;
	/**
	 * Whether there is one or more vehicle within the detection range of
	 * traffic light
	 */
	public boolean isDetectedVehicleForLight = false;
	/**
	 * Delay in seconds. This value can be used for calibration.
	 */
	public double delay;
	/**
	 * Number of lanes for right-turn
	 */
	public int numRightLanes;
	public int numRightOnlyLanes;
	/**
	 * Number of lanes for left-turn
	 */
	public int numLeftLanes;
	public int numLeftOnlyLanes;

	public Edge(final int importedStartNodeIndex, final int importedEndNodeIndex, final String type, final String name,
			final double maxspeed, final boolean roundabout, final List<String> tramRoutesRef,
			final List<String> busRoutesRef, final int numRightLanes, final int numLeftLanes,
			final int numRightOnlyLanes, final int numLeftOnlyLanes) {
		super();
		this.importedStartNodeIndex = importedStartNodeIndex;
		this.importedEndNodeIndex = importedEndNodeIndex;
		this.type = RoadType.valueOf(type);
		this.name = name;
		freeFlowSpeed = maxspeed;
		currentSpeed = freeFlowSpeed;
		isRoundabout = roundabout;
		this.tramRoutesRef.addAll(tramRoutesRef);
		this.busRoutesRef.addAll(busRoutesRef);
		this.numLeftLanes = numLeftLanes;
		this.numLeftOnlyLanes = numLeftOnlyLanes;
		this.numRightLanes = numRightLanes;
		this.numRightOnlyLanes = numRightOnlyLanes;
	}

	public void clearVehicles(){
		for (Lane lane : lanes) {
			lane.clearVehicles();
		}
	}

	public int getLaneCount(){
		return lanes.size();
	}

	public void addLane(Lane lane){
		lanes.add(lane);
	}

	public Lane getFirstLane(){
		if(!lanes.isEmpty()){
			return lanes.get(0);
		}
		return null;
	}

	public List<Lane> getLanes(){
		return Collections.unmodifiableList(lanes);
	}

	public Lane getLaneAwayFromRoadside(Lane currentLane){
		int nextLaneNumber = currentLane.laneNumber + 1;
		if(nextLaneNumber > -1 && nextLaneNumber < lanes.size()){
			return lanes.get(nextLaneNumber);
		}
		return null;
	}

	public Lane getLaneTowardsRoadside(Lane currentLane){
		int nextLaneNumber = currentLane.laneNumber - 1;
		if(nextLaneNumber > -1 && nextLaneNumber < lanes.size()){
			return lanes.get(nextLaneNumber);
		}
		return null;
	}

	public boolean isAllLanesOnLeftBlocked(int laneNumber){
		if (Settings.isDriveOnLeft) {
			for (int num = laneNumber - 1; num >= 0; num--) {
				if (!lanes.get(num).isBlocked) {
					return false;
				}
			}
		} else {
			for (int num = laneNumber + 1; num < lanes.size(); num++) {
				if (!lanes.get(num).isBlocked) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isAllLanesOnRightBlocked(int laneNumber) {
		if (Settings.isDriveOnLeft) {
			for (int num = laneNumber + 1; num < lanes.size(); num++) {
				if (!lanes.get(num).isBlocked) {
					return false;
				}
			}
		} else {
			for (int num = laneNumber - 1; num >= 0; num--) {
				if (!lanes.get(num).isBlocked) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isAllLanesOnRoadSideBlocked(int laneNumber){
		for (int num = laneNumber - 1; num >= 0; num--) {
			if (!lanes.get(num).isBlocked) {
				return false;
			}
		}
		return true;
	}

	public boolean isAllLanesAwayRoadSideBlocked(int laneNumber){
		for (int num = laneNumber + 1; num < lanes.size(); num++) {
			if (!lanes.get(num).isBlocked) {
				return false;
			}
		}
		return true;
	}

	public boolean isBlocked() {
		for (final Lane lane : lanes) {
			if (!lane.isBlocked) {
				return false;
			}
		}
		return true;
	}

	public Lane getLane(int laneNumber){
		if(laneNumber > -1 && laneNumber < lanes.size()) {
			return lanes.get(laneNumber);
		}
		return null;
	}

	public boolean isSuitableForRouteEndOfInternalVehicle() {
		return  !(length < Settings.minLengthOfRouteStartEndEdge) && !isRoundabout;
	}

	public boolean isSuitableForRouteStartOfInternalVehicle(List<GridCell> workareaCells) {
		// Note: route cannot start from cross-border edge at the starting side of the edge. This is to prevent problem in transferring of vehicle.
		return  !(length < Settings.minLengthOfRouteStartEndEdge) && !isRoundabout && workareaCells.contains(endNode.gridCell);
	}

	public boolean isOnTwoWayRoad() {
		for (final Edge edgeToCheck : endNode.outwardEdges) {
			if (edgeToCheck.endNode == startNode) {
				return true;
			}
		}
		return false;
	}

	public boolean isEdgeOnPathOfPriorityVehicle() {
		for (final Lane l : lanes) {
			if (l.isPriority) {
				return true;
			}
		}
		return false;
	}

	public boolean isEdgeContainsPriorityVehicle() {
		if (isEdgeOnPathOfPriorityVehicle()) {
			for (final Lane l : lanes) {
				if(l.hasPriorityVehicles()){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check whether an edge is on one-way road, i.e., there is no edge that
	 * links the same two end points but is in the opposite direction.
	 */
	boolean isOnOneWayRoad() {
		for (final Edge e : startNode.inwardEdges) {
			if (e.startNode == endNode) {
				return false;
			}
		}
		return true;
	}

	public void addParkedVehicle(Vehicle v){
		parkedVehicles.add(v);
	}

	public void removeParkedVehicle(Vehicle v){
		parkedVehicles.remove(v);
	}

	public void clearParkedVehicles(){
		parkedVehicles.clear();
	}
}
