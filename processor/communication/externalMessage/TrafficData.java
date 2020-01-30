package processor.communication.externalMessage;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.RoadNetwork;
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

    public void setTrafficData(TrafficNetwork trafficNetwork){
        for (Edge edge : trafficNetwork.edges) {
            RoadExternal roadExt = new RoadExternal();
            roadExt.setIndex(edge.index);
            roadExt.setNumLanes(edge.getLaneCount());
            roadExt.setStartNode(edge.startNode.index);
            roadExt.setEndNode(edge.endNode.index);

            int numVehicles = 0;
            for (Lane lane : edge.getLanes()){
                numVehicles += lane.getVehicles().size();

            }
            roadExt.setNumVehicles(numVehicles);
            //traffic details
            trafficData.add(roadExt);
        }

        for (Vehicle v : trafficNetwork.vehicles){

            ArrayList<Integer> pathInInt = new ArrayList<>();
            for (RouteLeg routeLeg : v.getNextRouteLegs()){
                pathInInt.add(routeLeg.edge.index);
            }

            paths.add(new VehiclePathExternal(pathInInt, 1));
        }
    }
}
