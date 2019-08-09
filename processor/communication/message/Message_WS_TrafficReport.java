package processor.communication.message;

import java.util.ArrayList;
import java.util.List;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.light.LightCoordinator;
import traffic.light.Phase;
import traffic.light.TrafficLightCluster;
import traffic.light.TrafficLightTiming;
import traffic.road.Edge;
import traffic.routing.RouteLeg;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;
import traffic.vehicle.VehicleUtil;

/**
 * Worker-to-server message that is sent by worker after simulating one step.
 * This message is only used if synchronization involves server. Information
 * contained in this message can be used for updating GUI.
 *
 */
public class Message_WS_TrafficReport {
	public String workerName;
	public ArrayList<Serializable_GUI_Vehicle> vehicleList = new ArrayList<>();
	public ArrayList<Serializable_GUI_Light> lightList = new ArrayList<>();
	public ArrayList<SerializableRouteDump> randomRoutes = new ArrayList<>();
	public ArrayList<Serializable_Finished_Vehicle> finishedList = new ArrayList<>();
	public int step;
	public int numInternalNonPubVehicles;
	public int numInternalTrams;
	public int numInternalBuses;

	public Message_WS_TrafficReport() {

	}

	public Message_WS_TrafficReport(final String workerName, final ArrayList<Vehicle> vehiclesOnRoad,
									final LightCoordinator lightCoordinator, final ArrayList<Vehicle> newVehiclesSinceLastReport,
									List<Vehicle> vehiclesFinished,final int step,
									final int numInternalNonPubVehicles, final int numInternalTrams, final int numInternalBuses) {
		this.workerName = workerName;
		if (Settings.isVisualize || Settings.isOutputTrajectory) {
			vehicleList = getDetailOfActiveVehiclesOnRoad(vehiclesOnRoad);
		}
		if (Settings.isVisualize && Settings.trafficLightTiming != TrafficLightTiming.NONE) {
			lightList = getDetailOfLights(lightCoordinator);
		}
		if (Settings.isOutputInitialRoutes) {
			randomRoutes = getInitialRouteList(newVehiclesSinceLastReport);
		}
		this.finishedList = new ArrayList<>(getFinishedListFromVehicles(vehiclesFinished, step/ Settings.numStepsPerSecond));
		this.step = step;
		this.numInternalNonPubVehicles = numInternalNonPubVehicles;
		this.numInternalTrams = numInternalTrams;
		this.numInternalBuses = numInternalBuses;
	}

	public Message_WS_TrafficReport(final String workerName, final int step, final TrafficNetwork trafficNetwork){
		this(workerName, trafficNetwork.vehicles, trafficNetwork.lightCoordinator,
				trafficNetwork.newVehiclesSinceLastReport, trafficNetwork.getFinishedVehicles(), step,
				trafficNetwork.numInternalNonPublicVehicle,
				trafficNetwork.numInternalTram, trafficNetwork.numInternalBus);
	}

	ArrayList<Serializable_GUI_Vehicle> getDetailOfActiveVehiclesOnRoad(final ArrayList<Vehicle> vehicles) {
		final ArrayList<Serializable_GUI_Vehicle> list = new ArrayList<>();
		for (final Vehicle v : vehicles) {
			if (v.active && (v.lane != null)) {
				final Serializable_GUI_Vehicle sVehicle = new Serializable_GUI_Vehicle();
				if ((v.type == VehicleType.TRAM) && (v.lane.edge.timeTramStopping > 0)) {
					sVehicle.type = v.type.name() + "@Stop";
				} else {
					sVehicle.type = v.type.name();
				}
				sVehicle.speed = v.speed;
				sVehicle.acceleration = v.acceleration;
				final double[] coordinates = VehicleUtil.calculateCoordinates(v);
				sVehicle.lonHead = coordinates[0];
				sVehicle.latHead = coordinates[1];
				sVehicle.lonTail = coordinates[2];
				sVehicle.latTail = coordinates[3];
				sVehicle.length = v.type.length;
				sVehicle.width = v.type.width;
				sVehicle.numLinksToGo = v.getRouteLegCount() - 1 - v.indexLegOnRoute;
				sVehicle.id = v.id;
				sVehicle.vid = v.vid;
				sVehicle.worker = workerName;
				sVehicle.driverProfile = v.driverProfile.name();
				sVehicle.slowDownFactor = v.getRecentSlowDownFactor();
				sVehicle.edgeIndex = v.lane.edge.index;
				sVehicle.laneIndex = v.lane.index;
				sVehicle.originalEdgeMaxSpeed = v.lane.edge.freeFlowSpeed;
				sVehicle.isAffectedByPriorityVehicle = v.isAffectedByPriorityVehicle;
				list.add(sVehicle);
			}
		}

		return list;
	}

