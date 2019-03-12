package traffic.vehicle.lanechange;

import traffic.vehicle.Vehicle;

/**
 * This class makes lane-change decisions. Current implementation uses MOBIL
 * model, which can be changed to other models.
 *
 */
public class LaneChange {

	MOBIL mobil;

	public LaneChange() {
		mobil = new MOBIL();
	}

	/**
	 * Uses a lane-changing model to decide lane change.
	 *
	 */
	public LaneChangeDirection decideLaneChange(final Vehicle vehicle) {
		return mobil.decideLaneChange(vehicle);
	}
}
