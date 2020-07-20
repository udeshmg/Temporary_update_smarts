package processor.communication.externalMessage;

import traffic.vehicle.Vehicle;

/**
 * Creates a simplified object for Vehicle class for communication
 */

public class VehicleExternal {

    private int vid = 0;
    private double headPosition = 0;
    private double headPoistionFromEnd = 0;
    private double speed = 0;
    private int edgeId = 0;


    public VehicleExternal(final Vehicle vehicle){
        this.headPosition = vehicle.headPosition;
        this.headPoistionFromEnd = vehicle.lane.edge.length - vehicle.headPosition;
        this.speed = vehicle.speed;
        this.edgeId = vehicle.lane.edge.index;
        this.vid = vehicle.vid;
    }

    public int getVid() {
        return vid;
    }

    public void setVid(int vid) {
        this.vid = vid;
    }

    public double getHeadPoistionFromEnd() {
        return headPoistionFromEnd;
    }

    public void setHeadPoistionFromEnd(double headPoistionFromEnd) {
        this.headPoistionFromEnd = headPoistionFromEnd;
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
