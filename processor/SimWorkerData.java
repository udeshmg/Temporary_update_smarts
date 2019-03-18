package processor;

import common.Settings;
import processor.communication.message.SerializableExternalVehicle;
import processor.communication.message.SerializableInt;
import processor.worker.Fellow;
import processor.worker.Simulation;
import processor.worker.Worker;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.GridCell;
import traffic.road.Node;
import traffic.road.RoadUtil;
import traffic.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class SimWorkerData {

    private TrafficNetwork trafficNetwork;
	private Simulation simulation;
    private int numLocalRandomPrivateVehicles = 0;
    private int numLocalRandomTrams = 0;
    private int numLocalRandomBuses = 0;
	private List<Edge> pspBorderEdges = new ArrayList<>();// For PSP (server-less)
	private List<Edge> pspNonBorderEdges = new ArrayList<>();// For PSP (server-less)
    private int step = 0;
    private double timeNow;

	public void initSimulation(Map<String, List<Edge>> pspEdges){
        simulation = new Simulation(trafficNetwork);
        pspBorderEdges = pspEdges.get("Border");
        pspNonBorderEdges = pspEdges.get("NonBorder");
    }

    public void initTrafficNetwork(String roadGraph){
        if (roadGraph.equals("builtin")) {
            Settings.roadGraph = RoadUtil.importBuiltinRoadGraphFile();
        } else {
            Settings.roadGraph = roadGraph;
        }
        trafficNetwork = new TrafficNetwork();
    }

    public void simulateOneStep(Worker worker, boolean isNewNonPubVehiclesAllowed,
                                boolean isNewTramsAllowed, boolean isNewBusesAllowed){
	    simulation.simulateOneStep(worker, timeNow, step, pspBorderEdges, pspNonBorderEdges, numLocalRandomPrivateVehicles,
                numLocalRandomTrams, numLocalRandomBuses, isNewNonPubVehiclesAllowed, isNewTramsAllowed, isNewBusesAllowed);
    }

    public void resetVariables(int numLocalRandomPrivateVehicles, int numLocalRandomTrams, int numLocalRandomBuses){
        this.numLocalRandomPrivateVehicles = numLocalRandomPrivateVehicles;
        this.numLocalRandomTrams = numLocalRandomTrams;
        this.numLocalRandomBuses = numLocalRandomBuses;
    }

    public TrafficNetwork getTrafficNetwork() {
        return trafficNetwork;
    }

    public void createVehicles(ArrayList<SerializableExternalVehicle> externalRoutes){
        trafficNetwork.createExternalVehicles(externalRoutes, timeNow);
        trafficNetwork.createInternalVehicles(numLocalRandomPrivateVehicles, numLocalRandomTrams,
                numLocalRandomBuses, true, true, true, timeNow);
    }

    public void changeLaneBlock(int laneIndex, boolean isBlocked) {
        trafficNetwork.lanes.get(laneIndex).isBlocked = isBlocked;
    }

    public void initTrafficLightCoordinator(ArrayList<SerializableInt> indexNodesToAddLight, ArrayList<SerializableInt> indexNodesToRemoveLight, ArrayList<Node> lightNodes){
        if ((indexNodesToAddLight.size() > 0) || (indexNodesToRemoveLight.size() > 0)) {
            trafficNetwork.lightCoordinator.init(lightNodes, indexNodesToAddLight, indexNodesToRemoveLight);
        }
    }

    public void resetLights(ArrayList<Node> lightNodes){
        // Reset lights
        trafficNetwork.lightCoordinator.init(lightNodes, new ArrayList<SerializableInt>(), new ArrayList<SerializableInt>());
    }

    public void addTransferredVehicle(Vehicle vehicle){
        trafficNetwork.addOneTransferredVehicle(vehicle, timeNow);
    }

    public void resetExistingNetwork(){
        // Reset existing network
        for (final Edge edge : trafficNetwork.edges) {
            edge.currentSpeed = edge.freeFlowSpeed;
        }
    }

    public void resetTraffic(){
        for (final Edge edge : pspBorderEdges) {
            edge.clearVehicles();
        }
        for (final Edge edge : pspNonBorderEdges) {
            edge.clearVehicles();
        }
        trafficNetwork.resetTraffic();
    }

    public void updateTrafficAtOutgoingEdgesToFellows(int laneIndex, double position, double speed){
        trafficNetwork.lanes.get(laneIndex).endPositionOfLatestVehicleLeftThisWorker = position;
        trafficNetwork.lanes.get(laneIndex).speedOfLatestVehicleLeftThisWorker = speed;
    }

    public void clearReportedTrafficData(){
	    trafficNetwork.clearReportedData();
    }

    public void buildEnvironment(ArrayList<GridCell> workCells, String workerName){
        trafficNetwork.buildEnvironment(workCells, workerName, step);
    }

    public int getStep() {
        return step;
    }

    public double getTimeNow() {
        return timeNow;
    }

    public void setStep(int step) {
        this.step = step;
        this.timeNow = step / Settings.numStepsPerSecond;
    }
}
