package processor.communication.externalMessage;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import processor.communication.message.Message_WS_TrafficReport;
import traffic.road.RoadNetwork;
import processor.communication.externalMessage.RoadGraphExternal;
import com.google.gson.Gson;

import java.util.ArrayList;

public class LaneManager implements ExternalSimulationListener {

    private LaneManager() {
        init();
    }

    private static class LaneManagerHelper{
        private static final LaneManager instance = new LaneManager();
    }

    public static LaneManager getInstance(){
        return LaneManagerHelper.instance;
    }

    private ZContext context = null;
    private ZMQ.Socket socket = null;
    private RoadGraphExternal roadGraph = null;


    @Override
    public void init() {
        context = new ZContext();
        socket = context.createSocket(SocketType.REQ);
        socket.connect("tcp://*:5556");
        System.out.println("Created the client connection from SMARTS");
    }

    @Override
    public void getRoadGraph(RoadNetwork roadNetwork) {
        roadGraph = new RoadGraphExternal();
        roadGraph.buildRoadGraph(roadNetwork);
        // process and send
        Gson gson = new Gson();
        RoadExternal rd = new RoadExternal();
        String str = gson.toJson(roadGraph);
        System.out.println("GSON output: " + str);

        sendMessage(str);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void getMessage(Message_WS_TrafficReport trafficReport) {
        // process and send
    }

    @Override
    public void getTrafficData(RoadNetwork roadNetwork) {
        TrafficData trafficData = new TrafficData();
        trafficData.setTrafficData(roadNetwork);
        Gson gson = new Gson();
        String str = gson.toJson(trafficData);
        System.out.println("GSON output: " + str);

        sendMessage(str);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ArrayList<Integer> laneChangeMessage() {
        return null;
    }

    private void sendMessage(String str){
        socket.send(str.getBytes(ZMQ.CHARSET), 0);
        byte[] reply = socket.recv();
        System.out.println(
                "Received: [" + new String(reply, ZMQ.CHARSET) + "]"
        );
    }

    public void readBuffer(){

    }
}
