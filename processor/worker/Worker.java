package processor.worker;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;

import common.Settings;
import common.SysUtil;
import processor.SimWorkerData;
import processor.communication.IncomingConnectionBuilder;
import processor.communication.MessageHandler;
import processor.communication.MessageSender;
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
import processor.communication.message.Message_WW_Traffic;
import processor.communication.message.SerializableDouble;
import processor.communication.message.SerializableFrontVehicleOnBorder;
import processor.communication.message.SerializableGridCell;
import processor.communication.message.SerializableInt;
import processor.communication.message.SerializableVehicle;
import processor.communication.message.SerializableWorkerMetadata;
import processor.communication.message.Serializable_GPS_Rectangle;
import traffic.TrafficNetwork;
import traffic.light.LightUtil;
import traffic.road.Edge;
import traffic.road.GridCell;
import traffic.road.Node;
import traffic.road.RoadUtil;
import traffic.routing.RouteUtil;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;

/**
 * Worker receives simulation configuration from server and simulates traffic in
 * a specific area. Operations include:
 * <ul>
 * <li>Initialize work area
 * <li>Generate vehicles
 * <li>Update status of vehicles and traffic lights
 * <li>Exchange traffic information with neighbor workers
 * <li>Report result back to server
 * </ul>
 *
 * Note: IP/port of worker should be directly accessible as the information will
 * be used by server to build TCP connection with the worker.
 *
 * Simulation runs in server-based mode (BSP) or server-less mode (PSP). In BSP,
 * a worker needs to wait two messages from server at each time step: one asking
 * the worker to share traffic information with its fellow workers, another
 * asking the worker to simulate (computing models for vehicles and traffic
 * lights). In PSP, worker synchronize simulation with its fellow workers
 * automatically. Worker can send information about vehicles and traffic lights
 * to server in both modes, if server asks the worker to do so during setup. The
 * information can be used to update GUI at server or a remote controller.
 */
public class Worker implements MessageHandler, Runnable {
	class ConnectionBuilderTerminationTask extends TimerTask {
		@Override
		public void run() {
			connectionBuilder.terminate();
		}
	}

	/**
	 * Starts worker and tries to connect with server
	 *
	 * @param args
	 *            server address (optional)
	 */
	public static void main(final String[] args) {

		if (args.length > 0) {
			Settings.serverAddress = args[0];
		}

		new Worker().run();
	}

