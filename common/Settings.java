package common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import processor.SimulationListener;
import traffic.light.schedule.TLScheduler;
import traffic.network.*;
import traffic.light.TrafficLightTiming;
import traffic.routing.Routing;
import traffic.vehicle.EmergencyStrategy;

/**
 * Global settings.
 *
 */
public class Settings {

	public static SettingsDictionary dictionary = new SettingsDictionary();

	/*
	 * Simulation
	 */
	public static boolean isNewEnvironment = true;//True when certain settings are changed during setup, e.g., changing map
	public static int maxNumSteps = 10000000;//Simulation stops when reaching this number of steps
	public static double numStepsPerSecond = 5;//This determines the step length.
	public static int pauseTimeBetweenStepsInMilliseconds = 0;//Can be used to adjust pace so a user can slow down simulation on GUI
	public static int trafficReportStepGapInServerlessMode = 1;

	/*
	 * Display
	 */
	public static boolean isVisualize = true;
	public static int controlPanelGapToRight = 20;
	public static int controlPanelWidth = 450;

	/*
	 * Distributed computing
	 */
	public static boolean isServerBased = true;//Whether workers receive instructions from server for each step
	public static boolean isSharedJVM = false;//Whether server and workers use the same JVM
	public static int numWorkers = 1;//Number of workers that run simulation in parallel
	public static int numGridRows = 0;//Number of rows in the virtual grid covering simulation area
	public static int numGridCols = 0;//Number of columns in the virtual grid covering simulation area
	public static double maxGridCellWidthHeightInMeters = 500;//A cell should not be too large for load balancing purpose
	public static String serverAddress = "127.0.0.1";
	public static int serverListeningPortForWorkers = 50000;//Server's port for listening connection request initiated by worker

	/*
	 * Input
	 */
	public static String inputSimulationScript = "script.txt";//Simulation setup file when GUI is not used
	public static String inputOpenStreetMapFile = "cbd.osm";//OSM file where road network information can be extracted
	public static String inputBuiltinResource = "/resources/";//Directory where built-in resources are located
	public static String inputBuiltinRoadGraph = inputBuiltinResource + "roads.txt";//Built-in road network data
	public static String inputBuiltinAdministrativeRegionCentroid = inputBuiltinResource + "country_centroids_all.csv";//Coordinates of administrative regions
	public static String inputForegroundVehicleFile = "";//Route file of foreground vehicles
	public static String inputBackgroundVehicleFile = "";//Route file of background vehicles

	/*
	 * Output
	 */
	public static boolean isOutputTrajectory = false;
	public static boolean isOutputInitialRoutes = false;
	public static boolean isOutputSimulationLog = false;
	public static String prefixOutputTrajectory = "trajectory_";//Time-stamped GPS trajectory of vehicles
	public static String prefixOutputRoutePlan = "route_";//Route plan of vehicles that have appeared in simulation
	public static String prefixOutputSimLog = "log";//General statistic, e.g., simulation time
	public static String prefixOutputForegroundTravelTime = "time_";//Travel time of foreground vehicles

	/*
	 * Road network
	 */
	public static boolean isBuiltinRoadGraph = true;//Whether to use the default road network
	public static double lonVsLat = 1.26574588;//Longitude-latitude ratio in terms of Euclidean distance per degree. This is re-computed when loading map.
	public static int numLanesPerEdge = 0;//A positive value means all edges have the same number of lanes. '0' means the number is read from OSM data or set based on edge type.
	public static String roadGraph = "";//Road graph data in a string. 
	public static double laneWidthInMeters = 3.25;
	public static double pavementLineRatio = 1.0;

	/*
	 * Map data
	 */
	final public static String delimiterItem = "\u001e";
	final public static String delimiterSubItem = "\u001f";

	/*
	 * Traffic generation
	 */
	public static int numGlobalRandomPrivateVehicles = 100;//Number of non-public vehicles in the whole area. These vehicles are generated internally.
	public static int numGlobalRandomTrams = 0;//Number of trams in the whole area. These vehicles are generated internally.
	public static int numGlobalRandomBuses = 0;//Number of buses in the whole area. These vehicles are generated internally.
	public static boolean isAllowTramRule = true;//Whether non-tram vehicles wait for trams stopping at tram station
	public static ArrayList<Double> driverProfileDistribution = new ArrayList<>(
			Arrays.asList(0.01, 0.1, 0.8, 0.1, 0.01));//Probability distribution of drivers with different profiles

