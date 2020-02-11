package traffic.network;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadNetwork;

import java.util.List;

public class PredefinedODLoader extends ODDistributor {

    private int counter = 0;
    private int iterator = 0;
    private int trafficType = 0;

    public PredefinedODLoader(int maxCounter) {
        this.maxCounter = maxCounter;
    }

    private int maxCounter = 2;
    private int [][][] odMatrix = {{{13,1}, {1,13}, {8,11}, {11,8}, {7,4}, {4,7}, {14,2}, {2,14}},
                                   {{12,2}, {2,12}, {13,7}, {7,13}, {14,2}, {2,14}, {8,3}, {3,8}}};

    @Override
    public Node[] getStartAndEndEdge(TrafficNetwork trafficNetwork, List<Edge> possibleStartEdges, List<Edge> possibleEndEdges, int timeNow) {
        Node[] odpair = {trafficNetwork.nodes.get(odMatrix[trafficType][counter][0]), trafficNetwork.nodes.get(odMatrix[trafficType][counter][1])};
        iterate();
        if (timeNow > 1000) {
            trafficType = 1;
        }
        else { trafficType = 1;}
        return odpair;
    }

    private void iterate(){
        counter ++;
        iterator ++;
        if (counter == maxCounter){ counter = 0;}

    }
}
