package processor.communication.externalMessage;

import traffic.vehicle.Vehicle;

/**
 * Creates a simplified object for Vehicle class for communication
 */

public class VehicleExternal {

    private int vid = 0;
    private double headPosition = 0;
    private double headPositionFromEnd = 0;
    private double speed = 0;
    private int edgeId = 0;
    private boolean done = false;
    private double timeRemain = 0;
    private boolean is_success = false;
    private double gap = 0;
    private double frontVehicleSpeed = 0;
    private boolean crashed = false;
    private boolean externalControl = false;
    private boolean isVirtual = false;


    public VehicleExternal(final Vehicle vehicle){
        this.headPosition = vehicle.headPosition;
        this.headPositionFromEnd = vehicle.lane.edge.length - vehicle.headPosition;
        this.speed = vehicle.speed;
        this.edgeId = vehicle.lane.edge.index;
        this.vid = vehicle.vid;
        this.done = vehicle.episodeStat.isEpisodeDone();
        this.timeRemain = vehicle.episodeStat.getTimeRemain();
        this.is_success = vehicle.episodeStat.isSuccess();
        this.crashed = vehicle.episodeStat.isCrashed();
        this.gap = vehicle.episodeStat.getGap();
        this.frontVehicleSpeed = vehicle.episodeStat.getFrontVehicleSpeed();
        this.externalControl = vehicle.episodeStat.isInExternalControl() && vehicle.episodeStat.isIntersectionConstraints();
        this.isVirtual = vehicle.episodeStat.isVirtual();

        if (done) {
            vehicle.episodeStat.setInExternalControl(false);
            vehicle.episodeStat.setIntersectionConstraints(false);
        }

        //Vehicle frontVehicle = vehicle.lane.getClosestFrontVehicleInLane(vehicle, 0);
        //if (frontVehicle != null) {
        //    frontVehicleSpeed = frontVehicle.speed;
        //    gap = frontVehicle.headPosition - vehicle.headPosition;
        //}
        //else {
        //    frontVehicleSpeed = vehicle.lane.edge.freeFlowSpeed;
        //    gap = vehicle.lane.edge.length + 100 - vehicle.headPosition;
        //    if (vehicle.crashed){
        //        gap = 0;
        //    }
        //
        //}

    }

    public int getVid() {
        return vid;
    }

    public void setVid(int vid) {
        this.vid = vid;
    }

    public double getHeadPositionFromEnd() {
        return headPositionFromEnd;
    }

    public void setHeadPositionFromEnd(double headPositionFromEnd) {
        this.headPositionFromEnd = headPositionFromEnd;
    }

    public double getHeadPosition() {
        return headPosition;
    }

    public void setHeadPosition(double headPosition) {
        this.headPosition = headPosition;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(int edgeId) {
        this.edgeId = edgeId;
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
}
