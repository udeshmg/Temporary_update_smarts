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
	private List<Fellow> connectedFellows;
	ArrayList<Vehicle> oneStepData_vehiclesReachedFellowWorker = new ArrayList<>();
	ArrayList<Vehicle> oneStepData_allVehiclesReachedDestination = new ArrayList<>();

	public Simulation(final TrafficNetwork trafficNetwork, final ArrayList<Fellow> connectedFellows) {
		this.trafficNetwork = trafficNetwork;
		this.connectedFellows = connectedFellows;
	}

	void clearOneStepData() {
		oneStepData_vehiclesReachedFellowWorker.clear();
		oneStepData_allVehiclesReachedDestination.clear();
	}



	ArrayList<Vehicle> moveVehicleForward(final double timeNow, final ArrayList<Edge> edges, Worker worker) {
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

	void moveVehicleToNextLink(final double timeNow, final ArrayList<Vehicle> vehiclesToCheck) {
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



	synchronized void simulateOneStep(final Worker worker, boolean isNewNonPubVehiclesAllowed,
			boolean isNewTramsAllowed, boolean isNewBusesAllowed) {
		pause();
		moveVehiclesAroundBorder(worker);
		transferDataTofellow(worker);
		moveVehiclesNotAroundBorder(worker);
		removeTripFinishedVehicles();
		trafficNetwork.changeLaneOfVehicles(worker.timeNow);
		trafficNetwork.updateTrafficLights();
		trafficNetwork.updateTramStopTimers();
		trafficNetwork.releaseVehicleFromParking(worker.timeNow);
		trafficNetwork.blockTramAtTramStop();
		trafficNetwork.removeActiveVehicles(oneStepData_vehiclesReachedFellowWorker);
		trafficNetwork.createInternalVehicles(worker.numLocalRandomPrivateVehicles, worker.numLocalRandomTrams,
				worker.numLocalRandomBuses, isNewNonPubVehiclesAllowed, isNewTramsAllowed, isNewBusesAllowed,
				worker.timeNow);
		trafficNetwork.repeatExternalVehicles(worker.step, worker.timeNow);

		// Clear one-step data
		clearOneStepData();
	}

	void moveVehiclesAroundBorder(final Worker worker){
		final ArrayList<Vehicle> vehiclesAroundBorder = moveVehicleForward(worker.timeNow, worker.pspBorderEdges,
				worker);
		moveVehicleToNextLink(worker.timeNow, vehiclesAroundBorder);
	}

	void transferDataTofellow(final Worker worker){
		if (!Settings.isServerBased) {
			worker.transferVehicleDataToFellow();
		}
	}

	void moveVehiclesNotAroundBorder(final Worker worker){
		final ArrayList<Vehicle> vehiclesNotAroundBorder = moveVehicleForward(worker.timeNow, worker.pspNonBorderEdges,
				worker);
		moveVehicleToNextLink(worker.timeNow, vehiclesNotAroundBorder);
	}

	void removeTripFinishedVehicles(){
		trafficNetwork.removeActiveVehicles(oneStepData_allVehiclesReachedDestination);
	}
}
