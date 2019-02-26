package traffic.routing;

import java.util.ArrayList;
import java.util.List;

import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

public abstract class Routing {
	public enum Algorithm {
		DIJKSTRA, RANDOM_A_STAR
	}

	TrafficNetwork trafficNetwork;

    public Routing(final TrafficNetwork trafficNetwork) {
		this.trafficNetwork = trafficNetwork;
	}

	public abstract ArrayList<RouteLeg> createCompleteRoute(Edge startEdge, Edge endEdge, VehicleType type);

	

}
