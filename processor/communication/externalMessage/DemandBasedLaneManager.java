package processor.communication.externalMessage;

import processor.communication.message.Message_WS_TrafficReport;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.RoadNetwork;
import traffic.routing.RouteLeg;
import traffic.vehicle.Vehicle;

import java.util.ArrayList;

public class DemandBasedLaneManager implements ExternalSimulationListener {

    private RoadGraphExternal roadGraph = null;
    private RoadIndex rdIndex = new RoadIndex();


    private static class LaneManagerHelper{
        private static final DemandBasedLaneManager instance = new DemandBasedLaneManager();
    }

    public static ExternalSimulationListener getInstance(){
        return LaneManagerHelper.instance;
    }

    @Override
    public void init() {

    }

    @Override
    public void getRoadGraph(RoadNetwork roadNetwork) {

    }

    @Override
    public void getMessage(Message_WS_TrafficReport trafficReport) {

    }

    @Override
    public void getTrafficData(TrafficNetwork trafficNetwork) {
        for (Edge edge : trafficNetwork.edges){
            edge.projectVehicles = 0;
        }

        for (Vehicle v : trafficNetwork.newVehiclesforDemandEstimation){
            if (v.getNextRouteLegs().size() > 1) {
                for (RouteLeg routeLeg : v.getNextRouteLegs()){
                    routeLeg.edge.projectVehicles++;
                }
            }
        }
        trafficNetwork.newVehiclesforDemandEstimation.clear();

        for (Edge edge : trafficNetwork.edges){
            Edge oppositeEdge = edge.getOppositeEdge();

            if ((oppositeEdge.projectVehicles/oppositeEdge.getLaneCount()) > 0.5 * (edge.projectVehicles/edge.getLaneCount())){
                rdIndex.edges.add(edge.index);
            }
            else if (( edge.projectVehicles/edge.getLaneCount()) > 0.5 * (oppositeEdge.projectVehicles/oppositeEdge.getLaneCount())){
                rdIndex.edges.add(oppositeEdge.index);
            }
        }

    }

    @Override
    public RoadIndex getRoadDirChange() {
        RoadIndex extSend  = this.rdIndex;
        this.rdIndex = new RoadIndex();
        return extSend;
    }

    @Override
    public ArrayList<Integer> laneChangeMessage() {
        return null;
    }
}
