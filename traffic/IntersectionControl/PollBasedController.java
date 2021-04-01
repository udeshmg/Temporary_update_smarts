package traffic.IntersectionControl;

import traffic.road.Edge;
import traffic.road.Node;
import traffic.vehicle.IntersectionStatManager;
import traffic.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PollBasedController extends IntersectionController {


    ArrayList<ArrayList<IntersectionStatManager>> allQueues = new ArrayList<>();
    ArrayList<Double> previousScheduledTimes = new ArrayList<>();
    ArrayList<Boolean> isQueueVisited= new ArrayList<>();

    double serviceTime = 1.0;
    double switchTime = 1.0;
    double latestArrivedTime = 1000000.0;
    double timeToReachAtMaxSpeed = 21;

    public PollBasedController(Node node, double controlRegion){
        this.node = node;
        this.controlRegion = controlRegion;

        initTimes();
        initVisited();
    }

    public PollBasedController(){
        this.node = null;
        this.controlRegion = 380;

        initTimes();
        initVisited();
    }

    public void initTimes(){
        for (int i = 0; i < 4; i++){
            previousScheduledTimes.add(-(serviceTime+switchTime));
        }
    }

    public void initVisited(){
        isQueueVisited.clear();
        for (int i = 0; i < 4; i++){
            isQueueVisited.add(false);
        }
    }

    @Override
    public void computeSchedule(double timeNow) {

        if (!isTriggered) return;
        // Get arriving vehicles within control region and not yet finished servicing
        int queueIndex = 0;
        for (Edge edge : node.inwardEdges){
                ArrayList<IntersectionStatManager> laneQueue = new ArrayList<>();
                double lastExitedVehicleTimeSch = 0.0;
                for (Vehicle vehicle : edge.getFirstLane().getVehicles()){
                    if (!vehicle.episodeStat.isFinalizedSchedule(timeNow)){
                        if (controlRegion > (vehicle.lane.edge.length - vehicle.headPosition)){
                        laneQueue.add(vehicle.episodeStat);
                        }
                    }
                    else{
                        if (vehicle.episodeStat.getTimeRemain() > lastExitedVehicleTimeSch){
                            previousScheduledTimes.set(queueIndex, vehicle.episodeStat.getTimeRemain()-timeToReachAtMaxSpeed);
                        }
                    }
                }
                allQueues.add(laneQueue);
                queueIndex++;
        }
        pollBasedSchedule(timeNow);
        isTriggered = false;
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

        initVisited();

        double currentTimeAssigned = 0.0;
        while (!allVehiclesServiced()){
            while( !allQueues.get(indexOfQueue).isEmpty() &&
                    allQueues.get(indexOfQueue).get(0).getTimeArrived() <= timeNow + currentTimeAssigned ){

                IntersectionStatManager stat = allQueues.get(indexOfQueue).get(0);
                currentTimeAssigned = serviceTime + Math.max(currentTimeAssigned, getSafestAllowedTime(indexOfQueue));
                stat.setAssignedTime(currentTimeAssigned+timeNow);
                stat.assignIntersectionConstrains(timeNow, (currentTimeAssigned) + timeToReachAtMaxSpeed);
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

    private double getSafestAllowedTime(int index){
        int i = 0;
        double safestTime = 0;
        for (Double assignedTime : previousScheduledTimes){
            if (index != i){
                if (safestTime < assignedTime+switchTime) {
                    safestTime = assignedTime+switchTime;
                }
            else{
                if (safestTime < assignedTime+serviceTime) {
                        safestTime = assignedTime+serviceTime;
                }
                }
            }
            i++;
        }
        return safestTime;

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
                new IntersectionStatManager(10)
                //new IntersectionStatManager(3.5),
                //new IntersectionStatManager(7),
                //new IntersectionStatManager(9)
        );


        ArrayList<IntersectionStatManager> q1 = new ArrayList<>(l1);

        List<IntersectionStatManager> l2 = Arrays.asList(
                new IntersectionStatManager(22),
                new IntersectionStatManager(23.5)
                //new IntersectionStatManager(4),
                //new IntersectionStatManager(6),
                //new IntersectionStatManager(11)
        );

        ArrayList<IntersectionStatManager> q2 = new ArrayList<>(l2);
        ArrayList<ArrayList<IntersectionStatManager>> allQueues = new ArrayList<>();
        allQueues.add(q1);
        allQueues.add(q2);

        //pollBasedController.allQueues = allQueues;

        pollBasedController.getVehiclesInServiceFromQueue(allQueues, 0);
        pollBasedController.pollBasedSchedule(10);

        for (IntersectionStatManager stat : l1) {
            System.out.println("Assigned time: " + stat.getAssignedTime() + " , " + stat.getTimeToReach());
        }


        for (IntersectionStatManager stat : l2) {
            System.out.println("Assigned time: " + stat.getAssignedTime()  + " , " + stat.getTimeToReach());
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
