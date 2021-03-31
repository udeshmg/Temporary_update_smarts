package traffic.IntersectionControl;

import common.Settings;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.vehicle.Vehicle;

import java.security.interfaces.EdECKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RandomController extends IntersectionController {

    private Random randomTimingGenerator = null;
    private int time = 22;
    private int increment = 0;

    public RandomController(Node node, double controlRegion, Settings settings){
        this.node = node;
        this.controlRegion = controlRegion;
        this.randomTimingGenerator = settings.randomTimingGenerator;
        time = 20;
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
                !vehicle.episodeStat.isIntersectionConstraints() && !vehicle.episodeStat.isEpisodeDone()) {

            Vehicle frontVehicle =  vehicle.lane.getClosestFrontVehicleInLane(vehicle, 0);

            int lowerBound = 20;
            //if (frontVehicle != null){
            //     lowerBound = Math.max(lowerBound, (int)frontVehicle.episodeStat.getTimeRemain());
            //}

            int timeToReach = randomTimingGenerator.nextInt(25) + lowerBound;
            //int timeToReach = 1 + lowerBound;
            increment++;
            vehicle.episodeStat.assignIntersectionConstrains(timeNow,timeToReach);
            //System.out.println("Assigned time: " + vehicle.vid + " " + time);
            time++;
        }
    }

}