	/*
	 * Routing
	 */
	public static Routing.Algorithm routingAlgorithm = Routing.Algorithm.DIJKSTRA;//Routing algorithm
	public static double minLengthOfRouteStartEndEdge = 20;//In meters. This may affect whether there is enough space to insert vehicle.
	public static List<double[]> listRouteSourceWindowForInternalVehicle = new ArrayList<>();//List of windows where random routes start
	public static List<double[]> listRouteDestinationWindowForInternalVehicle = new ArrayList<>();//List of windows where random routes end
	public static List<double[]> listRouteSourceDestinationWindowForInternalVehicle = new ArrayList<>();//List of windows where random routes start or end

	public static List<double[]> guiSourceWindowsForInternalVehicle = new ArrayList<>();//List of windows where random routes start
	public static List<double[]> guiDestinationWindowsForInternalVehicle = new ArrayList<>();//List of windows where random routes end
	public static List<double[]> guiSourceDestinationWindowsForInternalVehicle = new ArrayList<>();//List of windows where random routes start or end
	public static boolean isAllowPriorityVehicleUseTramTrack = true;//Whether priority vehicles can use tram edge

	/*
	 * Vehicle model
	 */
	public static double lookAheadDistance = 50;//In meters. Vehicle looks for impeding objects within a certain distance.
	public static double intersectionSpeedThresholdOfFront = 2.78;//In m/s. If the speed of front vehicle is lower than this value, its back vehicle may not cross intersections between them. 
	public static double minTimeSafeToCrossIntersection = 5;//In seconds. This affects how vehicle reacts to conflict traffic at intersection.
	public static double periodOfTrafficWaitForTramAtStop = 20;//In seconds. How long a tram needs to wait at tram stop.
	public static double minGapBetweenTramStopTimerCountDowns = 3;//In seconds. When tram stop timer reaches 0, it cannot be triggered again immediately when this value is positive. 
	public static boolean isAllowReroute = false;//Whether imported vehicles can change routes automatically in congested traffic
	public static int maxNumReRouteOfInternalVehicle = 10;//The system removes an internal vehicle if it has been re-routed for too many times.
	public static EmergencyStrategy emergencyStrategy = EmergencyStrategy.NonEmergencyPullOffToRoadside;//How non-priority vehicle reacts to priority vehicles, e.g., ambulance, police car, etc.
	public static double congestionSpeedThreshold = 1;//In m/s. The maximum speed of a traffic congestion.
	public static boolean isDriveOnLeft = true;

	/*
	 * Traffic light
	 */
	public static TrafficLightTiming trafficLightTiming = TrafficLightTiming.FIXED;//Current traffic light timing strategy
	public static double trafficLightDetectionDistance = 30;//In meters. How far a vehicle can see a light.
	public static double maxLightGroupRadius = 60;//In meters. Controls size of the area where a cluster of lights can be identified.

	/**
	 * OD Distribution
	 */
	//public static ODDistributor odDistributor = new RandomODDistributor();
	//public static TemporalDistributor temporalDistributor = new UniformTemporalDistributor();
	public static String odDistributor = "Random";
	public static String temporalDistributor = "Uniform";
	public static String vehicleTypeDistributor = "Default";
	public static double safetyHeadwayMultiplier = 1;
	public static String defaultDownloadDirectory = "download";
	public static String defaultTestName = null;
	public static int defaultRunIndex = 1;
	public static String downloadDirectory = defaultDownloadDirectory;
	public static String testName = defaultTestName;
	public static int runIndex = defaultRunIndex;
	public static boolean inputOnlyODPairsOfForegroundVehicleFile = false;
	public static int updateStepInterval = 1;
	public static String simulationListener = "";
	public static double startGapOffset = 0.0;
	public static double startPosOffset = 0.0;
	public static boolean stopsAtMaxSteps = true;
	public static int gridlockDetectionTime = 600;

	public static ODDistributor getODDistributor(){
		return dictionary.getODDistributor(odDistributor);
	}

	public static TemporalDistributor getTemporalDistributor(){
		return dictionary.getTemporalDistributor(temporalDistributor);
	}

	public static VehicleTypeDistributor getVehicleTypeDistributor(){
		return dictionary.getVehicleTypeDistributor(vehicleTypeDistributor);
	}

	public static SimulationListener getSimulationListener(){
		return dictionary.getSimulationListener(simulationListener);
	}

	public static TLScheduler getLightScheduler(){
		return dictionary.getTLScheduler(trafficLightTiming.name());
	}
}
