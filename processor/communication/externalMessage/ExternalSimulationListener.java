package processor.communication.externalMessage;

import processor.communication.message.Message_WS_TrafficReport;
import traffic.TrafficNetwork;
import traffic.road.RoadNetwork;

import java.util.ArrayList;

public interface ExternalSimulationListener {

    void init();
    void getRoadGraph(RoadNetwork roadNetwork);
    void getMessage(Message_WS_TrafficReport trafficReport);
    void getTrafficData(TrafficNetwork trafficNetwork);

    static ExternalSimulationListener getInstance(){return null;};
    RoadIndex getRoadDirChange();
    ArrayList<Integer> laneChangeMessage();
}
