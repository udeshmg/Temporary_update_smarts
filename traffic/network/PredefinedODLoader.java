package traffic.network;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.RoadNetwork;

import java.util.List;

public class PredefinedODLoader extends ODDistributor {

    private int counter = 0;
    private int maxCounter = 3;
    private int [][] odMatrix = {{38,0}, {39,2}, {24,9}};

    @Override
    public Edge[] getStartAndEndEdge(TrafficNetwork trafficNetwork, List<Edge> possibleStartEdges, List<Edge> possibleEndEdges) {
        Edge[] odpair = {trafficNetwork.edges.get(odMatrix[counter][0]), trafficNetwork.edges.get(odMatrix[counter][1])};
        iterate();
        return odpair;
    }

    private void iterate(){
        counter ++;
        if (counter == maxCounter){ counter = 0;}
    }
}
