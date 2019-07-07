package processor.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import common.Settings;
import processor.SimServerData;
import processor.SimulationProcessor;
import processor.communication.IncomingConnectionBuilder;
import processor.communication.MessageHandler;
import processor.communication.message.Message_SW_BlockLane;
import processor.communication.message.Message_SW_ChangeSpeed;
import processor.communication.message.Message_SW_KillWorker;
import processor.communication.message.Message_SW_ServerBased_ShareTraffic;
import processor.communication.message.Message_SW_ServerBased_Simulate;
import processor.communication.message.Message_SW_Serverless_Pause;
import processor.communication.message.Message_SW_Serverless_Resume;
import processor.communication.message.Message_SW_Serverless_Start;
import processor.communication.message.Message_SW_Serverless_Stop;
import processor.communication.message.Message_SW_Setup;
import processor.communication.message.Message_WS_Join;
import processor.communication.message.Message_WS_TrafficReport;
import processor.communication.message.Message_WS_ServerBased_SharedMyTrafficWithNeighbor;
import processor.communication.message.Message_WS_Serverless_Complete;
import processor.communication.message.Message_WS_SetupCreatingVehicles;
import processor.communication.message.Message_WS_SetupDone;
import traffic.road.Node;
import traffic.road.RoadNetwork;

/**
 * This class can do: 1) loading and distributing simulation configuration; 2)
 * balancing workload between workers; 3) instructing workers to perform tasks
 * (if server-based synchronization is enabled); 4) visualizing simulation; 5)
 * collecting results from workers; 6) writing results to files.
 * 
 * This class can be run as Java application.
 */
public class Server implements MessageHandler, Runnable, SimulationProcessor {
	ArrayList<WorkerMeta> workerMetas = new ArrayList<>();
	public boolean isSimulating = false;//Whether simulation is running, i.e., it is not paused or stopped
	private boolean isOpenForNewWorkers = true;
	private ArrayList<Message_WS_TrafficReport> receivedTrafficReportCache = new ArrayList<>();
	private SimServerData data = new SimServerData();

	public static void main(final String[] args) {
		if (processCommandLineArguments(args)) {
			new Server().run();
		} else {
			System.out.println("There is an error in command line parameter. Program exits.");
		}
	}

