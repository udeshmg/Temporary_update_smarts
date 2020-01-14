package processor.communication.externalMessage;

import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.RoadNetwork;

import java.util.ArrayList;

public class TrafficData {

    ArrayList<RoadExternal> trafficData = null;

    public TrafficData(){
        trafficData = new ArrayList<>();
    }

    public void setTrafficData(RoadNetwork roadNetwork){
        for (Edge edge : roadNetwork.edges) {
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
    }
}
