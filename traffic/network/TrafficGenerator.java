package traffic.network;

import traffic.TrafficNetwork;
import traffic.road.Edge;

import java.util.ArrayList;
import java.util.List;

public abstract class TrafficGenerator {

    public ODDistributor getOdDistributor() {
        return odDistributor;
    }

    public TemporalDistributor getTemporalDistributor() {
        return temporalDistributor;
    }

    public VehicleTypeDistributor getVehicleTypeDistributor() {
        return vehicleTypeDistributor;
    }

    public void setOdDistributor(ODDistributor odDistributor) {
        this.odDistributor = odDistributor;
    }

    public void setTemporalDistributor(TemporalDistributor temporalDistributor) {
        this.temporalDistributor = temporalDistributor;
    }

    public void setVehicleTypeDistributor(VehicleTypeDistributor vehicleTypeDistributor) {
        this.vehicleTypeDistributor = vehicleTypeDistributor;
    }

    private ODDistributor odDistributor = null;
    private TemporalDistributor temporalDistributor = null;
    private VehicleTypeDistributor vehicleTypeDistributor = null;

    public TrafficGenerator(ODDistributor odDistributor, TemporalDistributor temporalDistributor, VehicleTypeDistributor vehicleTypeDistributor) {
        this.odDistributor = odDistributor;
        this.temporalDistributor = temporalDistributor;
        this.vehicleTypeDistributor = vehicleTypeDistributor;
    }

    public TrafficGenerator(){
    }

    public abstract ArrayList<ODDemand> getGeneratedTraffic(TrafficNetwork trafficNetwork, List<Edge> possibleStartEdges, List<Edge> possibleEndEdges, int timeStep);
}
