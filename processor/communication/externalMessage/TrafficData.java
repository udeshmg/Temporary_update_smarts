package processor.communication.externalMessage;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.vehicle.Vehicle;

import java.util.ArrayList;

public class TrafficData {

    ArrayList<RoadExternal> trafficData = null;
    ArrayList<VehiclePathExternal> paths = null;
    ArrayList<VehicleExternal> vehicles = null;

    public TrafficData(){
        trafficData = new ArrayList<>();
        paths = new ArrayList<>();
        vehicles = new ArrayList<>();

    }


    public void setTrafficData(TrafficNetwork trafficNetwork){
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

            for (Lane lane : edge.getLanes()){
                numVehicles += lane.getVehicles().size();
                for (Vehicle v : lane.getVehicles()){


                    vehicles.add(new VehicleExternal(v));


                    numVehicles++;
                    if (v.edgeBeforeTurnRight == edge){
                        numVehiclesRight++;
                    }
                    else if (v.edgeBeforeTurnLeft == edge){
                        numVehiclesLeft++;
                    }
                    else {
                        numVehiclesStraight++;
                    }

                    if (v.speed < 5){
                        numVehiclesStopped++;
                    }
                    else{
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

            //traffic details
            trafficData.add(roadExt);
        }

        paths = trafficNetwork.getVehiclePaths();

    }
}
