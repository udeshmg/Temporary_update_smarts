package processor.communication.message;

import java.util.ArrayList;
import java.util.List;

import common.Settings;
import processor.server.WorkerMeta;
import traffic.light.LightUtil;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.routing.RouteUtil;

/**
 * Server-to-worker message containing the simulation configuration. Worker will
 * set up simulation environment upon receiving this message.
 */
public class Message_SW_Setup {
	public boolean isNewEnvironment;
	public int numWorkers;
	public int startStep;
	public int maxNumSteps;
	public double numStepsPerSecond;
	public int workerToServerReportStepGapInServerlessMode;
	public double periodOfTrafficWaitForTramAtStop;
	public ArrayList<SerializableDouble> driverProfileDistribution = new ArrayList<>();
	public double lookAheadDistance;
	public String trafficLightTiming;
	public boolean isVisualize;
	public int numRandomPrivateVehicles;
	public int numRandomTrams;
	public int numRandomBuses;
	public ArrayList<SerializableExternalVehicle> externalRoutes = new ArrayList<>();
	/**
	 * Metadata of all workers
	 */
	public ArrayList<SerializableWorkerMetadata> metadataWorkers = new ArrayList<>();
	public boolean isServerBased;
	public String roadGraph;
	public String routingAlgorithm;
	/**
	 * Index of nodes where traffic light is added by user
	 */
	public ArrayList<SerializableInt> indexNodesToAddLight = new ArrayList<>();
	/**
	 * Index of nodes where traffic light is removed by user
	 */
	public ArrayList<SerializableInt> indexNodesToRemoveLight = new ArrayList<>();
	public boolean isAllowPriorityVehicleUseTramTrack;
	public boolean isOutputForegroundTrajectory = false;
	public boolean isOutputInternalBackgroundRoutePlan = false;
	public ArrayList<Serializable_GPS_Rectangle> listRouteSourceWindowForInternalVehicle = new ArrayList<>();
	public ArrayList<Serializable_GPS_Rectangle> listRouteDestinationWindowForInternalVehicle = new ArrayList<>();
	public ArrayList<Serializable_GPS_Rectangle> listRouteSourceDestinationWindowForInternalVehicle = new ArrayList<>();
	public boolean isAllowReroute = false;
	public boolean isAllowTramRule = true;
	public boolean isDriveOnLeft;

	public Message_SW_Setup() {

	}

	public Message_SW_Setup(final ArrayList<WorkerMeta> workers, final WorkerMeta workerToReceiveMessage,
			final ArrayList<Edge> edges, final int step, final ArrayList<Node> nodesToAddLight,
			final ArrayList<Node> nodesToRemoveLight) {
		isNewEnvironment = Settings.isNewEnvironment;
		numWorkers = Settings.numWorkers;
		startStep = step;
		maxNumSteps = Settings.maxNumSteps;
		numStepsPerSecond = Settings.numStepsPerSecond;
		workerToServerReportStepGapInServerlessMode = Settings.trafficReportStepGapInServerlessMode;
		periodOfTrafficWaitForTramAtStop = Settings.periodOfTrafficWaitForTramAtStop;
		driverProfileDistribution = getDriverProfilePercentage(Settings.driverProfileDistribution);
		lookAheadDistance = Settings.lookAheadDistance;
		trafficLightTiming = Settings.trafficLightTiming.name();
		isVisualize = Settings.isVisualize;
		metadataWorkers = appendMetadataOfWorkers(workers);
		numRandomPrivateVehicles = workerToReceiveMessage.numRandomPrivateVehicles;
		numRandomTrams = workerToReceiveMessage.numRandomTrams;
		numRandomBuses = workerToReceiveMessage.numRandomBuses;
		externalRoutes = workerToReceiveMessage.externalRoutes;
		isServerBased = Settings.isServerBased;
		if (Settings.isNewEnvironment) {
			if (Settings.isBuiltinRoadGraph) {
				roadGraph = "builtin";
			} else {
				roadGraph = Settings.roadGraph;
			}
		} else {
			roadGraph = "";
		}
		routingAlgorithm = Settings.routingAlgorithm.name();
		isAllowPriorityVehicleUseTramTrack = Settings.isAllowPriorityVehicleUseTramTrack;
		indexNodesToAddLight = getLightNodeIndex(nodesToAddLight);
		indexNodesToRemoveLight = getLightNodeIndex(nodesToRemoveLight);
		isOutputForegroundTrajectory = Settings.isOutputTrajectory;
		isOutputInternalBackgroundRoutePlan = Settings.isOutputInitialRoutes;
		listRouteSourceWindowForInternalVehicle = getListRouteWindow(Settings.listRouteSourceWindowForInternalVehicle);
		listRouteDestinationWindowForInternalVehicle = getListRouteWindow(
				Settings.listRouteDestinationWindowForInternalVehicle);
		listRouteSourceDestinationWindowForInternalVehicle = getListRouteWindow(
				Settings.listRouteSourceDestinationWindowForInternalVehicle);
		isAllowReroute = Settings.isAllowReroute;
		isAllowTramRule = Settings.isAllowTramRule;
		isDriveOnLeft = Settings.isDriveOnLeft;
	}

