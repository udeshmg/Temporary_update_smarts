package traffic.IntersectionControl;

import common.Settings;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.vehicle.Vehicle;

import java.security.interfaces.EdECKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PollBasedController extends IntersectionController {

    private Random randomTimingGenerator = null;

    public PollBasedController(Node node, double controlRegion, Settings settings){
        this.node = node;
        this.controlRegion = controlRegion;
        this.randomTimingGenerator = settings.randomTimingGenerator;
    }

    @Override
    public void computeSchedule(double timeNow) {

        for (Edge edge : node.inwardEdges){
            List<Vehicle> vehicles = edge.getFirstLane().getVehicles();
            for (Vehicle vehicle : vehicles){
                assignTime(vehicle, timeNow);
            }
        }
    }

    public void assignTime(Vehicle vehicle, double timeNow){
        if (controlRegion > (vehicle.lane.edge.length - vehicle.headPosition) &&
                !vehicle.isIntersectionConstraints()) {
            int timeToReach = randomTimingGenerator.nextInt(15) + 30;
            vehicle.assignIntersectionConstrains(timeNow, timeToReach);
        }
    }

}
