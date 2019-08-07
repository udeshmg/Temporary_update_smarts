package processor;

import common.Settings;
import osm.OSM;
import processor.communication.message.*;
import processor.server.*;
import processor.server.gui.GUI;
import traffic.road.Node;
import traffic.road.RoadNetwork;
import traffic.road.RoadUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Copyright (c) 2019, The University of Melbourne.
 * All rights reserved.
 * <p>
 * You are permitted to use the code for research purposes provided that the following conditions are met:
 * <p>
 * * You cannot redistribute any part of the code.
 * * You must give appropriate citations in any derived work.
 * * You cannot use any part of the code for commercial purposes.
 * * The copyright holder reserves the right to change the conditions at any time without prior notice.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * Created by tmuthugama on 3/18/2019
 */
public class SimServerData {
    private RoadNetwork roadNetwork;
    private GUI gui;
    private ConsoleUI consoleUI;
    private ScriptLoader scriptLoader = new ScriptLoader();
    private FileOutput fileOutput = new FileOutput();
    private double simulationWallTime = 0;//Total time length spent on simulation
    private int totalNumWwCommChannels = 0;//Total number of communication channels between workers. A worker has two channels with a neighbor worker, one for sending and one for receiving.
    private ArrayList<SerializableRouteDump> allRoutes = new ArrayList<SerializableRouteDump>();
    private HashMap<String, TreeMap<Double, double[]>> allTrajectories = new HashMap<String, TreeMap<Double, double[]>>();
    private long timeStamp = 0;
    private int numInternalNonPubVehiclesAtAllWorkers = 0;
    private int numInternalTramsAtAllWorkers = 0;
    private int numInternalBusesAtAllWorkers = 0;
    private ArrayList<Node> nodesToAddLight = new ArrayList<>();
    private ArrayList<Node> nodesToRemoveLight = new ArrayList<>();
    private int numVehiclesCreatedDuringSetup = 0;//For updating setup progress on GUI
    private int numVehiclesNeededAtStart = 0;//For updating setup progress on GUI
    private int step = 0;//Time step in the current simulation
    private TrjOutput trjOutput;
    private VehicleDataOutput vdOutput;
    private List<Experiment> experiments = null;
    private int experimentIndex = 0;
    private int runIndex = 0;

    public void showNumberOfConnectedWorkers(int number) {
        if (Settings.isVisualize) {
            gui.updateNumConnectedWorkers(number);
        } else {
            System.out.println(number + "/" + Settings.numWorkers + " workers connected.");
        }
    }

    public void initRoadNetwork() {
        // Load default road network
        Settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile();
        roadNetwork = new RoadNetwork();
    }

    public void initUIs(SimulationProcessor processor) {
        // Start GUI or load simulation configuration without GUI
        if (Settings.isVisualize) {
            if (gui != null) {
                gui.dispose();
            }
            final GUI newGUI = new GUI(processor);
            gui = newGUI;
            gui.setVisible(true);
        } else {
            consoleUI = new ConsoleUI(processor);
            consoleUI.acceptInitialConfigFromConsole();
        }
    }

    public void prepareForSetup() {
        if (Settings.isVisualize) {
            gui.getReadyToSetup();
        } else {
            consoleUI.acceptSimScriptFromConsole();// Let user input simulation script path
        }
    }

    public void prepareForSetupAgain() {
        updateExperimentIndices();
        if ((Settings.isVisualize)) {
            gui.getReadyToSetup();
            if (experiments != null && experiments.size() != experimentIndex) {
                System.out.println("Loading configuration of new simulation...");
                runAutomatedSim();
            }
        } else {
            consoleUI.acceptConsoleCommandAtSimEnd();
            if (experiments != null && experiments.size() != experimentIndex) {
                System.out.println("Loading configuration of new simulation...");
                startSimulationFromLoadedScript();
            }
        }
    }

    public void updateFromReport(final ArrayList<Serializable_GUI_Vehicle> vehicleList,
                                 final ArrayList<Serializable_GUI_Light> lightList, final String workerName, final int numWorkers,
                                 final int step, ArrayList<SerializableRouteDump> randomRoutes,
                                 ArrayList<Serializable_Finished_Vehicle> finished, int numInternalNonPubVehicles,
                                 int numInternalTrams, int numInternalBuses) {
        // Update GUI
        if (Settings.isVisualize) {
            gui.updateObjectData(vehicleList, lightList, workerName, numWorkers, step);
        }
        // Add new vehicle position to its trajectory
        double timeStamp = step / Settings.numStepsPerSecond;
        if (Settings.isOutputTrajectory) {
            for (Serializable_GUI_Vehicle vehicle : vehicleList) {
                if (!allTrajectories.containsKey(vehicle.id)) {
                    allTrajectories.put(vehicle.id, new TreeMap<Double, double[]>());
                }
                allTrajectories.get(vehicle.id).put(timeStamp, new double[]{vehicle.latHead, vehicle.lonHead});
            }
        }
        for (Serializable_Finished_Vehicle finishedVehicle : finished) {
            vdOutput.outputVehicleData(finishedVehicle);
        }
        trjOutput.outputTrajData(step, vehicleList);
        // Store routes of new vehicles created since last report
        if (Settings.isOutputInitialRoutes) {
            allRoutes.addAll(randomRoutes);
        }
        // Increment vehicle counts
        numInternalNonPubVehiclesAtAllWorkers += numInternalNonPubVehicles;
        numInternalTramsAtAllWorkers += numInternalTrams;
        numInternalBusesAtAllWorkers += numInternalBuses;
    }

