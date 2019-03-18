package processor.worker;

import java.util.ArrayList;
import java.util.List;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.vehicle.Vehicle;

/**
 * This class performs simulation at worker. The simulation includes a sequence
 * of tasks, such as moving vehicles forward based on their speed and
 * surrounding environment, making lane changes, updating traffic lights, etc.
 *
 */
public class Simulation {
	TrafficNetwork trafficNetwork;
	ArrayList<Vehicle> oneStepData_vehiclesReachedFellowWorker = new ArrayList<>();
	ArrayList<Vehicle> oneStepData_allVehiclesReachedDestination = new ArrayList<>();

	public Simulation(final TrafficNetwork trafficNetwork) {
		this.trafficNetwork = trafficNetwork;
	}

	void clearOneStepData() {
		oneStepData_vehiclesReachedFellowWorker.clear();
		oneStepData_allVehiclesReachedDestination.clear();
	}



	ArrayList<Vehicle> moveVehicleForward(final double timeNow, final List<Edge> edges) {
		final ArrayList<Vehicle> vehicles = new ArrayList<>();
		for (final Edge edge : edges) {
			double accumulatedVehicleSpeed = 0;
			int numVehiclesOnEdge = 0;
			for (final Lane lane : edge.getLanes()) {
				for (final Vehicle vehicle : lane.getVehicles()) {

					if (!vehicle.active) {
						continue;
					}

					vehicles.add(vehicle);
					numVehiclesOnEdge++;

					vehicle.moveForward(timeNow);

					// Update accumulated vehicle speed
					accumulatedVehicleSpeed += vehicle.speed;
					vehicle.reRoute(timeNow, trafficNetwork.routingAlgorithm);
					if(vehicle.isFinished()){
						oneStepData_allVehiclesReachedDestination.add(vehicle);
					}
					// Set priority lanes
					vehicle.setPriorityLanes(true);
				}

			}
			// Update average vehicle speed for this lane
			if (numVehiclesOnEdge > 0) {
				edge.currentSpeed = accumulatedVehicleSpeed / numVehiclesOnEdge;
			} else {
				edge.currentSpeed = edge.freeFlowSpeed;
			}
		}
		return vehicles;
	}

	void moveVehicleToNextLink(List<Fellow> connectedFellows, final double timeNow, final ArrayList<Vehicle> vehiclesToCheck) {
		for (final Vehicle vehicle : vehiclesToCheck) {

			if (!vehicle.active) {
				continue;
			}
			vehicle.moveToNextLink(timeNow, connectedFellows);
			if(vehicle.isFinished()){
				oneStepData_allVehiclesReachedDestination.add(vehicle);
			}
			if(vehicle.isReachedFellow()){
				oneStepData_vehiclesReachedFellowWorker.add(vehicle);
			}
		}

	}

	/**
	 * Pause thread. This can be useful for observing simulation on GUI.
	 */
	void pause() {
		if (Settings.pauseTimeBetweenStepsInMilliseconds > 0) {
			try {
				Thread.sleep(Settings.pauseTimeBetweenStepsInMilliseconds);
			} catch (final InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Check whether a vehicle reaches the work area of a fellow worker.
	 */
	public static boolean reachFellow(List<Fellow> connectedFellows, final Vehicle vehicle) {
		if (vehicle.lane == null) {
			return false;
		}
		for (final Fellow fellowWorker : connectedFellows) {
			for (final Edge edge : fellowWorker.inwardEdgesAcrossBorder) {
				if (edge == vehicle.lane.edge) {
					vehicle.active = false;
					fellowWorker.vehiclesToCreateAtBorder.add(vehicle);
					return true;
				}
			}
		}
		return false;
	}



	synchronized public void simulateOneStep(Worker worker, double timeNow, int step, List<Edge> pspBorderEdges,
											 List<Edge> pspNonBorderEdges, int numLocalRandomPrivateVehicles, int numLocalRandomTrams,
											 int numLocalRandomBuses, boolean isNewNonPubVehiclesAllowed,
			boolean isNewTramsAllowed, boolean isNewBusesAllowed) {
		pause();
		moveVehiclesAroundBorder(worker.connectedFellows, timeNow, pspBorderEdges);
		transferDataTofellow(worker);
		moveVehiclesNotAroundBorder(worker.connectedFellows, timeNow, pspNonBorderEdges);
		removeTripFinishedVehicles();
		trafficNetwork.changeLaneOfVehicles(timeNow);
		trafficNetwork.updateTrafficLights();
		trafficNetwork.updateTramStopTimers();
		trafficNetwork.releaseVehicleFromParking(timeNow);
		trafficNetwork.blockTramAtTramStop();
		trafficNetwork.removeActiveVehicles(oneStepData_vehiclesReachedFellowWorker);
		trafficNetwork.createInternalVehicles(numLocalRandomPrivateVehicles, numLocalRandomTrams,
				numLocalRandomBuses, isNewNonPubVehiclesAllowed, isNewTramsAllowed, isNewBusesAllowed,
				timeNow);
		trafficNetwork.repeatExternalVehicles(step, timeNow);

		// Clear one-step data
		clearOneStepData();
	}

	void moveVehiclesAroundBorder(List<Fellow> connectedFellows, double timeNow, List<Edge> pspBorderEdges){
		final ArrayList<Vehicle> vehiclesAroundBorder = moveVehicleForward(timeNow, pspBorderEdges);
		moveVehicleToNextLink(connectedFellows, timeNow, vehiclesAroundBorder);
	}

	void transferDataTofellow(final Worker worker){
		if (!Settings.isServerBased) {
			worker.transferVehicleDataToFellow();
		}
	}

	void moveVehiclesNotAroundBorder(List<Fellow> connectedFellows, double timeNow, List<Edge> pspNonBorderEdges){
		final ArrayList<Vehicle> vehiclesNotAroundBorder = moveVehicleForward(timeNow, pspNonBorderEdges);
		moveVehicleToNextLink(connectedFellows, timeNow, vehiclesNotAroundBorder);
	}

	void removeTripFinishedVehicles(){
		trafficNetwork.removeActiveVehicles(oneStepData_allVehiclesReachedDestination);
	}
}