	Worker me = this;
	IncomingConnectionBuilder connectionBuilder;
	ArrayList<Fellow> fellowWorkers = new ArrayList<>();
	String name = "";
	MessageSender senderForServer;
	String address = "localhost";
	int listeningPort;
	Workarea workarea;
	ArrayList<Fellow> connectedFellows = new ArrayList<>();//Fellow workers that share at least one edge with this worker
	ArrayList<Message_WW_Traffic> receivedTrafficCache = new ArrayList<>();
	boolean isDuringServerlessSim;//Once server-less simulation begins, this will true until the simulation ends
	boolean isPausingServerlessSim;
	Thread singleWorkerServerlessThread = new Thread();//Used when this worker is the only worker in server-less mode
	protected int numVehicleCreatedSinceLastSetupProgressReport = 0;
	protected SimWorkerData data = new SimWorkerData();

	
	void buildThreadForSingleWorkerServerlessSimulation() {
		singleWorkerServerlessThread = new Thread() {
			public void run() {
				while (data.getStep() < Settings.maxNumSteps) {
					if (!isDuringServerlessSim) {
						break;
					}
					while (isPausingServerlessSim) {
						try {
							sleep(1);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					data.simulateOneStep(me, true, true, true);
					sendTrafficReportInServerlessMode();
					data.setStep(data.getStep() + 1);
				}
				// Finish simulation
				senderForServer.send(new Message_WS_Serverless_Complete(name, data.getStep(), data.getTrafficNetwork().getVehicleCount()));
			}
		};
	}

	public void sendTrafficReportInServerlessMode() {
		if ((data.getStep() + 1) % Settings.trafficReportStepGapInServerlessMode == 0) {
			senderForServer.send(new Message_WS_TrafficReport(name, data.getStep(), data.getTrafficNetwork()));
			data.clearReportedTrafficData();
		}
	}

	void createVehiclesFromFellow(final Message_WW_Traffic messageToProcess) {
		for (final SerializableVehicle serializableVehicle : messageToProcess.vehiclesEnteringReceiver) {
			final Vehicle vehicle = serializableVehicle.createVehicle(data.getTrafficNetwork());
			data.addTransferredVehicle(vehicle);
		}
	}

	/**
	 * Divide edges overlapping with the responsible area of this worker into
	 * two sets based on their closeness to the border of the responsible area.
	 */
	Map<String, List<Edge>> divideLaneSetForSim() {
		Map<String, List<Edge>> setMap = new HashMap<>();
		List<Edge> pspBorderEdges = new ArrayList<>();// For PSP (server-less)
		List<Edge> pspNonBorderEdges = new ArrayList<>();
		final HashSet<Edge> edgeSet = new HashSet<>();
		for (final Fellow fellow : fellowWorkers) {
			for (final Edge e : fellow.inwardEdgesAcrossBorder) {
				edgeSet.add(e);
				edgeSet.addAll(findInwardEdgesWithinCertainDistance(e.startNode, 0, 28.0 / Settings.numStepsPerSecond,
						edgeSet));
			}
		}
		pspBorderEdges.addAll(edgeSet);
		for (final Edge e : data.getTrafficNetwork().edges) {
			if (!edgeSet.contains(e)) {
				pspNonBorderEdges.add(e);
			}
		}
		setMap.put("Border", pspBorderEdges);
		setMap.put("NonBorder", pspNonBorderEdges);
		return setMap;
	}

	/**
	 * Find the fellow workers that need to communicate with this worker. Each
	 * of these fellow workers shares at least one edge with this worker.
	 */
	void findConnectedFellows() {
		connectedFellows.clear();
		for (final Fellow fellowWorker : fellowWorkers) {
			if ((fellowWorker.inwardEdgesAcrossBorder.size() > 0)
					|| (fellowWorker.outwardEdgesAcrossBorder.size() > 0)) {
				connectedFellows.add(fellowWorker);
			}
		}
	}

	HashSet<Edge> findInwardEdgesWithinCertainDistance(final Node node, final double accumulatedDistance,
			final double maxDistance, final HashSet<Edge> edgesToSkip) {
		for (final Edge e : node.inwardEdges) {
			if (edgesToSkip.contains(e)) {
				continue;
			} else {
				edgesToSkip.add(e);
				final double updatedAccumulatedDistance = accumulatedDistance + e.length;
				if (updatedAccumulatedDistance < maxDistance) {
					findInwardEdgesWithinCertainDistance(e.startNode, accumulatedDistance, maxDistance, edgesToSkip);
				}
			}
		}
		return edgesToSkip;
	}

	boolean isAllFellowsAtState(final FellowState state) {
		int count = 0;
		for (final Fellow w : connectedFellows) {
			if (w.state == state) {
				count++;
			}
		}
		return count == connectedFellows.size();

	}

	/**
	 * Create a new worker. Set the worker's name, address and listening port.
	 * The worker comes with a new receiver for receiving messages, e.g.,
	 * connection requests, from other entities such as workers.
	 */
	void join() {
		// Get IP address
		address = SysUtil.getMyIpV4Addres();
		// Find an available port
		listeningPort = Settings.serverListeningPortForWorkers + 1
				+ (new Random()).nextInt(65535 - Settings.serverListeningPortForWorkers);
		while (true) {
			try {
				final ServerSocket ss = new ServerSocket(listeningPort);
				ss.close();
				break;
			} catch (final IOException e) {
				listeningPort = 60000 + (new Random()).nextInt(65535 - 60000);
				continue;
			}
		}
		connectionBuilder = new IncomingConnectionBuilder(listeningPort, this);
		connectionBuilder.start();
		senderForServer = new MessageSender(Settings.serverAddress, Settings.serverListeningPortForWorkers);
		name = SysUtil.getRandomID(4);
		workarea = new Workarea(name, null);
		senderForServer.send(new Message_WS_Join(name, address, listeningPort));
	}

	void proceedBasedOnSyncMethod() {
		// In case neighbor already sent traffic for this step
		processCachedReceivedTraffic();

		if (isAllFellowsAtState(FellowState.SHARED)) {
			if (Settings.isServerBased) {
				senderForServer.send(new Message_WS_ServerBased_SharedMyTrafficWithNeighbor(name));
			} else if (isDuringServerlessSim) {
				sendTrafficReportInServerlessMode();

				// Proceed to next step or finish
				if (data.getStep() >= Settings.maxNumSteps) {
					senderForServer
							.send(new Message_WS_Serverless_Complete(name, data.getStep(), data.getTrafficNetwork().getVehicleCount()));
					resetTraffic();
				} else if (!isPausingServerlessSim) {
					data.setStep(data.getStep() + 1);
					data.simulateOneStep(this, true, true, true);
					proceedBasedOnSyncMethod();
				}
			}
		}
	}

	void processCachedReceivedTraffic() {
		final Iterator<Message_WW_Traffic> iMessage = receivedTrafficCache.iterator();

		while (iMessage.hasNext()) {
			final Message_WW_Traffic message = iMessage.next();
			if (message.stepAtSender == data.getStep()) {
				processReceivedTraffic(message);
				iMessage.remove();
			}
		}
	}

	ArrayList<GridCell> processReceivedGridCells(final ArrayList<SerializableGridCell> received,
			final GridCell[][] grid) {
		final ArrayList<GridCell> cellsInWorkarea = new ArrayList<>();
		for (final SerializableGridCell receivedCell : received) {
			cellsInWorkarea.add(grid[receivedCell.row][receivedCell.column]);
		}
		return cellsInWorkarea;
	}

	void processReceivedMetadataOfWorkers(final ArrayList<SerializableWorkerMetadata> metadataWorkers) {
		// Set work area of all workers
		for (final SerializableWorkerMetadata metadata : metadataWorkers) {
			final ArrayList<GridCell> cellsInWorkarea = processReceivedGridCells(metadata.gridCells,
					data.getTrafficNetwork().grid);
			if (metadata.name.equals(name)) {
				workarea.setWorkCells(cellsInWorkarea);
			} else {
				final Fellow fellow = new Fellow(metadata.name, metadata.address, metadata.port, cellsInWorkarea);
				fellowWorkers.add(fellow);
			}
		}

		// Identify edges shared with fellow workers
		for (final Fellow fellowWorker : fellowWorkers) {
			fellowWorker.getEdgesFromAnotherArea(workarea);
			fellowWorker.getEdgesToAnotherArea(workarea);
		}

		// Identify fellow workers that share edges with this worker
		findConnectedFellows();

		// Prepare communication with the fellow workers that share edges with
		// this worker
		for (final Fellow fellowWorker : connectedFellows) {
			fellowWorker.prepareCommunication();
		}
	}

	@Override
	public synchronized void processReceivedMsg(final Object message) {
		if (message instanceof Message_SW_Setup) {
			onSWSetupMessage((Message_SW_Setup) message);
		} else if (message instanceof Message_SW_ServerBased_ShareTraffic) {
			onSWServerBasedShareTrafficMsg((Message_SW_ServerBased_ShareTraffic) message);
		} else if (message instanceof Message_WW_Traffic) {
			onWWTrafficMsg((Message_WW_Traffic) message);
		} else if (message instanceof Message_SW_ServerBased_Simulate) {
			onSWServerBasedSimulate((Message_SW_ServerBased_Simulate) message);
		} else if (message instanceof Message_SW_Serverless_Start) {
			onSWServerlessStart((Message_SW_Serverless_Start) message);
		} else if (message instanceof Message_SW_KillWorker) {
			onSWKillWorker((Message_SW_KillWorker) message);
		} else if (message instanceof Message_SW_Serverless_Stop) {
			onSWServerlessStop((Message_SW_Serverless_Stop) message);
		} else if (message instanceof Message_SW_Serverless_Pause) {
			onSWServerlessPause((Message_SW_Serverless_Pause) message);
		} else if (message instanceof Message_SW_Serverless_Resume) {
			onSWServerlessResume((Message_SW_Serverless_Resume) message);
		} else if (message instanceof Message_SW_ChangeSpeed) {
			onSWChangeSpeed((Message_SW_ChangeSpeed) message);
		} else if (message instanceof Message_SW_BlockLane) {
			onSWBlockLane((Message_SW_BlockLane) message);
		}
	}

	void processReceivedSimulationConfiguration(final Message_SW_Setup received) {
		received.setupFromMessage();
		data.setStep(received.startStep);
		data.resetVariables(received.numRandomPrivateVehicles, received.numRandomTrams, received.numRandomBuses);

		if (received.isNewEnvironment) {
			data.initTrafficNetwork(received.roadGraph);
			processReceivedMetadataOfWorkers(received.metadataWorkers);
			data.initSimulation(divideLaneSetForSim());
		} else {
			data.resetExistingNetwork();
		}

		data.resetLights(getLightNodes());
	}

	void processReceivedTraffic(final Message_WW_Traffic messageToProcess) {
		updateFellowState(messageToProcess.senderName, FellowState.SHARING_DATA_RECEIVED);
		createVehiclesFromFellow(messageToProcess);
		updateTrafficAtOutgoingEdgesToFellows(messageToProcess);
	}

	void resetTraffic() {
		for (final Fellow fellow : fellowWorkers) {
			fellow.vehiclesToCreateAtBorder.clear();
			fellow.state = FellowState.SHARED;
		}
		receivedTrafficCache.clear();
		data.resetTraffic();
	}

	@Override
	public void run() {
		// Join system by connecting with server
		join();
	}

	/**
	 * Send vehicle position on cross-border edges to fellow workers.
	 */
	void transferVehicleDataToFellow() {
		for (final Fellow fellowWorker : connectedFellows) {
			fellowWorker.send(new Message_WW_Traffic(name, fellowWorker, data.getStep()));
			updateFellowState(fellowWorker.name, FellowState.SHARING_DATA_SENT);
			fellowWorker.vehiclesToCreateAtBorder.clear();
		}
	}

	void updateFellowState(final String workerName, final FellowState newState) {
		for (final Fellow fellow : connectedFellows) {
			if (fellow.name.equals(workerName)) {
				if ((fellow.state == FellowState.SHARING_DATA_RECEIVED)
						&& (newState == FellowState.SHARING_DATA_SENT)) {
					fellow.state = FellowState.SHARED;
				} else if ((fellow.state == FellowState.SHARING_DATA_SENT)
						&& (newState == FellowState.SHARING_DATA_RECEIVED)) {
					fellow.state = FellowState.SHARED;
				} else {
					fellow.state = newState;
				}
				break;
			}
		}
	}

	void updateTrafficAtOutgoingEdgesToFellows(final Message_WW_Traffic received) {
		for (final SerializableFrontVehicleOnBorder info : received.lastVehiclesLeftReceiver) {
			data.updateTrafficAtOutgoingEdgesToFellows(info.laneIndex, info.endPosition, info.speed);
		}
	}

	public void onSWSetupMessage(Message_SW_Setup msg){
		processReceivedSimulationConfiguration(msg);
		data.buildEnvironment(workarea.workCells, workarea.workerName);
		resetTraffic();
		//Pause a bit so other workers can reset before starting simulation
		try {
			Thread.sleep(500);
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		data.initTrafficLightCoordinator(msg.indexNodesToAddLight, msg.indexNodesToRemoveLight, getLightNodes());
		// Reset fellow state
		for (final Fellow connectedFellow : connectedFellows) {
			connectedFellow.state = FellowState.SHARED;
		}
		// Create vehicles
		numVehicleCreatedSinceLastSetupProgressReport = 0;
		final TimerTask progressTimerTask = new TimerTask() {
			@Override
			public void run() {
				senderForServer.send(new Message_WS_SetupCreatingVehicles(
						data.getTrafficNetwork().getVehicleCount() - numVehicleCreatedSinceLastSetupProgressReport));
				numVehicleCreatedSinceLastSetupProgressReport = data.getTrafficNetwork().getVehicleCount();
			}
		};
		final Timer progressTimer = new Timer();
		final Random random = new Random();
		if (Settings.isVisualize) {
			progressTimer.scheduleAtFixedRate(progressTimerTask, 500, random.nextInt(1000) + 1);
		}
		data.createVehicles(msg.externalRoutes);
		progressTimerTask.cancel();
		progressTimer.cancel();
		// Let server know that setup is done
		senderForServer.send(new Message_WS_SetupDone(name, connectedFellows.size()));
	}

	protected ArrayList<Node> getLightNodes(){
		/*
		 * Collects the nodes with traffic lights in the current work area
		 */
		final ArrayList<Node> lightNodes = new ArrayList<>();
		for (final Node node : data.getTrafficNetwork().nodes) {
			if (workarea.workCells.contains(node.gridCell) && node.light && (node.inwardEdges.size() > 0)) {
				lightNodes.add(node);
			}
		}
		return lightNodes;
	}

	private void onSWServerBasedShareTrafficMsg(Message_SW_ServerBased_ShareTraffic msg){
		data.setStep(msg.currentStep);
		transferVehicleDataToFellow();
		proceedBasedOnSyncMethod();
	}

	private void onWWTrafficMsg(Message_WW_Traffic msg){
		receivedTrafficCache.add(msg);
		proceedBasedOnSyncMethod();
	}

	public void onSWServerBasedSimulate(Message_SW_ServerBased_Simulate msg){
		data.simulateOneStep(this, msg.isNewNonPubVehiclesAllowed, msg.isNewTramsAllowed, msg.isNewBusesAllowed);
		senderForServer.send(new Message_WS_TrafficReport(name, data.getStep(), data.getTrafficNetwork()));
		data.clearReportedTrafficData();
	}

	private void onSWServerlessStart(Message_SW_Serverless_Start msg){
		data.setStep(msg.startStep);
		isDuringServerlessSim = true;
		isPausingServerlessSim = false;

		if (connectedFellows.size() == 0) {
			buildThreadForSingleWorkerServerlessSimulation();
			singleWorkerServerlessThread.start();
		} else {
			data.simulateOneStep(this, true, true, true);
			proceedBasedOnSyncMethod();
		}
	}

	private void onSWKillWorker(Message_SW_KillWorker msg){
		// Quit depending on how the worker was started
		if (msg.isSharedJVM) {
			final ConnectionBuilderTerminationTask task = new ConnectionBuilderTerminationTask();
			new Timer().schedule(task, 1);
		} else {
			System.exit(0);
		}
	}

	private void onSWServerlessStop(Message_SW_Serverless_Stop msg){
		isDuringServerlessSim = false;
		isPausingServerlessSim = false;
		singleWorkerServerlessThread.stop();
		resetTraffic();
	}

	private void onSWServerlessPause(Message_SW_Serverless_Pause msg){
		isPausingServerlessSim = true;
	}

	private void onSWServerlessResume(Message_SW_Serverless_Resume msg){
		isPausingServerlessSim = false;
		// When it is not single worker environment, explicitly resume the routine tasks
		if (connectedFellows.size() > 0) {
			proceedBasedOnSyncMethod();
		}
	}

	private void onSWChangeSpeed(Message_SW_ChangeSpeed msg){
		Settings.pauseTimeBetweenStepsInMilliseconds = msg.pauseTimeBetweenStepsInMilliseconds;
	}

	private void onSWBlockLane(Message_SW_BlockLane msg){
		data.changeLaneBlock(msg.laneIndex, msg.isBlocked);
	}


	public SimWorkerData getData() {
		return data;
	}
}