    public void startSimulationInUI() {
        if (Settings.isVisualize) {
            gui.startSimulation();
        }
    }

    public void resetStepInUI() {
        if (Settings.isVisualize) {
            gui.stepToDraw = 0;
        }
    }

    public void updateSetupprogressInUI(int created) {
        numVehiclesCreatedDuringSetup += created;
        double createdVehicleRatio = (double) numVehiclesCreatedDuringSetup / numVehiclesNeededAtStart;
        if (createdVehicleRatio > 1) {
            createdVehicleRatio = 1;
        }

        if (Settings.isVisualize) {
            gui.updateSetupProgress(createdVehicleRatio);
        }
    }

    public RoadNetwork getRoadNetwork() {
        return roadNetwork;
    }

    public void changeMap() {
        Settings.listRouteSourceWindowForInternalVehicle.clear();
        Settings.listRouteDestinationWindowForInternalVehicle.clear();
        Settings.listRouteSourceDestinationWindowForInternalVehicle.clear();
        Settings.isNewEnvironment = true;

        if (Settings.inputOpenStreetMapFile.length() > 0) {
            final OSM osm = new OSM();
            osm.processOSM(Settings.inputOpenStreetMapFile, true);
            Settings.isBuiltinRoadGraph = false;
            // Revert to built-in map if there was an error when converting new map
            if (Settings.roadGraph.length() == 0) {
                Settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile();
                Settings.isBuiltinRoadGraph = true;
            }
        } else {
            Settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile();
            Settings.isBuiltinRoadGraph = true;
        }

        roadNetwork = new RoadNetwork();//Build road network based on new road graph
    }

    public void initFileOutput() {
        // Initialize output
        fileOutput.init();
        trjOutput = new TrjOutput(fileOutput.getTrjFos(), Settings.numWorkers, (int) Settings.numStepsPerSecond, Settings.maxNumSteps,
                roadNetwork.minLon, roadNetwork.minLat, roadNetwork.mapWidth, roadNetwork.mapHeight);
        trjOutput.outputMapData();
        vdOutput = new VehicleDataOutput(fileOutput.getVdFos());
    }

    public void writeOutputFiles(int step) {
        fileOutput.outputSimLog(step, simulationWallTime, totalNumWwCommChannels);
        fileOutput.outputRoutes(allRoutes);
        fileOutput.outputBestTTData(allRoutes);
        allRoutes.clear();
        fileOutput.outputTrajectories(allTrajectories);
        allTrajectories.clear();
        fileOutput.close();
    }

    /**
     * Updates wall time spent on simulation.
     */
    synchronized public void updateSimulationTime() {
        simulationWallTime += (double) (System.nanoTime() - timeStamp) / 1000000000;
    }

    public void takeTimeStamp() {
        timeStamp = System.nanoTime();
    }

    public boolean[] updateVehicleCounts() {
        boolean isNewNonPubVehiclesAllowed = numInternalNonPubVehiclesAtAllWorkers < Settings.numGlobalRandomPrivateVehicles;
        boolean isNewTramsAllowed = numInternalTramsAtAllWorkers < Settings.numGlobalRandomTrams;
        boolean isNewBusesAllowed = numInternalBusesAtAllWorkers < Settings.numGlobalRandomBuses;

        // Clear vehicle counts from last step
        numInternalNonPubVehiclesAtAllWorkers = 0;
        numInternalTramsAtAllWorkers = 0;
        numInternalBusesAtAllWorkers = 0;
        boolean[] required = new boolean[3];
        required[0] = isNewNonPubVehiclesAllowed;
        required[1] = isNewTramsAllowed;
        required[2] = isNewBusesAllowed;
        return required;
    }

    public void setLightChangeNode(Node node) {
        node.light = !node.light;
        nodesToAddLight.remove(node);
        nodesToRemoveLight.remove(node);
        if (node.light) {
            nodesToAddLight.add(node);
        } else {
            nodesToRemoveLight.add(node);
        }
    }

    public void resetVariablesForSetup() {
        // Reset temporary variables
        simulationWallTime = 0;
        totalNumWwCommChannels = 0;
        numVehiclesCreatedDuringSetup = 0;
        numVehiclesNeededAtStart = 0;
        step = 0;
        allRoutes.clear();
        allTrajectories.clear();
    }

    public void reserVariablesAtStop() {
        numInternalNonPubVehiclesAtAllWorkers = 0;
        numInternalTramsAtAllWorkers = 0;
        numInternalBusesAtAllWorkers = 0;
        nodesToAddLight.clear();
        nodesToRemoveLight.clear();
    }

