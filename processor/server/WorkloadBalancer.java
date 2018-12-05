package processor.server;

import java.util.ArrayList;
import java.util.Random;

import common.Settings;
import traffic.road.GridCell;
import traffic.road.RoadNetwork;

/**
 * Balance work load, i.e., the number of random background vehicles, between
 * workers. A worker will not generate new vehicles if the number of vehicles
 * running at the worker reaches the assigned volume.
 */
public class WorkloadBalancer {
	static Random random = new Random();

	static void assignNumInternalVehiclesToWorkers(final ArrayList<WorkerMeta> workers, final RoadNetwork roadNetwork) {
		if ((Settings.listRouteSourceWindowForInternalVehicle.size() == 0)
				&& (Settings.listRouteSourceDestinationWindowForInternalVehicle.size() == 0)) {
			assignNumInternalVehiclesToWorkersBasedOnWorkarea(workers);
		} else {
			assignNumInternalVehiclesToWorkersBasedOnSourceWindow(workers, roadNetwork);
		}
	}

	/**
	 * Determine the number of random background vehicles at each worker. There
	 * should be at least one user-defined source window when this method is
	 * called. A worker, whose work area contains cells that intersect one or
	 * more source windows, needs to maintain a certain number of random
	 * background vehicles. A worker with more intersected cells needs to
	 * maintain a higher number of vehicles.
	 *
	 */
	static void assignNumInternalVehiclesToWorkersBasedOnSourceWindow(final ArrayList<WorkerMeta> workers,
			final RoadNetwork roadNetwork) {
		final ArrayList<double[]> windows = new ArrayList<>();
		windows.addAll(Settings.listRouteSourceDestinationWindowForInternalVehicle);
		windows.addAll(Settings.listRouteSourceWindowForInternalVehicle);
		final double latPerRow = (Math.abs(roadNetwork.maxLat - roadNetwork.minLat) / Settings.numGridRows) + 0.0000001;
		final double lonPerCol = (Math.abs(roadNetwork.maxLon - roadNetwork.minLon) / Settings.numGridCols) + 0.0000001;
		final ArrayList<GridCell> cellsInWindows = new ArrayList<>();
		for (final double[] window : windows) {
			final int minCol = (int) Math.floor(Math.abs(window[0] - roadNetwork.minLon) / lonPerCol);
			final int maxRow = (int) Math.floor(Math.abs(window[1] - roadNetwork.minLat) / latPerRow);
			final int maxCol = (int) Math.floor(Math.abs(window[2] - roadNetwork.minLon) / lonPerCol);
			final int minRow = (int) Math.floor(Math.abs(window[3] - roadNetwork.minLat) / latPerRow);

			for (int row = minRow; row <= maxRow; row++) {
				for (int col = minCol; col <= maxCol; col++) {
					if (row < 0 || col < 0 || row >= Settings.numGridRows || col >= Settings.numGridCols) {
						continue;
					}
					if (!cellsInWindows.contains(roadNetwork.grid[row][col])) {
						cellsInWindows.add(roadNetwork.grid[row][col]);
					}
				}
			}
		}

		for (final WorkerMeta worker : workers) {
			int numWorkerareaCellsInWindows = 0;
			for (final GridCell cell : worker.workarea.workCells) {
				if (cellsInWindows.contains(cell)) {
					numWorkerareaCellsInWindows++;
				}
			}
			final double ratio = (double) numWorkerareaCellsInWindows / cellsInWindows.size();
			worker.numRandomPrivateVehicles = (int) (Settings.numGlobalRandomPrivateVehicles * ratio);
			worker.numRandomTrams = (int) (Settings.numGlobalRandomTrams * ratio);
			worker.numRandomBuses = (int) (Settings.numGlobalRandomBuses * ratio);

		}
	}

