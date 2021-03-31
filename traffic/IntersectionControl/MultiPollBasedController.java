package traffic.IntersectionControl;

import traffic.road.Edge;
import traffic.road.Lane;
import traffic.road.Node;
import traffic.vehicle.IntersectionStatManager;
import traffic.vehicle.Vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiPollBasedController extends IntersectionController {


    ArrayList<ArrayList<IntersectionStatManager>> allQueues = new ArrayList<>();
    ArrayList<Double> currentTimes = new ArrayList<>();
    double latestArrivedTime = 1000000.0;
    double timeToReachAtMaxSpeed = 22;

    Double switchOverTime = 2.0;
    Double serviceTime = 1.0;
    Double intermediateTime = 1.0;

    ArrayList<List<Double>> map = new ArrayList<>();

    public MultiPollBasedController(Node node, double controlRegion){
        this.node = node;
        this.controlRegion = controlRegion;
        setQueueMapping();
    }

    public MultiPollBasedController(){
        this.node = null;
        this.controlRegion = 380;
    }

    @Override
    public void computeSchedule(double timeNow) {

        if (!isTriggered) return;
        // Get arriving vehicles within control region and not yet finished servicing
        for (Edge edge : node.inwardEdges){
            if (edge.index == 12 || edge.index == 13) {
                for (Lane lane : edge.getLanes()) {
                    ArrayList<IntersectionStatManager> laneQueue = new ArrayList<>();
                    for (Vehicle vehicle : lane.getVehicles()) {
                        if (!vehicle.episodeStat.isFinalizedSchedule(timeNow)) {
                            if (controlRegion > (vehicle.lane.edge.length - vehicle.headPosition)) {
                                laneQueue.add(vehicle.episodeStat);
                            }
                        }
                    }
                    allQueues.add(laneQueue);
                }
            }
        }
        setQueueMapping();
        pollBasedSchedule(timeNow);
        isTriggered = false;
    }

    public void setQueueMapping(){
        ArrayList<List<Double>> map = new ArrayList<>();

        map.add(Arrays.asList(1.0,-1.0,3.0,1.0));
        map.add(Arrays.asList(-1.0,1.0,4.0,3.0));
        map.add(Arrays.asList(3.0,1.0,1.0,-1.0));
        map.add(Arrays.asList(4.0,3.0,-1.0,1.0));
        this.map = map;

        resetCurrentTimes();
    }


    public void setQueueMapping(ArrayList<List<Double>> map){
        this.map = map;
        resetCurrentTimes();
    }

    private void resetCurrentTimes(){
        currentTimes.clear();
        for (int i = 0; i < map.size(); i++){
            currentTimes.add(-1.0);
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
        resetCurrentTimes();

        double currentTimeAssigned = latestArrivedTime;
        while (!allVehiclesServiced()){
            while( !allQueues.get(indexOfQueue).isEmpty() &&
                    allQueues.get(indexOfQueue).get(0).getTimeArrived() <= currentTimeAssigned + serviceTime){


                IntersectionStatManager stat = allQueues.get(indexOfQueue).get(0);
                currentTimeAssigned = Math.max(stat.getTimeArrived()+serviceTime, getSafestAllowedTime(indexOfQueue));
                stat.setAssignedTime(currentTimeAssigned);
                stat.assignIntersectionConstrains(timeNow, (currentTimeAssigned - latestArrivedTime) + timeToReachAtMaxSpeed);
                allQueues.get(indexOfQueue).remove(0);

                // Store last vehicle's time
                currentTimes.set(indexOfQueue, currentTimeAssigned);
                //currentTimeAssigned += serviceTime;

            }
            //currentTimeAssigned += switchTime;

            indexOfQueue = findFirstArrivedVehicleInQueues();
            currentTimeAssigned = latestArrivedTime;
        }
        allQueues.clear();
    }

    private double getSafestAllowedTime(int index){
        int i = 0;
        double safestTime = 0;
        for (Double assignedTime : map.get(index)){
            if (currentTimes.get(i) >= 0 && assignedTime > 0 && safestTime < currentTimes.get(i) + assignedTime){ //assigned time -1 indicates no vehicles
                safestTime = currentTimes.get(i) + assignedTime;
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

        MultiPollBasedController pollBasedController = new MultiPollBasedController();


        ArrayList<List<Double>> map = new ArrayList<>();

        Double switchOverTime = 2.0;
        Double serviceTime = 1.0;
        Double intermediateTime = 1.0;

        map.add(Arrays.asList(1.0,-1.0,2.0,1.5));
        map.add(Arrays.asList(-1.0,1.0,4.0,3.5));
        map.add(Arrays.asList(2.0,1.5,1.0,-1.0));
        map.add(Arrays.asList(4.0,3.5,-1.0,1.0));

        List<IntersectionStatManager> l1 = Arrays.asList(
                new IntersectionStatManager(0),
                new IntersectionStatManager(1),
                new IntersectionStatManager(3.5)
                //new IntersectionStatManager(7),
                //new IntersectionStatManager(9)
        );


        ArrayList<IntersectionStatManager> q1 = new ArrayList<>(l1);

        List<IntersectionStatManager> l2 = Arrays.asList(
                new IntersectionStatManager(1),
                new IntersectionStatManager(2)
                //new IntersectionStatManager(4),
                //new IntersectionStatManager(6),
                //new IntersectionStatManager(11)
        );

        ArrayList<IntersectionStatManager> q2 = new ArrayList<>(l2);

        List<IntersectionStatManager> l3 = Arrays.asList(
                new IntersectionStatManager(2),
                new IntersectionStatManager(3.5)
                //new IntersectionStatManager(4),
                //new IntersectionStatManager(6),
                //new IntersectionStatManager(11)
        );

        ArrayList<IntersectionStatManager> q3 = new ArrayList<>(l3);

        List<IntersectionStatManager> l4 = Arrays.asList(
                new IntersectionStatManager(1.5),
                new IntersectionStatManager(2.5)
                //new IntersectionStatManager(4),
                //new IntersectionStatManager(6),
                //new IntersectionStatManager(11)
        );

        ArrayList<IntersectionStatManager> q4 = new ArrayList<>(l4);

        ArrayList<ArrayList<IntersectionStatManager>> allQueues = new ArrayList<>();
        allQueues.add(q1);
        allQueues.add(q2);
        allQueues.add(q3);
        allQueues.add(q4);
        //pollBasedController.allQueues = allQueues;

        pollBasedController.getVehiclesInServiceFromQueue(allQueues, 0);
        pollBasedController.setQueueMapping(map);
        pollBasedController.pollBasedSchedule(0);

        for (IntersectionStatManager stat : l1) {
            System.out.println("Assigned time: " + stat.getAssignedTime() + " , " + stat.getTimeToReach());
        }

        for (IntersectionStatManager stat : l2) {
            System.out.println("Assigned time: " + stat.getAssignedTime() + " , " + stat.getTimeToReach());
        }

        for (IntersectionStatManager stat : l3) {
            System.out.println("Assigned time: " + stat.getAssignedTime() + " , " + stat.getTimeToReach());
        }

        for (IntersectionStatManager stat : l4) {
            System.out.println("Assigned time: " + stat.getAssignedTime() + " , " + stat.getTimeToReach());
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
