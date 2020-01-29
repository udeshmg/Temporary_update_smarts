package processor.communication.externalMessage;

import processor.communication.message.Message_WS_TrafficReport;
import traffic.road.RoadNetwork;

import java.util.ArrayList;

public interface ExternalSimulationListener {

    void init();
    void getRoadGraph(RoadNetwork roadNetwork);
    void getMessage(Message_WS_TrafficReport trafficReport);
    void getTrafficData(RoadNetwork roadNetwork);
    RoadIndex getRoadDirChange();
    ArrayList<Integer> laneChangeMessage();
}