	/**
	 * Determine the number of random background vehicles at each worker. Each
	 * worker gets roughly the same number of vehicles as the total lane length
	 * at each worker is roughly the same.
	 *
	 */
	static void assignNumInternalVehiclesToWorkersBasedOnWorkarea(final ArrayList<WorkerMeta> workers) {
		int totalNumAssignedPrivateVehicles = 0;
		int totalNumAssignedTrams = 0;
		int totalNumAssignedBuses = 0;

		// Assign numbers to workers except the last one
		for (int i = 0; i < (workers.size() - 1); i++) {
			workers.get(i).numRandomPrivateVehicles = (int) (workers.get(i).laneLengthRatioAgainstWholeMap
					* Settings.numGlobalRandomPrivateVehicles);
			totalNumAssignedPrivateVehicles += workers.get(i).numRandomPrivateVehicles;
			workers.get(
					i).numRandomTrams = (int) (workers.get(i).laneLengthRatioAgainstWholeMap * Settings.numGlobalRandomTrams);
			totalNumAssignedTrams += workers.get(i).numRandomTrams;
			workers.get(
					i).numRandomBuses = (int) (workers.get(i).laneLengthRatioAgainstWholeMap * Settings.numGlobalRandomBuses);
			totalNumAssignedBuses += workers.get(i).numRandomBuses;
		}
		// Assign numbers to the last worker
		workers.get(workers.size() - 1).numRandomPrivateVehicles = Settings.numGlobalRandomPrivateVehicles
				- totalNumAssignedPrivateVehicles;
		workers.get(workers.size() - 1).numRandomTrams = Settings.numGlobalRandomTrams - totalNumAssignedTrams;
		workers.get(workers.size() - 1).numRandomBuses = Settings.numGlobalRandomBuses - totalNumAssignedBuses;
	}

	/**
	 * Determine work areas of workers. A work area consists of one or more grid
	 * cells. The total lane length of a worker area is similar to that of
	 * another work area.
	 */
	public static void partitionGridCells(final ArrayList<WorkerMeta> workers, final RoadNetwork roadNetwork) {

		// Clear existing grid cells in the work area of each worker
		for (final WorkerMeta worker : workers) {
			worker.workarea.workCells.clear();
		}

		final GridCell[][] grid = roadNetwork.grid;

		double laneLengthWholeMap = 0;
		for (int i = 0; i < Settings.numGridRows; i++) {
			for (int j = 0; j < Settings.numGridCols; j++) {
				laneLengthWholeMap += grid[i][j].laneLength;
			}
		}

		final double optimalLaneLengthPerWorker = laneLengthWholeMap / Settings.numWorkers;

		int totalLaneLengthInCurrentWorkarea = 0;
		int workerIndex = 0;
		for (int row = 0; row < Settings.numGridRows; row++) {
			for (int col = 0; col < Settings.numGridCols; col++) {

				final int nextTotalLength = totalLaneLengthInCurrentWorkarea + grid[row][col].laneLength;

				if ((nextTotalLength > optimalLaneLengthPerWorker) && (workerIndex < (Settings.numWorkers - 1))) {
					final boolean isAddCellToCurrentWorker = random.nextBoolean();
					if (isAddCellToCurrentWorker) {
						totalLaneLengthInCurrentWorkarea += grid[row][col].laneLength;
						workers.get(workerIndex).workarea.workCells.add(grid[row][col]);
						// Update lane length ratio (roads in current worker vs. whole map)
						workers.get(workerIndex).laneLengthRatioAgainstWholeMap = totalLaneLengthInCurrentWorkarea
								/ laneLengthWholeMap;
						workerIndex++;
						totalLaneLengthInCurrentWorkarea = 0;
					} else {
						workerIndex++;
						totalLaneLengthInCurrentWorkarea = 0;
						totalLaneLengthInCurrentWorkarea += grid[row][col].laneLength;
						workers.get(workerIndex).workarea.workCells.add(grid[row][col]);
					}

				} else {
					totalLaneLengthInCurrentWorkarea += grid[row][col].laneLength;
					workers.get(workerIndex).workarea.workCells.add(grid[row][col]);
					// Update lane length ratio (roads in current worker vs. whole map)
					workers.get(workerIndex).laneLengthRatioAgainstWholeMap = totalLaneLengthInCurrentWorkarea
							/ laneLengthWholeMap;
				}

			}
		}

		for (final WorkerMeta worker : workers) {
			System.out.println("Worker " + worker.name + "'s area has " + worker.workarea.workCells.size() + " cells.");
		}

	}

}