	ArrayList<SerializableWorkerMetadata> appendMetadataOfWorkers(final ArrayList<WorkerMeta> workers) {
		final ArrayList<SerializableWorkerMetadata> listSerializableWorkerMetadata = new ArrayList<>();
		for (final WorkerMeta worker : workers) {
			listSerializableWorkerMetadata.add(new SerializableWorkerMetadata(worker));
		}
		return listSerializableWorkerMetadata;
	}

	ArrayList<SerializableDouble> getDriverProfilePercentage(final ArrayList<Double> percentages) {
		final ArrayList<SerializableDouble> list = new ArrayList<>();
		for (final Double percentage : percentages) {
			list.add(new SerializableDouble(percentage));
		}
		return list;
	}

	ArrayList<SerializableInt> getLightNodeIndex(final ArrayList<Node> nodes) {
		final ArrayList<SerializableInt> list = new ArrayList<>();
		for (final Node node : nodes) {
			list.add(new SerializableInt(node.index));
		}
		return list;
	}

	ArrayList<Serializable_GPS_Rectangle> getListRouteWindow(final List<double[]> windows) {
		final ArrayList<Serializable_GPS_Rectangle> list = new ArrayList<>();
		for (final double[] window : windows) {
			list.add(new Serializable_GPS_Rectangle(window[0], window[1], window[2], window[3]));
		}
		return list;
	}

	public void setupFromMessage(){
		Settings.numWorkers = numWorkers;
		Settings.maxNumSteps = maxNumSteps;
		Settings.numStepsPerSecond = numStepsPerSecond;
		Settings.trafficReportStepGapInServerlessMode = workerToServerReportStepGapInServerlessMode;
		Settings.periodOfTrafficWaitForTramAtStop = periodOfTrafficWaitForTramAtStop;
		Settings.driverProfileDistribution = setDriverProfileDistribution(driverProfileDistribution);
		Settings.lookAheadDistance = lookAheadDistance;
		Settings.trafficLightTiming = LightUtil.getLightTypeFromString(trafficLightTiming);
		Settings.isVisualize = isVisualize;
		Settings.isServerBased = isServerBased;
		Settings.routingAlgorithm = RouteUtil.getRoutingAlgorithmFromString(routingAlgorithm);
		Settings.isAllowPriorityVehicleUseTramTrack = isAllowPriorityVehicleUseTramTrack;
		Settings.isOutputInitialRoutes = isOutputInternalBackgroundRoutePlan;
		Settings.isOutputTrajectory = isOutputForegroundTrajectory;
		Settings.listRouteSourceWindowForInternalVehicle = setRouteSourceDestinationWindow(listRouteSourceWindowForInternalVehicle);
		Settings.listRouteDestinationWindowForInternalVehicle = setRouteSourceDestinationWindow(listRouteDestinationWindowForInternalVehicle);
		Settings.listRouteSourceDestinationWindowForInternalVehicle = setRouteSourceDestinationWindow(listRouteSourceDestinationWindowForInternalVehicle);
		Settings.isAllowReroute = isAllowReroute;
		Settings.isAllowTramRule = isAllowTramRule;
		Settings.isDriveOnLeft = isDriveOnLeft;
	}

	/**
	 * Set the percentage of drivers with different profiles, from highly
	 * aggressive to highly polite.
	 */
	ArrayList<Double> setDriverProfileDistribution(final ArrayList<SerializableDouble> sList) {
		final ArrayList<Double> list = new ArrayList<>();
		for (final SerializableDouble sd : sList) {
			list.add(sd.value);
		}
		return list;
	}

	ArrayList<double[]> setRouteSourceDestinationWindow(final ArrayList<Serializable_GPS_Rectangle> sList) {
		final ArrayList<double[]> list = new ArrayList<>();
		for (final Serializable_GPS_Rectangle sgr : sList) {
			list.add(new double[] { sgr.minLon, sgr.maxLat, sgr.maxLon, sgr.minLat });
		}
		return list;
	}
}
