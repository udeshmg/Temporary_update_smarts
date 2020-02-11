package traffic.network;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.vehicle.VehicleType;

import java.util.ArrayList;
import java.util.List;

public class RandomTrafficGenerator extends TrafficGenerator {
    public RandomTrafficGenerator(ODDistributor odDistributor, TemporalDistributor temporalDistributor, VehicleTypeDistributor vehicleTypeDistributor) {
        super(odDistributor, temporalDistributor, vehicleTypeDistributor);
    }

    @Override
    public ArrayList<ODDemand> getGeneratedTraffic(TrafficNetwork trafficNetwork, List<Edge> possibleStartEdges, List<Edge> possibleEndEdges, int timeStep) {
        ArrayList<ODDemand> ODMatrix = new ArrayList<>();
        for (int i = 0; i < getTemporalDistributor().getCurrentVehicleLimit(200, timeStep, trafficNetwork.getSettings().maxNumSteps); i++){
            ODDemand odDemand = new ODDemand();
            Node[] OD = getOdDistributor().getStartAndEndEdge(trafficNetwork, possibleStartEdges, possibleEndEdges, timeStep);

            odDemand.setOrigin(OD[0]);
            odDemand.setDestination(OD[1]);

            int numVehicles = 1; //getTemporalDistributor().getCurrentVehicleLimit(1, timeStep, 18000);
            odDemand.setNumVehicles(numVehicles);

            VehicleType vehicleType = getVehicleTypeDistributor().getVehicleType();

            odDemand.setVehicleType(vehicleType);

            ODMatrix.add(odDemand);

        }
        return ODMatrix;

    }
}
