package traffic.vehicle;

import traffic.road.Edge;

public class IntersectionStatManager {

    private boolean isEpisodeDone = false;
    private boolean isSuccess = false;
    private double timeToReach = 30; // in seconds
    private double timeRemain = 0;
    private boolean isIntersectionConstraints = false;
    private Edge targetEdge = null;
    private boolean crashed = false;

    private double gap = 0;
    private double frontVehicleSpeed = 0.0;
    private double frontVehicleTimeRemain = 0.0;
    private double frontVehicleDistance = 0.0;
    private boolean inExternalControl = false;

    private boolean finalizedSchedule = false;

    private double timeArrived = 0.0;
    private double assignedTime = -1;
    private boolean controllerNotified = false;

    private boolean isVirtual = false;
    private int id = -1;

    private int stepCounter = 0;

    public IntersectionStatManager(double timeArrived){
        this.timeArrived = timeArrived;
    }

    public IntersectionStatManager(){
        stepCounter = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void findEpisodeFinished(double timeNow, Vehicle vehicle){
        Vehicle frontVehicle =  vehicle.lane.getClosestFrontVehicleInLane(vehicle, 0);

        if (isEpisodeDone) return;


        if (frontVehicle != null){
            isVirtual = false;

            frontVehicleTimeRemain = frontVehicle.episodeStat.getTimeRemain();
            frontVehicleDistance = frontVehicle.lane.edge.length - frontVehicle.headPosition;

            double headway = (frontVehicle.headPosition - vehicle.headPosition)/Math.max(vehicle.speed, 0.1);
            //if (headway < 0.5){ //2 meters
            if (frontVehicle.headPosition < vehicle.headPosition + 4.5){ //2 meters
                //crashed =  true;
                //isEpisodeDone = true;
            }
            else {
                crashed = false;
            }
            setGap(frontVehicle.headPosition - vehicle.headPosition);
            setFrontVehicleSpeed(frontVehicle.speed);
        }
        else {
            isVirtual = true;
            frontVehicleTimeRemain = 0;
            frontVehicleDistance = 0;
            setGap(vehicle.lane.edge.length + 100 - vehicle.headPosition);
            setFrontVehicleSpeed(vehicle.lane.edge.freeFlowSpeed);
        }


        if ( timeNow >= timeToReach || Math.abs(vehicle.lane.edge.length - vehicle.headPosition) < 4.5) {
            isEpisodeDone = true;
            if ( timeNow >= (timeToReach-2) && Math.abs(vehicle.lane.edge.length - vehicle.headPosition) < 4.5){
                isSuccess = true;
            }
            else {
                isSuccess = false;
            }
        }

        timeRemain = timeToReach - timeNow;


    }

    public void findCruiseEpisodeFinished(double timeNow, Vehicle vehicle){
        Vehicle frontVehicle =  vehicle.lane.getClosestFrontVehicleInLane(vehicle, 0);
        stepCounter++;
        if (isEpisodeDone) return;

        if (frontVehicle != null){
            double headway = (frontVehicle.headPosition - vehicle.headPosition)/Math.max(vehicle.speed, 0.1);

            //if (headway < 0.5){ //2 meters
            if (frontVehicle.headPosition < vehicle.headPosition + 4.5){ //2 meters
                crashed =  true;
                isEpisodeDone = true;
            }
            setGap(frontVehicle.headPosition - vehicle.headPosition);
            setFrontVehicleSpeed(frontVehicle.speed);
        }
        else {
            setGap(vehicle.lane.edge.length + 100 - vehicle.headPosition);
            setFrontVehicleSpeed(vehicle.lane.edge.freeFlowSpeed);
        }


        if (Math.abs(vehicle.lane.edge.length - vehicle.headPosition) < 4.5) {
            isEpisodeDone = true;
            isSuccess = true;
        }

        if (stepCounter > 1000 ){
            isEpisodeDone = true;
            isSuccess = false;
        }

        timeRemain = timeToReach - timeNow;
    }

    public void assignIntersectionConstrains(double timeNow, double timeToReach){
        isIntersectionConstraints = true;
        this.setTimeToReach(timeNow+timeToReach);
        this.setTimeRemain(timeToReach);
    }

    public void notifyController(Vehicle vehicle, double timeNow){
        if (vehicle.lane.edge.endNode.intersectionControllerInUse && !controllerNotified ){
            if (vehicle.lane.edge.endNode.intersectionController.getControlRegion() > (vehicle.lane.edge.length - vehicle.headPosition)) {
                vehicle.lane.edge.endNode.intersectionController.notifyController();
                controllerNotified = true;
                vehicle.episodeStat.setTimeArrived(timeNow);
            }
        }
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public void setVirtual(boolean virtual) {
        isVirtual = virtual;
    }

    public void resetNotifyController(){
        controllerNotified = false;
    }



    public boolean isEpisodeDone() {
        return isEpisodeDone;
    }

    public void setEpisodeDone(boolean episodeDone) {
        isEpisodeDone = episodeDone;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public double getTimeToReach() {
        return timeToReach;
    }

    public void setTimeToReach(double timeToReach) {
        this.timeToReach = timeToReach;
    }

    public double getTimeRemain() {
        return timeRemain;
    }

    public void setTimeRemain(double timeRemain) {
        this.timeRemain = timeRemain;
    }

    public boolean isIntersectionConstraints() {
        return isIntersectionConstraints;
    }

    public void setIntersectionConstraints(boolean intersectionConstraints) {
        isIntersectionConstraints = intersectionConstraints;
    }

    public Edge getTargetEdge() {
        return targetEdge;
    }

    public void setTargetEdge(Edge targetEdge) {
        this.targetEdge = targetEdge;
    }

    public boolean isCrashed() {
        return crashed;
    }

    public void setCrashed(boolean crashed) {
        this.crashed = crashed;
    }

    public double getGap() {
        return gap;
    }

    public void setGap(double gap) {
        this.gap = gap;
    }

    public double getFrontVehicleSpeed() {
        return frontVehicleSpeed;
    }

    public void setFrontVehicleSpeed(double frontVehicleSpeed) {
        this.frontVehicleSpeed = frontVehicleSpeed;
    }

    public boolean isInExternalControl() {
        return inExternalControl;
    }

    public void setInExternalControl(boolean inExternalControl) {
        this.inExternalControl = inExternalControl;
    }


    public boolean isFinalizedSchedule(double timeNow) {
        if (assignedTime < 0)
            return false;

        if (assignedTime <= timeNow)
            return true;
        return false;
    }

    public void setFinalizedSchedule(boolean finalizedSchedule) {
        this.finalizedSchedule = finalizedSchedule;
    }


    public double getTimeArrived() {
        return timeArrived;
    }

    public void setTimeArrived(double timeArrived) {
        this.timeArrived = timeArrived;
    }

    public double getAssignedTime() {
        return assignedTime;
    }

    public void setAssignedTime(double assignedTime) {
        this.assignedTime = assignedTime;
    }

    public double getFrontVehicleTimeRemain() {
        return frontVehicleTimeRemain;
    }

    public void setFrontVehicleTimeRemain(double frontVehicleTimeRemain) {
        this.frontVehicleTimeRemain = frontVehicleTimeRemain;
    }

    public double getFrontVehicleDistance() {
        return frontVehicleDistance;
    }

    public void setFrontVehicleDistance(double frontVehicleDistance) {
        this.frontVehicleDistance = frontVehicleDistance;
    }
}
