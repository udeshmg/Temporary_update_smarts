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
public class SimulationData {
    private RoadNetwork roadNetwork;
    private GUI gui;
    private ConsoleUI consoleUI;


    public void showNumberOfConnectedWorkers(int number){
        if (Settings.isVisualize) {
            gui.updateNumConnectedWorkers(number);
        }else{
            System.out.println(number + "/" + Settings.numWorkers + " workers connected.");
        }
    }

    public void initRoadNetwork(){
        // Load default road network
        Settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile();
        roadNetwork = new RoadNetwork();
    }

    public void initUIs(SimulationProcessor processor){
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

    public void prepareForSetup(){
        if (Settings.isVisualize) {
            gui.getReadyToSetup();
        } else {
            consoleUI.acceptSimScriptFromConsole();// Let user input simulation script path
        }
    }

    public void prepareForSetupAgain(){
        if ((Settings.isVisualize)) {
            gui.getReadyToSetup();
        } else {
            consoleUI.readyToStartSim();
        }
    }

    public void updateUI(final ArrayList<Serializable_GUI_Vehicle> vehicleList,
                         final ArrayList<Serializable_GUI_Light> lightList, final String workerName, final int numWorkers,
                         final int step){
        // Update GUI
        if (Settings.isVisualize) {
            gui.updateObjectData(vehicleList, lightList, workerName, numWorkers, step);
        }
    }

    public void startSimulationInUI(){
        if (Settings.isVisualize) {
            gui.startSimulation();
        }
    }

    public void resetStepInUI(){
        if (Settings.isVisualize) {
            gui.stepToDraw = 0;
        }
    }

    public void updateSetupprogressInUI(int  numVehiclesCreatedDuringSetup, int numVehiclesNeededAtStart){
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

    public void changeMap(){
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
}