	ArrayList<Serializable_GUI_Light> getDetailOfLights(final LightCoordinator lightCoordinator) {
		final ArrayList<Serializable_GUI_Light> list = new ArrayList<>();
		for (final TrafficLightCluster edgeGroups : lightCoordinator.lightClusters) {
			for (Phase phase : edgeGroups.phases) {
				for (final Edge e : phase.getEdges()) {
					final double lightPositionToEdgeRatio = (e.length - 1) / e.length;
					final double latitude = (e.startNode.lat
							+ ((e.endNode.lat - e.startNode.lat) * lightPositionToEdgeRatio));
					final double longitude = (e.startNode.lon
							+ ((e.endNode.lon - e.startNode.lon) * lightPositionToEdgeRatio));
					list.add(new Serializable_GUI_Light(longitude, latitude, e.lightColor.color));
				}
			}
		}

		return list;
	}

	ArrayList<SerializableRouteDump> getInitialRouteList(final ArrayList<Vehicle> vehicles) {
		final ArrayList<SerializableRouteDump> list = new ArrayList<>();

		for (final Vehicle vehicle : vehicles) {
			final ArrayList<SerializableRouteDumpPoint> routeDumpPoints = new ArrayList<>();
			final SerializableRouteDumpPoint startPoint = new SerializableRouteDumpPoint(
					vehicle.getRouteLegEdge(0).startNode.osmId, vehicle.getRouteLeg(0).stopover);
			routeDumpPoints.add(startPoint);
			for (final RouteLeg routeLeg : vehicle.getRouteLegs()) {
				final SerializableRouteDumpPoint point = new SerializableRouteDumpPoint(routeLeg.edge.endNode.osmId,
						routeLeg.stopover);
				routeDumpPoints.add(point);
			}
			list.add(new SerializableRouteDump(vehicle.id, vehicle.vid, vehicle.type.name(), vehicle.timeRouteStart,
					vehicle.getRouteLeg(0).edge.startNode.index,
					vehicle.getRouteLeg(vehicle.getRouteLegCount()-1).edge.endNode.index,
					vehicle.getBestTravelTime(), vehicle.getRouteLength(), vehicle.getRouteString(), routeDumpPoints,
					vehicle.driverProfile.name()));
		}

		return list;
	}

	ArrayList<SerializableVehicleSpeed> getVehicleSpeedList(final ArrayList<Double> vehicleSpeedOnCalibratedEdges) {
		final ArrayList<SerializableVehicleSpeed> list = new ArrayList<>();
		for (final double speed : vehicleSpeedOnCalibratedEdges) {
			list.add(new SerializableVehicleSpeed(speed));
		}
		return list;
	}

	public List<Serializable_Finished_Vehicle> getFinishedListFromVehicles(List<Vehicle> vehiclesFinished, double timeNow){
		List<Serializable_Finished_Vehicle> finishedVehicles = new ArrayList<>();
		for (Vehicle vehicle : vehiclesFinished) {
			Serializable_Finished_Vehicle finishedVehicle = new Serializable_Finished_Vehicle(vehicle.id, vehicle.vid,
					vehicle.getRouteLeg(0).edge.startNode.index,
					vehicle.getRouteLeg(vehicle.getRouteLegCount()-1).edge.endNode.index,
					vehicle.getBestTravelTime(), timeNow - vehicle.timeRouteStart, vehicle.getRouteLength(), vehicle.getRouteString());
			finishedVehicles.add(finishedVehicle);
		}
		return finishedVehicles;
	}



	public ArrayList<Serializable_Finished_Vehicle> getFinishedList() {
		return finishedList;
	}

}
