package common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import processor.SimulationListener;
import processor.communication.externalMessage.ExternalSimulationListener;
import traffic.light.manager.TLManager;
import traffic.network.*;
import traffic.light.TrafficLightTiming;
import traffic.routing.Routing;
import traffic.vehicle.EmergencyStrategy;
import traffic.vehicle.lanedecide.LaneDecider;

/**
 * Global settings.
 *
 */
public class Settings {

	public Settings() {
	}

	public SettingsDictionary dictionary = new SettingsDictionary();

	/*
	 * Simulation
	 */
	public boolean isNewEnvironment = true;//True when certain settings are changed during setup, e.g., changing map
	public int maxNumSteps = 10000000;//Simulation stops when reaching this number of steps
	public double numStepsPerSecond = 5;//This determines the step length.
	public int pauseTimeBetweenStepsInMilliseconds = 0;//Can be used to adjust pace so a user can slow down simulation on GUI
	public int trafficReportStepGapInServerlessMode = 1;
	public int laneUpdateInterval = 300; // lane update time interval in steps

	/*
	 * Display
	 */
	public boolean isVisualize = false;
	public int controlPanelGapToRight = 20;
	public int controlPanelWidth = 450;

	/*
	 * Distributed computing
	 */
	public boolean isServerBased = true;//Whether workers receive instructions from server for each step
	public static boolean isSharedJVM = false;//Whether server and workers use the same JVM
	public static int numWorkers = 1;//Number of workers that run simulation in parallel
	public int numGridRows = 0;//Number of rows in the virtual grid covering simulation area
	public int numGridCols = 0;//Number of columns in the virtual grid covering simulation area
	public double maxGridCellWidthHeightInMeters = 500;//A cell should not be too large for load balancing purpose
	public static String serverAddress = "127.0.0.1";
	public static int serverListeningPortForWorkers = 50000;//Server's port for listening connection request initiated by worker

	/*
	 * Input
	 */
	public String inputSimulationScript = "script.txt";//Simulation setup file when GUI is not used
	public String inputOpenStreetMapFile = "C:/Users/pgunarathna/IdeaProjects/Temporary_update_smarts/resources/Grid_12x12.osm";//OSM file where road network information can be extracted
	public String inputBuiltinResource = "/resources/";//Directory where built-in resources are located
	public String inputBuiltinRoadGraph = inputBuiltinResource + "roads.txt";//Built-in road network data
	public String inputBuiltinAdministrativeRegionCentroid = inputBuiltinResource + "country_centroids_all.csv";//Coordinates of administrative regions
	public String inputForegroundVehicleFile = "";//Route file of foreground vehicles
	public String inputBackgroundVehicleFile = "";//Route file of background vehicles

	/*
	 * Output
	 */
	public boolean isOutputTrajectory = false;
	public boolean isOutputInitialRoutes = false;
	public boolean isOutputSimulationLog = false;
	public String prefixOutputTrajectory = "trajectory_";//Time-stamped GPS trajectory of vehicles
	public String prefixOutputRoutePlan = "route_";//Route plan of vehicles that have appeared in simulation
	public String prefixOutputSimLog = "log";//General statistic, e.g., simulation time
	public String prefixOutputForegroundTravelTime = "time_";//Travel time of foreground vehicles

	/*
	 * Road network
	 */
	public boolean isBuiltinRoadGraph = true;//Whether to use the default road network
	public double lonVsLat = 1.26574588;//Longitude-latitude ratio in terms of Euclidean distance per degree. This is re-computed when loading map.
	public int numLanesPerEdge = 0;//A positive value means all edges have the same number of lanes. '0' means the number is read from OSM data or set based on edge type.
	public String roadGraph = "";//Road graph data in a string.
	public double laneWidthInMeters = 3.25;
	public double pavementLineRatio = 1.0;

	/*
	 * Map data
	 */
	final public static String delimiterItem = "\u001e";
	final public static String delimiterSubItem = "\u001f";

	/*
	 * Traffic generation
	 */
	public int numGlobalRandomPrivateVehicles = 100;//Number of non-public vehicles in the whole area. These vehicles are generated internally.
	public int numGlobalRandomTrams = 0;//Number of trams in the whole area. These vehicles are generated internally.
	public int numGlobalRandomBuses = 0;//Number of buses in the whole area. These vehicles are generated internally.
	public boolean isAllowTramRule = true;//Whether non-tram vehicles wait for trams stopping at tram station
	public ArrayList<Double> driverProfileDistribution = new ArrayList<>(
			Arrays.asList(0.01, 0.1, 0.8, 0.1, 0.01));//Probability distribution of drivers with different profiles

	/*
	 * Routing
	 */
	public Routing.Algorithm routingAlgorithm = Routing.Algorithm.DIJKSTRA;//Routing algorithm
	public double minLengthOfRouteStartEndEdge = 20;//In meters. This may affect whether there is enough space to insert vehicle.
	public List<double[]> listRouteSourceWindowForInternalVehicle = new ArrayList<>();//List of windows where random routes start
	public List<double[]> listRouteDestinationWindowForInternalVehicle = new ArrayList<>();//List of windows where random routes end
	public List<double[]> listRouteSourceDestinationWindowForInternalVehicle = new ArrayList<>();//List of windows where random routes start or end

