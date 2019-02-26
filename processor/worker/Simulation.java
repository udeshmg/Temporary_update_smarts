package processor.worker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import common.Settings;
import processor.communication.message.SerializableTrajectoryPoint;
import traffic.TrafficNetwork;
import traffic.light.TrafficLightTiming;
import traffic.road.Edge;
import traffic.road.Lane;
import traffic.routing.RouteLeg;
import traffic.vehicle.CarFollow;
import traffic.vehicle.LaneChange;
import traffic.vehicle.LaneChangeDirection;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;
import traffic.vehicle.VehicleUtil;

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
					/*
					 * Re-route vehicle in certain situations
					 */
					if ((vehicle.type != VehicleType.TRAM) && (Settings.isAllowReroute)) {
						boolean reRoute = false;
						// Reroute happens if vehicle has moved too slowly for too long or the road is blocked ahead
						if (vehicle.indexLegOnRoute < (vehicle.routeLegs.size() - 1)) {
							if ((timeNow - vehicle.timeJamStart) > vehicle.driverProfile.minRerouteTimeGap
									|| vehicle.isRoadBlockedAhead) {
								reRoute = true;
							}
						}

						if (reRoute) {
							// Cancel priority lanes
							vehicle.setPriorityLanes(false);

							// Reroute vehicle
							trafficNetwork.routingAlgorithm.reRoute(vehicle);

							// Reset jam start time
							vehicle.timeJamStart = timeNow;
							// Increment reroute count
							vehicle.numReRoute++;
							// Limit number of re-route for internal vehicle
							if ((vehicle.numReRoute > Settings.maxNumReRouteOfInternalVehicle) && !vehicle.isExternal) {
								oneStepData_allVehiclesReachedDestination.add(vehicle);
							}
						}
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
			double overshootDist = vehicle.headPosition - vehicle.lane.edge.length;

			if (overshootDist >= 0) {

				// Cancel priority lanes
				vehicle.setPriorityLanes(false);

				while ((vehicle.indexLegOnRoute < vehicle.routeLegs.size()) && (overshootDist >= 0)) {
					// Update head position
					vehicle.headPosition -= vehicle.lane.edge.length;
					// Update route leg
					vehicle.indexLegOnRoute++;

					// Check whether vehicle finishes trip
					if (vehicle.active && (vehicle.indexLegOnRoute >= vehicle.routeLegs.size())) {
						oneStepData_allVehiclesReachedDestination.add(vehicle);
						if (vehicle.isForeground) {
							vehicle.timeTravel = timeNow - vehicle.timeRouteStart;
						}
						break;
					}
					// Locate the new lane of vehicle. If the specified lane does not exist (e.g., moving from primary road to secondary road), change to the one with the highest lane number
					final RouteLeg nextLeg = vehicle.routeLegs.get(vehicle.indexLegOnRoute);
					final Edge nextEdge = nextLeg.edge;
					Lane newLane = null;
					if (nextEdge.getLaneCount() <= vehicle.lane.laneNumber) {
						newLane = nextEdge.getLane(nextEdge.getLaneCount() - 1);
					} else {
						newLane = nextEdge.getLane(vehicle.lane.laneNumber);
					}
					vehicle.updateLane(newLane);
					// Remember the cluster of traffic lights
					if (nextEdge.startNode.idLightNodeGroup != 0) {
						vehicle.idLightGroupPassed = nextEdge.startNode.idLightNodeGroup;
					}
					// Update the overshoot distance of vehicle
					overshootDist -= nextEdge.length;
					// Check whether vehicle reaches fellow worker
					if (reachFellow( connectedFellows, vehicle)) {
						oneStepData_vehiclesReachedFellowWorker.add(vehicle);
						break;
					}
					// Park vehicle as plan if vehicle remains on the same
					// worker
					if (nextLeg.stopover > 0) {
						vehicle.park(false, timeNow);
						break;
					}
				}

				// Set priority lanes
				vehicle.setPriorityLanes(true);
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
		if (Settings.trafficLightTiming != TrafficLightTiming.NONE) {
			trafficNetwork.lightCoordinator.updateLights();
		}
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
