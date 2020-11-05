package traffic.IntersectionControl;

import common.Settings;
import org.apache.commons.numbers.gamma.RegularizedGamma;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.vehicle.IntersectionStatManager;
import traffic.vehicle.Vehicle;

import java.security.interfaces.EdECKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class PollBasedController extends IntersectionController {


    ArrayList<ArrayList<IntersectionStatManager>> allQueues = new ArrayList<>();
    double serviceTime = 1.0;
    double switchTime = 1.0;
    double latestArrivedTime = 1000000.0;
    double timeToReachAtMaxSpeed = 380/22;

    public PollBasedController(Node node, double controlRegion){
        this.node = node;
        this.controlRegion = controlRegion;
    }

    public PollBasedController(){
        this.node = null;
        this.controlRegion = 380;
    }

    @Override
    public void computeSchedule(double timeNow) {

        // Get arriving vehicles within control region and not yet finished servicing
        for (Edge edge : node.inwardEdges){
            ArrayList<IntersectionStatManager> laneQueue = new ArrayList<>();
            for (Vehicle v : edge.getFirstLane().getVehicles()){
                if (!v.episodeStat.isFinalizedSchedule(timeNow) || !v.episodeStat.isIntersectionConstraints())
                    laneQueue.add(v.episodeStat);
            }
            allQueues.add(laneQueue);
        }
    }

    public void getVehiclesInServiceFromQueue(ArrayList<ArrayList<IntersectionStatManager>> data, double timeNow){
        for (ArrayList<IntersectionStatManager> queue : data){
            ArrayList<IntersectionStatManager> laneQueue = new ArrayList<>();
            for (IntersectionStatManager stat : queue){
                if (!stat.isFinalizedSchedule(timeNow) || !stat.isIntersectionConstraints())
                    laneQueue.add(stat);
            }
            allQueues.add(laneQueue);
        }
    }

    public void pollBasedSchedule(double timeNow){

        int indexOfQueue = findFirstArrivedVehicleInQueues();
        int numQueues = allQueues.size();

        double currentTimeAssigned = latestArrivedTime;
        while (!allVehiclesServiced()){
            while( !allQueues.get(indexOfQueue).isEmpty() &&
                    allQueues.get(indexOfQueue).get(0).getTimeArrived() <= currentTimeAssigned ){

                IntersectionStatManager stat = allQueues.get(indexOfQueue).get(0);
                currentTimeAssigned += serviceTime;
                stat.setAssignedTime(currentTimeAssigned);
                stat.assignIntersectionConstrains(timeNow, currentTimeAssigned + timeToReachAtMaxSpeed);
                allQueues.get(indexOfQueue).remove(0);
            }
            currentTimeAssigned += switchTime;

            indexOfQueue = findFirstArrivedVehicleInQueues();

            /*
            if (indexOfQueue < numQueues)
                indexOfQueue++;
            else
                indexOfQueue = 0;
                */
        }
        allQueues.clear();
    }

    private int findFirstArrivedVehicleInQueues(){
        int index = 0;
        int indexOfQueue = 0;
        latestArrivedTime = 10000000000000.0;

        //get first vehicle arrived
        for (ArrayList<IntersectionStatManager> stats : allQueues){
            if (!stats.isEmpty()){
                if (latestArrivedTime > stats.get(0).getTimeArrived()){
                    latestArrivedTime = stats.get(0).getTimeArrived();
                    indexOfQueue = index;
                }
            }
            index++;
        }
        return indexOfQueue;
    }

    private boolean allVehiclesServiced(){

        for (ArrayList<IntersectionStatManager> stats : allQueues){
            if (stats.size() != 0){
                return false;
            }
        }

        return true;
    }

    public static void main(final String[] args) {

        PollBasedController pollBasedController = new PollBasedController();

        List<IntersectionStatManager> l1 = Arrays.asList(
                new IntersectionStatManager(0),
                new IntersectionStatManager(1),
                new IntersectionStatManager(3.5),
                new IntersectionStatManager(7),
                new IntersectionStatManager(9)
        );


        ArrayList<IntersectionStatManager> q1 = new ArrayList<>(l1);

        List<IntersectionStatManager> l2 = Arrays.asList(
                new IntersectionStatManager(1),
                new IntersectionStatManager(2.5),
                new IntersectionStatManager(4),
                new IntersectionStatManager(6),
                new IntersectionStatManager(11)
        );

        ArrayList<IntersectionStatManager> q2 = new ArrayList<>(l2);
        ArrayList<ArrayList<IntersectionStatManager>> allQueues = new ArrayList<>();
        allQueues.add(q1);
        allQueues.add(q2);

        //pollBasedController.allQueues = allQueues;

        pollBasedController.getVehiclesInServiceFromQueue(allQueues, 0);
        pollBasedController.pollBasedSchedule(0);

        for (IntersectionStatManager stat : l1) {
            System.out.println("Assigned time: " + stat.getAssignedTime());
        }


        for (IntersectionStatManager stat : l2) {
            System.out.println("Assigned time: " + stat.getAssignedTime());
        }

        System.out.println("############");

        pollBasedController.getVehiclesInServiceFromQueue(allQueues, 2);
        pollBasedController.pollBasedSchedule(2);

        for (IntersectionStatManager stat : l1) {
            System.out.println("Assigned time: " + stat.getAssignedTime());
        }


        for (IntersectionStatManager stat : l2) {
            System.out.println("Assigned time: " + stat.getAssignedTime());
        }
        System.out.println("Done");


    }

}
