package traffic.road;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import traffic.vehicle.Vehicle;

/**
 * Lane is a basic element in road network. An edge contains one or more lanes.
 *
 */
public class Lane {
	/**
	 * The edge that this lane belongs to.
	 */
	public Edge edge;
	/**
	 * Lane number. The lane closest to road side is number 0.
	 */
	public int laneNumber;
	/**
	 * Index of this lane in the whole network.
	 */
	public int index;
	/**
	 * Collection of the vehicles traveling on this lane.
	 */
	public ArrayList<Vehicle> vehicles = new ArrayList<>(50);
	/**
	 * Whether this lane is manually blocked by user.
	 */
	public boolean isBlocked = false;
	/**
	 * Speed of the last vehicle if this lane crosses border between workers.
	 */
	public double speedOfLatestVehicleLeftThisWorker = 100;
	/**
	 * Head position of the last vehicle if this lane crosses border between
	 * workers.
	 */
	public double endPositionOfLatestVehicleLeftThisWorker = 1000000000;
	/**
	 * Whether this lane is being used by a priority vehicle, such as ambulance.
	 */
	public boolean isPriority = false;

	/**
	 * GPS coordinates of start/end points
	 *
	 */
	public double latStart, lonStart, latEnd, lonEnd, latLength, lonLength;
	public VehiclePositionComparator vehiclePositionComparator = new VehiclePositionComparator();

	public Lane(final Edge edge) {
		this.edge = edge;
	}

	public void addVehicleToLane(Vehicle v){
		vehicles.add(v);
		Collections.sort(vehicles, vehiclePositionComparator);
	}

	/**
	 * Comparator of vehicles based on their positions in a lane. The vehicle
	 * closest to the end of the lane, will be the first element in the sorted
	 * list.
	 *
	 */
	public class VehiclePositionComparator implements Comparator<Vehicle> {

		@Override
		public int compare(final Vehicle v1, final Vehicle v2) {
			return v1.headPosition > v2.headPosition ? -1 : v1.headPosition == v2.headPosition ? 0 : 1;
		}
	}
}