    public void updateNoOfVehiclesNeededAtStart(int vehiclesInRouteLoader) {
        // Get number of vehicles needed
        numVehiclesNeededAtStart = vehiclesInRouteLoader + Settings.numGlobalRandomPrivateVehicles
                + Settings.numGlobalRandomTrams + Settings.numGlobalRandomBuses;
    }

    public ArrayList<Node> getNodesToAddLight() {
        return nodesToAddLight;
    }

    public ArrayList<Node> getNodesToRemoveLight() {
        return nodesToRemoveLight;
    }

    public void updateChannelCount(int count) {
        totalNumWwCommChannels += count;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    /**
     * Get simulation setup from the imported setup list. The setup will be sent
     * to workers when server informs the workers to set up simulation. The
     * retrieved setup will be removed from the list.
     */
    public boolean setupForExperimentSimulation(Experiment experiment) {
        boolean isNewMap = false;

        Settings.isDriveOnLeft = experiment.isDriveOnLeft();
        Settings.maxNumSteps = experiment.getMaxNumSteps();
        Settings.numGlobalRandomPrivateVehicles = experiment.getNumRandomPrivateVehicles();
        Settings.numGlobalRandomTrams = experiment.getNumRandomTrams();
        Settings.numGlobalRandomBuses = experiment.getNumRandomBusses();
        if (experiment.getForegroundVehicleFile() != null && !experiment.getForegroundVehicleFile().equals("-")) {
            Settings.inputForegroundVehicleFile = experiment.getForegroundVehicleFile();
        } else {
            Settings.inputForegroundVehicleFile = "";
        }
        Settings.inputOnlyODPairsOfForegroundVehicleFile = experiment.isLoadOnlyODPairs();
        if (experiment.getBackgroundVehicleFile() != null && !experiment.getBackgroundVehicleFile().equals("-")) {
            Settings.inputBackgroundVehicleFile = experiment.getBackgroundVehicleFile();
        } else {
            Settings.inputBackgroundVehicleFile = "";
        }
        Settings.isOutputSimulationLog = experiment.isOutputSimulationLog();
//		Settings.isOutputForegroundTravelTime = experiment.isOutputForegroundTravelTime();
        Settings.isOutputTrajectory = experiment.isOutputTrajectory();
        Settings.isOutputInitialRoutes = experiment.isOutputInitialRoute();
        Settings.lookAheadDistance = experiment.getLookAheadDistance();
        Settings.numStepsPerSecond = experiment.getNumStepsPerSecond();
        Settings.isServerBased = experiment.isServerBased();
        if (experiment.getOsmMapFile() != null && !experiment.getOsmMapFile().equals("-")) {
            Settings.inputOpenStreetMapFile = experiment.getOsmMapFile();
            isNewMap = true;
        } else {
            Settings.inputOpenStreetMapFile = "";
        }
        Settings.trafficLightTiming = experiment.getTrafficLightTiming();
        Settings.routingAlgorithm = experiment.getRoutingAlgorithm();
        Settings.trafficReportStepGapInServerlessMode = experiment.getTrafficReportStepGapInServerlessMode();
        Settings.isAllowReroute = experiment.isAllowReroute();
        Settings.downloadDirectory = experiment.getDownLoadDirectory();
        Settings.testName = experiment.getTestName();
        Settings.odDistributor = experiment.getOdDistributor();
        Settings.temporalDistributor = experiment.getTemporalDistributor();
        Settings.simulationListener = experiment.getSimulationListener();
        Settings.vehicleTypeDistributor = experiment.getVehicleTypeDistributor();

        return isNewMap;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public int getExperimentIndex() {
        return experimentIndex;
    }

    public void startMultipleExperimentRunning() {
        scriptLoader.loadScriptFile();
        experiments = scriptLoader.getExperiments();
        experimentIndex = 0;
        runIndex = 1;
        runAutomatedSim();
    }

    public void updateExperimentIndices() {
        if(experiments != null) {
            if (experiments.size() == experimentIndex) {
                resetVariablesEndOfExperiment();
            }else {
                int runsNeed = experiments.get(experimentIndex).getNumRuns();
                if (runIndex == runsNeed) {
                    runIndex = 1;
                    experimentIndex++;
                } else {
                    runIndex++;
                }
            }
        }
    }

    public void resetVariablesEndOfExperiment(){
        Settings.testName = Settings.defaultTestName;
        Settings.downloadDirectory = Settings.defaultDownloadDirectory;
        experimentIndex = 0;
        runIndex = Settings.defaultRunIndex;
        experiments = null;
    }

    void startSimulationFromLoadedScript() {
        Experiment experiment = experiments.get(experimentIndex);
        boolean isNewMap = setupForExperimentSimulation(experiment);
        if (isNewMap) {
            changeMap();
        }
        consoleUI.startSimulationFromLoadedScript();
    }

    public void runAutomatedSim(){
        Experiment experiment = experiments.get(experimentIndex);
        boolean isNewMap = setupForExperimentSimulation(experiment);
        if (isNewMap) {
            gui.loadMapChange();
        }
        gui.updateGuiComps();
        System.out.println("Run next simulation");
        gui.runSimulationAuto();
    }

    public boolean loadScript() {
        return scriptLoader.loadScriptFile();
    }
}