	public List<double[]> guiSourceWindowsForInternalVehicle = new ArrayList<>();//List of windows where random routes start
	public List<double[]> guiDestinationWindowsForInternalVehicle = new ArrayList<>();//List of windows where random routes end
	public List<double[]> guiSourceDestinationWindowsForInternalVehicle = new ArrayList<>();//List of windows where random routes start or end
	public boolean isAllowPriorityVehicleUseTramTrack = true;//Whether priority vehicles can use tram edge

	/*
	 * Vehicle model
	 */
	public double lookAheadDistance = 50;//In meters. Vehicle looks for impeding objects within a certain distance.
	public double intersectionSpeedThresholdOfFront = 0;//In m/s. If the speed of front vehicle is lower than this value, its back vehicle may not cross intersections between them.
	public double minTimeSafeToCrossIntersection = 1;//In seconds. This affects how vehicle reacts to conflict traffic at intersection.
	public double periodOfTrafficWaitForTramAtStop = 20;//In seconds. How long a tram needs to wait at tram stop.
	public double minGapBetweenTramStopTimerCountDowns = 3;//In seconds. When tram stop timer reaches 0, it cannot be triggered again immediately when this value is positive.
	public boolean isAllowReroute = false;//Whether imported vehicles can change routes automatically in congested traffic
	public int maxNumReRouteOfInternalVehicle = 10;//The system removes an internal vehicle if it has been re-routed for too many times.
	public EmergencyStrategy emergencyStrategy = EmergencyStrategy.NonEmergencyPullOffToRoadside;//How non-priority vehicle reacts to priority vehicles, e.g., ambulance, police car, etc.
	public double congestionSpeedThreshold = 1;//In m/s. The maximum speed of a traffic congestion.
	public boolean isDriveOnLeft = true;

	/*
	 * Traffic light
	 */
	public TrafficLightTiming trafficLightTiming = TrafficLightTiming.FIXED;//Current traffic light timing strategy
	public double trafficLightDetectionDistance = 30;//In meters. How far a vehicle can see a light.
	public double maxLightGroupRadius = 10;//In meters. Controls size of the area where a cluster of lights can be identified.

	/**
	 * OD Distribution
	 */
	//public static ODDistributor odDistributor = new RandomODDistributor();
	//public static TemporalDistributor temporalDistributor = new UniformTemporalDistributor();
	public String trafficGenerator = "RushHour";
	public String odDistributor = "Random";
	public String temporalDistributor = "Uniform";
	public String vehicleTypeDistributor = "Default";
	public double safetyHeadwayMultiplier = 0.1;
	public String defaultDownloadDirectory = "download";
	public String defaultTestName = null;
	public int defaultRunIndex = 1;
	public String downloadDirectory = defaultDownloadDirectory;
	public String testName = defaultTestName;
	public int runIndex = defaultRunIndex;
	public boolean inputOnlyODPairsOfForegroundVehicleFile = false;
	public int updateStepInterval = 1;
	public String simulationListener = "";
	public double startGapOffset = 0.0;
	public double startPosOffset = 0.0;
	public boolean stopsAtMaxSteps = true;
	public int gridlockDetectionTime = 600;
	public String tlManager = "FIXED";
	public String laneDecide = "UNBALANCED";
	public String externalListner = "CLLA";

	/**
	 * External Listener Settings
	 */
	public boolean isExternalListenerUsed = true;

	public String getOutputPrefix (){
		if (!isExternalListenerUsed) return "noLA_"+String.valueOf(dictionary.getDemand())+"_";
		else return externalListner+"_"+String.valueOf(dictionary.getDemand())+"_";
	}

	public TrafficGenerator getTrafficGenerator() {
		return dictionary.getTrafficGenerator(trafficGenerator);
	}

	public void setTrafficGenerator(String trafficGenerator) {
		this.trafficGenerator = trafficGenerator;
	}

	public ExternalSimulationListener getExternalSimulationListener(){
		return dictionary.getExternalSimulationListener(externalListner);
	}

	public ODDistributor getODDistributor(){
		return dictionary.getODDistributor(odDistributor);
	}

	public TemporalDistributor getTemporalDistributor(){
		return dictionary.getTemporalDistributor(temporalDistributor);
	}

	public VehicleTypeDistributor getVehicleTypeDistributor(){
		return dictionary.getVehicleTypeDistributor(vehicleTypeDistributor);
	}

	public SimulationListener getSimulationListener(){
		return dictionary.getSimulationListener(simulationListener);
	}

	public TLManager getLightScheduler(){
		if(tlManager != null && !tlManager.isEmpty()) {
			return dictionary.getTLScheduler(tlManager);
		}
		return dictionary.getTLScheduler(trafficLightTiming.name());
	}

	public LaneDecider getLaneDecider(){
		return dictionary.getLaneDecider(laneDecide);
	}

}
