package processor.communication.externalMessage;

import traffic.TrafficNetwork;
import traffic.light.LightColor;
import traffic.light.Movement;
import traffic.light.TrafficLightCluster;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.routing.RouteLeg;
import traffic.vehicle.Vehicle;

import java.util.ArrayList;

public class TrafficData {

    ArrayList<RoadExternal> trafficData = null;
    ArrayList<VehiclePathExternal> paths = null;

    public TrafficData(){
        trafficData = new ArrayList<>();
        paths = new ArrayList<>();

    }


    public void setTrafficData(TrafficNetwork trafficNetwork, double timeNow) {
        for (Edge edge : trafficNetwork.edges) {
            RoadExternal roadExt = new RoadExternal();
            roadExt.setIndex(edge.index);
            roadExt.setNumLanes(edge.getLaneCount());
            roadExt.setStartNode(edge.startNode.index);
            roadExt.setEndNode(edge.endNode.index);

            int numVehicles = 0;
            int numVehiclesLeft = 0;
            int numVehiclesRight = 0;
            int numVehiclesStraight = 0;

            int numVehiclesStopped = 0;
            int numVehiclesOnMove = 0;

            for (Lane lane : edge.getLanes()) {
                numVehicles += lane.getVehicles().size();
                for (Vehicle v : lane.getVehicles()) {
                    //numVehicles++;
                    if (v.edgeBeforeTurnRight == edge) {
                        numVehiclesRight++;
                    } else if (v.edgeBeforeTurnLeft == edge) {
                        numVehiclesLeft++;
                    } else {
                        numVehiclesStraight++;
                    }

                    //if (v.headPosition > v.lane.edge.length*v.lane.edge.headPositionOfVSL){
                    if (v.speed < 0.1) {
                        numVehiclesStopped++;
                    } else {
                        numVehiclesOnMove++;
                    }
                }

            }

            roadExt.updateMvgData(edge.getNumVehicles(), edge.getNumVehiclesStraight(),
                    edge.getNumVehiclesRight(), edge.getNumVehiclesLeft());

            roadExt.setNumVehicles(numVehicles);
            roadExt.setNumVehiclesLeft(numVehiclesLeft);
            roadExt.setNumVehiclesRight(numVehiclesRight);
            roadExt.setNumVehiclesStraight(numVehiclesStraight);

            roadExt.setNumVehiclesStopped(numVehiclesStopped);
            roadExt.setNumVehiclesOnMove(numVehiclesOnMove);

            roadExt.setSignalColor(edge.getTrafficLightColor());
            roadExt.setExitFlow(edge.getNumVehiclesLeftThisRoad());
            roadExt.setTimeToGreen(mapTrafficLightsToEdges(trafficNetwork, edge, timeNow, LightColor.GYR_G));
            roadExt.setTimeToRed(mapTrafficLightsToEdges(trafficNetwork, edge, timeNow, LightColor.GYR_R));
            //traffic details
            trafficData.add(roadExt);
        }

        setVehicleData(trafficNetwork);

    }

    public double mapTrafficLightsToEdges(TrafficNetwork trafficNetwork, Edge edge, double timeNow, LightColor color){

        Node node = edge.endNode;
        TrafficLightCluster trafficLightCluster = trafficNetwork.lightCoordinator.clusterMap.get(node);

        for (Movement movement : trafficLightCluster.getMovements()){
            if ( movement.getControlEdge() == edge){
                return trafficLightCluster.getLightSchedule().timeForTrafficSignal(movement, timeNow, color);
            }
        }
        return 300.0;

    }

    public void setVehicleData(TrafficNetwork trafficNetwork){
        for (Vehicle v : trafficNetwork.vehicles){
            if (v.getNextRouteLegs().size() > 1) {
                ArrayList<Integer> pathInInt = new ArrayList<>();
                for (RouteLeg routeLeg : v.getNextRouteLegs()) {
                    pathInInt.add(routeLeg.edge.startNode.index);
                }
                if (v.getNextRouteLegs().size() > 1) {
                    Edge lastEdge = v.getNextRouteLegs().get(v.getNextRouteLegs().size() - 1).edge;
                    pathInInt.add(lastEdge.endNode.index);
                }


                paths.add(new VehiclePathExternal(pathInInt, 1));
            }
        }
    }
}
