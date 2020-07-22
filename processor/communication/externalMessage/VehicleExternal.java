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


    public VehicleExternal(final Vehicle vehicle){
        this.headPosition = vehicle.headPosition;
        this.headPositionFromEnd = vehicle.lane.edge.length - vehicle.headPosition;
        this.speed = vehicle.speed;
        this.edgeId = vehicle.lane.edge.index;
        this.vid = vehicle.vid;
        this.done = vehicle.isEpisodeDone();
        this.timeRemain = vehicle.getTimeRemain();
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
}
