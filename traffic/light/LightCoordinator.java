package traffic.light;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import common.Settings;
import processor.communication.message.SerializableInt;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadUtil;

/**
 * This class handles the initialization of traffic lights and the switch of
 * light colors. A light controls the traffic of a specific inward edge.
 *
 */
public class LightCoordinator {

	List<List<Node>> nodeGroups = new ArrayList<>();

	/**
	 * Groups of traffic lights. Lights in the same group are within a certain
	 * distance to each other.
	 */
	public List<TrafficLightCluster> lightClusters = new ArrayList<>();
	private TLScheduler tlScheduler;

	public void addRemoveLights(final List<Node> nodes, final List<SerializableInt> indexNodesToAddLight,
			final List<SerializableInt> indexNodesToRemoveLight) {
		for (final SerializableInt si : indexNodesToAddLight) {
			nodes.get(si.value).light = true;
		}
		for (final SerializableInt si : indexNodesToRemoveLight) {
			nodes.get(si.value).light = false;
		}
	}

	/**
	 * Group nodes with traffic signals. Nodes in the same group are within
	 * certain distance to each other.
	 */
	void groupAdjacentNodes(final List<Node> lightNodes) {
		nodeGroups.clear();

		for (final Node node : lightNodes) {
			node.idLightNodeGroup = 0;
		}
		if (Settings.trafficLightTiming == TrafficLightTiming.NONE) {
			return;
		}
		/*
		 * For each node with traffic light, find other nodes with lights within
		 * a certain distance.
		 */
		final HashSet<Node> checkedNodes = new HashSet<>();
		for (final Node node : lightNodes) {
			if (checkedNodes.contains(node)) {
				continue;
			}
			final List<Node> nodeGroup = new ArrayList<>();
			final long idNodeGroup = node.osmId;

			nodeGroup.add(node);
			node.idLightNodeGroup = idNodeGroup;

			checkedNodes.add(node);

			for (final Node otherNode : lightNodes) {
				if (checkedNodes.contains(otherNode)) {
					continue;
				}
				if (RoadUtil.getDistInMeters(node.lat, node.lon, otherNode.lat,
						otherNode.lon) < Settings.maxLightGroupRadius) {
					nodeGroup.add(otherNode);
					otherNode.idLightNodeGroup = idNodeGroup;

					checkedNodes.add(otherNode);
				}
			}
			nodeGroups.add(nodeGroup);
		}
	}

	/**
	 * Group inward edges based on the street names in node groups.
	 */
	void groupInwardEdges() {
		lightClusters.clear();
		if (Settings.trafficLightTiming == TrafficLightTiming.NONE) {
			return;
		}

		/*
		 * Identify the lights on the same street based on the name of the
		 * links.
		 */
		for (final List<Node> nodeGroup : nodeGroups) {
			final HashMap<String, Phase> phaseHashMap = new HashMap<>();

			for (final Node node : nodeGroup) {
				for (final Edge edge : node.inwardEdges) {
					if (!phaseHashMap.containsKey(edge.name)) {
						phaseHashMap.put(edge.name, new Phase());
					}
					Phase phase = phaseHashMap.get(edge.name);
					phase.addEdge(edge);
					edge.isDetectedVehicleForLight = false;
				}
			}

			final List<Phase> phaseList = new ArrayList<>();
			phaseList.addAll(phaseHashMap.values());

			lightClusters.add(new TrafficLightCluster(phaseList));

		}
	}

	/**
	 * Initialize traffic lights. Group inward edges based on street names.
	 * There can be multiple streets at the same intersection. Only one street
	 * can get green light in a group at any time. During initialization, the
	 * first street gets green light and other streets get red lights.
	 *
	 */
	public void init(final List<Node> mapNodes, final List<SerializableInt> indexNodesToAddLight,
			final List<SerializableInt> indexNodesToRemoveLight) {
		// Add or remove lights
		addRemoveLights(mapNodes, indexNodesToAddLight, indexNodesToRemoveLight);

		// Groups adjacent nodes with traffic lights based on distance.
		groupAdjacentNodes(mapNodes);
		System.out.println("Grouped adjacent traffic lights.");

		// Groups inward edges of the grouped nodes.
		groupInwardEdges();
		System.out.println("Divided lights in each light group based on street names.");

		tlScheduler = Settings.getLightScheduler();
		if(tlScheduler != null){
			tlScheduler.init(lightClusters);
		}
	}

	public void scheduleLights(TrafficNetwork trafficNetwork, double timeNow){
		if(tlScheduler != null){
			tlScheduler.schedule(trafficNetwork, timeNow);
		}
	}

	/**
	 * Compute how long the active streets have been given their current color.
	 * Change the color and/or change active streets depending on the situation.
	 */
	public void updateLights() {
		for (TrafficLightCluster lightCluster : lightClusters) {
			lightCluster.updateLights();
		}
	}

}