	static boolean processCommandLineArguments(final String[] args) {
		try {
			for (int i = 0; i < args.length; i++) {
				switch (args[i]) {
				case "-gui":
					Settings.isVisualize = Boolean.parseBoolean(args[i + 1]);
					break;
				}
			}
		} catch (final Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Adds a new worker unless simulation is running or the required number of
	 * workers is reached.
	 */
	void addWorker(final Message_WS_Join received) {
		final WorkerMeta worker = new WorkerMeta(received.workerName, received.workerAddress, received.workerPort);
		if (isAllWorkersAtState(WorkerState.NEW) && Settings.numWorkers > workerMetas.size()) {
			workerMetas.add(worker);
			data.showNumberOfConnectedWorkers(workerMetas.size());
			if (workerMetas.size() == Settings.numWorkers) {
				isOpenForNewWorkers = false;// No need for more workers
				data.prepareForSetup();
			}
		} else {
			final Message_SW_KillWorker msd = new Message_SW_KillWorker(Settings.isSharedJVM);
			worker.send(msd);
		}
	}

	/*
	 * Start a server-less simulation.
	 */
	void askWorkersProceedWithoutServer() {
		data.takeTimeStamp();
		final Message_SW_Serverless_Start message = new Message_SW_Serverless_Start(data.getStep());
		for (final WorkerMeta worker : workerMetas) {
			worker.send(message);
			worker.setState(WorkerState.SERVERLESS_WORKING);
		}
	}

	/*
	 * Ask workers to transfer vehicles information to fellow workers in
	 * server-based synchronization mode.
	 */
	void askWorkersShareTrafficDataWithFellowWorkers() {

		// Increment step
		data.setStep(data.getStep() + 1);
		data.takeTimeStamp();

		System.out.println("Doing step " + data.getStep());

		// Ask all workers to share data with fellow
		for (final WorkerMeta worker : workerMetas) {
			worker.setState(WorkerState.SHARING_STARTED);
		}
		for (final WorkerMeta worker : workerMetas) {
			worker.send(new Message_SW_ServerBased_ShareTraffic(data.getStep()));
		}
	}

	/*
	 * Ask workers to update traffic in their corresponding work areas for one
	 * time step. This is called after workers exchange traffic information with
	 * their neighbors in server-based simulation.
	 */
	synchronized void askWorkersSimulateOneStep() {
		boolean[] req = data.updateVehicleCounts();
		for (final WorkerMeta worker : workerMetas) {
			worker.setState(WorkerState.SIMULATING);
		}
		final Message_SW_ServerBased_Simulate message = new Message_SW_ServerBased_Simulate(req[0],
				req[1], req[2], UUID.randomUUID().toString());
		for (final WorkerMeta worker : workerMetas) {
			worker.send(message);
		}
	}

	public void changeMap() {
		data.changeMap();
	}

	@Override
	public void onClose() {
		killConnectedWorkers();
	}

	/**
	 * Change simulation speed by setting pause time after doing a step at all
	 * workers. Note that this will affect simulation time. By default there is
	 * no pause time between steps.
	 */
	public void changeSpeed(final int pauseTimeEachStep) {
		Settings.pauseTimeBetweenStepsInMilliseconds = pauseTimeEachStep;
		final Message_SW_ChangeSpeed message = new Message_SW_ChangeSpeed(Settings.pauseTimeBetweenStepsInMilliseconds);
		for (final WorkerMeta worker : workerMetas) {
			worker.send(message);
		}
	}

	boolean isAllWorkersAtState(final WorkerState state) {
		int count = 0;
		for (final WorkerMeta w : workerMetas) {
			if (w.state == state) {
				count++;
			}
		}

		return count == workerMetas.size();

	}

	public void killConnectedWorkers() {
		final Message_SW_KillWorker msd = new Message_SW_KillWorker(Settings.isSharedJVM);
		for (final WorkerMeta worker : workerMetas) {
			worker.send(msd);
		}
		workerMetas.clear();
		Settings.isNewEnvironment = true;
	}

	public void pauseSim() {
		isSimulating = false;
		if (!Settings.isServerBased) {
			final Message_SW_Serverless_Pause message = new Message_SW_Serverless_Pause(data.getStep());
			for (final WorkerMeta worker : workerMetas) {
				worker.send(message);
			}
		}
	}

	/**
	 * Process received message sent from worker. Based on the received message,
	 * server can update GUI, decide whether to do next time step or finish
	 * simulation and instruct the workers what to do next.
	 */
	@Override
	synchronized public void processReceivedMsg(final Object message) {
		if (message instanceof Message_WS_Join) {
			onWSJoinMessage((Message_WS_Join) message);
		} else if (message instanceof Message_WS_SetupCreatingVehicles) {
			onWSSetupCreatingVehiclesMsg((Message_WS_SetupCreatingVehicles) message);
		} else if (message instanceof Message_WS_SetupDone) {
			onWSSetupDoneMsg((Message_WS_SetupDone) message);
		} else if (message instanceof Message_WS_ServerBased_SharedMyTrafficWithNeighbor) {
			onWSServerBasedShareMyTrafficWithNeighbourMsg((Message_WS_ServerBased_SharedMyTrafficWithNeighbor) message);
		} else if (message instanceof Message_WS_TrafficReport) {
			onWSTrafficReportMsg((Message_WS_TrafficReport) message);
		} else if (message instanceof Message_WS_Serverless_Complete) {
			onWSServerlessCompleteMsg((Message_WS_Serverless_Complete) message);
		}
	}

	synchronized void processCachedReceivedTrafficReports() {
		final Iterator<Message_WS_TrafficReport> iMessage = receivedTrafficReportCache.iterator();
		while (iMessage.hasNext()) {
			final Message_WS_TrafficReport message = iMessage.next();
			data.updateFromReport(message.vehicleList, message.lightList, message.workerName, workerMetas.size(),
					message.step, message.randomRoutes, message.finishedList,message.numInternalNonPubVehicles, message.numInternalTrams, message.numInternalBuses);
			// Remove processed message
			iMessage.remove();
		}
	}

	@Override
	public void run() {
		data.initRoadNetwork();
		data.initUIs(this);
		// Prepare to receive connection request from workers
		new IncomingConnectionBuilder(Settings.serverListeningPortForWorkers, this).start();
	}

	public void resumeSim() {
		isSimulating = true;
		if (Settings.isServerBased) {
			System.out.println("Resuming server-based simulation...");
			askWorkersShareTrafficDataWithFellowWorkers();
		} else {
			System.out.println("Resuming server-less simulation...");
			final Message_SW_Serverless_Resume message = new Message_SW_Serverless_Resume(data.getStep());
			for (final WorkerMeta worker : workerMetas) {
				worker.send(message);
			}
		}
	}

	/**
	 * Update node lists for nodes where traffic light needs to be added or
	 * removed. The lists will be sent to worker during setup.
	 */
	public void setLightChangeNode(final Node node) {
		data.setLightChangeNode(node);
	}

	/**
	 * Resets dynamic fields and sends simulation configuration to workers. The
	 * workers will set up simulation environment upon receiving the
	 * configuration.
	 */
	public void setupNewSim() {
		data.resetVariablesForSetup();
		receivedTrafficReportCache.clear();

		// Reset worker status
		for (final WorkerMeta worker : workerMetas) {
			worker.setState(WorkerState.NEW);
		}

		// In a new environment (map), determine the work areas for all workers
		if (Settings.isNewEnvironment) {
			data.getRoadNetwork().buildGrid();
			WorkloadBalancer.partitionGridCells(workerMetas, data.getRoadNetwork());
		}

		// Determine the number of internal vehicles at all workers
		WorkloadBalancer.assignNumInternalVehiclesToWorkers(workerMetas, data.getRoadNetwork());

		// Assign vehicle routes from external file to workers
		final RouteLoader routeLoader = new RouteLoader(data.getRoadNetwork(), workerMetas);
		routeLoader.loadRoutes();

		data.updateNoOfVehiclesNeededAtStart(routeLoader.vehicles.size());

		// Send simulation configuration to workers
		for (final WorkerMeta worker : workerMetas) {
			worker.send(new Message_SW_Setup(workerMetas, worker, data.getRoadNetwork().edges, data.getStep(), data.getNodesToAddLight(),
					data.getNodesToRemoveLight()));
		}

		data.initFileOutput();

		Settings.isNewEnvironment = false;

		System.out.println("Sent simulation configuration to all workers.");

	}

	@Override
	public void setupMultipleSim() {
		data.startMultipleExperimentRunning();
	}

	@Override
	public boolean loadScript() {
		return data.loadScript();
	}

	public void askWorkersChangeLaneBlock(int laneIndex, boolean isBlocked) {
		for (final WorkerMeta worker : workerMetas) {
			worker.send(new Message_SW_BlockLane(laneIndex, isBlocked));
		}
	}

	void startSimulation() {
		if (data.getStep() < Settings.maxNumSteps) {
			System.out.println("All workers are ready to do simulation.");
			isSimulating = true;
			data.startSimulationInUI();
			if (Settings.isServerBased) {
				System.out.println("Starting server-based simulation...");
				askWorkersShareTrafficDataWithFellowWorkers();
			} else {
				System.out.println("Starting server-less simulation...");
				askWorkersProceedWithoutServer();
			}
		}

	}

	@Override
	public RoadNetwork getRoadNetwork() {
		return data.getRoadNetwork();
	}

	public void stopSim() {
		isSimulating = false;
		data.reserVariablesAtStop();
		processCachedReceivedTrafficReports();
		data.writeOutputFiles(data.getStep());

		// Ask workers stop in server-less mode. Note that workers may already stopped before receiving the message.
		if (!Settings.isServerBased) {
			final Message_SW_Serverless_Stop message = new Message_SW_Serverless_Stop(data.getStep());
			for (final WorkerMeta worker : workerMetas) {
				worker.send(message);
				worker.setState(WorkerState.NEW);
			}
		}

		System.out.println("Simulation stopped.\n");
		if (workerMetas.size() == Settings.numWorkers) {
			data.prepareForSetupAgain();
		}
	}



	synchronized void updateWorkerState(final String workerName, final WorkerState state) {
		for (final WorkerMeta worker : workerMetas) {
			if (worker.name.equals(workerName)) {
				worker.state = state;
				break;
			}
		}
	}

	private void onWSJoinMessage(Message_WS_Join msg){
		if (isOpenForNewWorkers)
			addWorker(msg);
	}

	private void onWSSetupCreatingVehiclesMsg(Message_WS_SetupCreatingVehicles msg){
		data.updateSetupprogressInUI((msg).numVehicles);
	}

	private void onWSSetupDoneMsg(Message_WS_SetupDone msg){
		updateWorkerState((msg).workerName, WorkerState.READY);
		data.updateChannelCount((msg).numFellowWorkers);
		if (isAllWorkersAtState(WorkerState.READY)) {
			data.resetStepInUI();
			startSimulation();
		}
	}

	private void onWSServerBasedShareMyTrafficWithNeighbourMsg(Message_WS_ServerBased_SharedMyTrafficWithNeighbor msg){
		if (!isSimulating) {
			// No need to process the message if simulation was stopped
			return;
		}
		for (final WorkerMeta w : workerMetas) {
			if (w.name.equals(msg.workerName) && (w.state == WorkerState.SHARING_STARTED)) {
				updateWorkerState(msg.workerName, WorkerState.SHARED);
				if (isAllWorkersAtState(WorkerState.SHARED)) {
					askWorkersSimulateOneStep();
				}
				break;
			}
		}
	}

	private void onWSTrafficReportMsg(Message_WS_TrafficReport msg){
		if (!isSimulating) {
			// No need to process the message if simulation was stopped
			return;
		}
		// Cache received reports
		receivedTrafficReportCache.add(msg);
		// Output data from the reports
		processCachedReceivedTrafficReports();
		// Stop if max number of steps is reached in server-based mode
		if (Settings.isServerBased) {
			updateWorkerState(msg.workerName, WorkerState.FINISHED_ONE_STEP);
			if (isAllWorkersAtState(WorkerState.FINISHED_ONE_STEP)) {
				data.updateSimulationTime();
				if (data.getStep() >= Settings.maxNumSteps) {
					stopSim();
				} else if (isSimulating) {
					askWorkersShareTrafficDataWithFellowWorkers();
				}
			}
		} else {
			// Update time step in server-less mode
			if (msg.step > data.getStep()) {
				data.setStep(msg.step);
			}
		}
	}

	private void onWSServerlessCompleteMsg(Message_WS_Serverless_Complete msg){
		if (!isSimulating) {
			// No need to process the message if simulation was stopped
			return;
		}
		if (msg.step > data.getStep()) {
			data.setStep(msg.step);
		}
		updateWorkerState(msg.workerName, WorkerState.NEW);
		if (isAllWorkersAtState(WorkerState.NEW)) {
			data.updateSimulationTime();
			stopSim();
		}
	}
}
