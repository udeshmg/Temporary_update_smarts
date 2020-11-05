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
    private boolean inExternalControl = false;

    private boolean finalizedSchedule = false;

    private double timeArrived = 0.0;
    private double assignedTime = 0.0;

    public IntersectionStatManager(double timeArrived){
        this.timeArrived = timeArrived;
    }

    public IntersectionStatManager(){
    }

    public void findEpisodeFinished(double timeNow, Vehicle vehicle){
        Vehicle frontVehicle =  vehicle.lane.getClosestFrontVehicleInLane(vehicle, 0);

        if (isEpisodeDone) return;

        if (frontVehicle != null){
            if (frontVehicle.headPosition < vehicle.headPosition + 2){ //2 meters
                crashed =  true;
                isEpisodeDone = true;
            }
            setGap(frontVehicle.headPosition - vehicle.headPosition);
            setFrontVehicleSpeed(frontVehicle.speed);
        }
        else {
            if (vehicle.lane.getVehicleCount() > 1){
                crashed =  true;
                isEpisodeDone = true;
                setGap(0);
                setFrontVehicleSpeed(vehicle.lane.getVehicles().get(0).speed);
            }
            else {
                setGap(vehicle.lane.edge.length + 100 - vehicle.headPosition);
                setFrontVehicleSpeed(vehicle.lane.edge.freeFlowSpeed);
            }
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

    public void assignIntersectionConstrains(double timeNow, double timeToReach){
        isIntersectionConstraints = true;
        this.setTimeToReach(timeNow+timeToReach);
        this.setTimeRemain(timeToReach);
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
}
