package traffic.network;

import traffic.road.Edge;
import traffic.vehicle.VehicleType;

public class ODDemand{

    public ODDemand(){
        this.origin = null;
        this.destination = null;
        this.numVehicles = 0;
        this.vehicleType = null;
    }


    public ODDemand(Edge origin, Edge destination, int numVehicles, VehicleType vehicleType) {
        this.origin = origin;
        this.destination = destination;
        this.numVehicles = numVehicles;
        this.vehicleType = vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public Edge getOrigin() {
        return origin;
    }

    public void setOrigin(Edge origin) {
        this.origin = origin;
    }

    public void setDestination(Edge destination) {
        this.destination = destination;
    }

    public void setNumVehicles(int numVehicles) {
        this.numVehicles = numVehicles;
    }

    public Edge getDestination() {
        return destination;
    }

    public int getNumVehicles() {
        return numVehicles;
    }

    private Edge origin;
    private Edge destination;
    private int numVehicles;
    private VehicleType vehicleType;

}
