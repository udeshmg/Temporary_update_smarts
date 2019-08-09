package traffic.light;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import common.Settings;
import processor.communication.message.SerializableInt;
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
	public List<TrafficLightCluster> lightGroups = new ArrayList<>();

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
		lightGroups.clear();
		if (Settings.trafficLightTiming == TrafficLightTiming.NONE) {
			return;
		}

		/*
		 * Identify the lights on the same street based on the name of the
		 * links.
		 */
		for (final List<Node> nodeGroup : nodeGroups) {
			final HashMap<String, List<Edge>> inwardEdgeGroupsHashMap = new HashMap<>();

			for (final Node node : nodeGroup) {
				for (final Edge edge : node.inwardEdges) {
					if (!inwardEdgeGroupsHashMap.containsKey(edge.name)) {
						inwardEdgeGroupsHashMap.put(edge.name, new ArrayList<Edge>());
					}
					inwardEdgeGroupsHashMap.get(edge.name).add(edge);
					edge.isDetectedVehicleForLight = false;
				}
			}

			final List<List<Edge>> inwardEdgeGroupsList = new ArrayList<>();
			inwardEdgeGroupsList.addAll(inwardEdgeGroupsHashMap.values());

			lightGroups.add(new TrafficLightCluster(inwardEdgeGroupsList));

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

		// Reset timer of light groups
		for (final TrafficLightCluster egbn : lightGroups) {
			resetGYR(egbn);
		}

		// Groups adjacent nodes with traffic lights based on distance.
		groupAdjacentNodes(mapNodes);
		System.out.println("Grouped adjacent traffic lights.");

		// Groups inward edges of the grouped nodes.
		groupInwardEdges();
		System.out.println("Divided lights in each light group based on street names.");

		// Set green color to the first street at any light group
		for (final TrafficLightCluster egbn : lightGroups) {
			egbn.phaseIndex = 0;
			setGYR(egbn, LightColor.GYR_G);
		}

	}

	/**
	 * Check whether the current active approach has a priority vehicle.
	 */
	boolean isPriorityVehicleInActiveApproach(final TrafficLightCluster egbn) {
		for (final Edge e : egbn.phases.get(egbn.phaseIndex)) {
			if (e.isEdgeContainsPriorityVehicle()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether inactive approaches have a priority vehicle.
	 */
	boolean isPriorityVehicleInInactiveApproach(final TrafficLightCluster egbn) {
		for (int i = 0; i < egbn.phases.size(); i++) {
			for (final Edge e : egbn.phases.get(i)) {
				if (e.isEdgeContainsPriorityVehicle()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check whether there is vehicle coming to the current approach under
	 * active control.
	 */
	boolean isTrafficExistAtActiveStreet(final TrafficLightCluster egbn) {
		for (final Edge e : egbn.phases.get(egbn.phaseIndex)) {
			if (e.isDetectedVehicleForLight) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether there is incoming vehicle at conflicting approaches.
	 */
	boolean isTrafficExistAtNonActiveStreet(final TrafficLightCluster egbn) {
		for (int i = 0; i < egbn.phases.size(); i++) {
			if (i == egbn.phaseIndex) {
				continue;
			}
			for (final Edge e : egbn.phases.get(i)) {
				if (e.isDetectedVehicleForLight) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Reset timer of all light groups.
	 *
	 */
	public void resetGYR(final TrafficLightCluster edgeGroupsAtNode) {
		for (final List<Edge> edgeGraoup : edgeGroupsAtNode.phases) {
			for (final Edge edge : edgeGraoup) {
				edge.lightColor = LightColor.GYR_G;
			}
		}
		edgeGroupsAtNode.trafficSignalTimerGYR = LightColor.GYR_G.minDynamicTime;
		edgeGroupsAtNode.trafficSignalAccumulatedGYRTime = 0;
	}

	/**
	 * Set the color of an active street and initialize the timer for the color.
	 * Non-active streets get red lights.
	 */
	public void setGYR(final TrafficLightCluster edgeGroupsAtNode, final LightColor type) {
		for (int i = 0; i < edgeGroupsAtNode.phases.size(); i++) {
			if (i == edgeGroupsAtNode.phaseIndex) {
				for (final Edge edge : edgeGroupsAtNode.phases.get(i)) {
					edge.lightColor = type;
				}
			} else {
				for (final Edge edge : edgeGroupsAtNode.phases.get(i)) {
					edge.lightColor = LightColor.KEEP_RED;
				}
			}
		}
		edgeGroupsAtNode.trafficSignalAccumulatedGYRTime = 0;
		if (Settings.trafficLightTiming == TrafficLightTiming.DYNAMIC) {
			edgeGroupsAtNode.trafficSignalTimerGYR = type.minDynamicTime;
		} else if (Settings.trafficLightTiming == TrafficLightTiming.FIXED) {
			edgeGroupsAtNode.trafficSignalTimerGYR = type.fixedTime;
		}
	}

	/**
	 * Compute how long the active streets have been given their current color.
	 * Change the color and/or change active streets depending on the situation.
	 */
	public void updateLights() {
		final double secEachStep = 1 / Settings.numStepsPerSecond;
		for (int i = 0; i < lightGroups.size(); i++) {
			final TrafficLightCluster egbn = lightGroups.get(i);
			egbn.trafficSignalAccumulatedGYRTime += secEachStep;

			final Edge anEdgeInActiveApproach = egbn.phases.get(egbn.phaseIndex).get(0);
			if (isPriorityVehicleInInactiveApproach(egbn) && !isPriorityVehicleInActiveApproach(egbn)) {
				// Grant green light to an inactive approach it has priority vehicle and the current active approach does not have one
				egbn.phaseIndex = getEdgeGroupIndexOfPriorityInactiveApproach(egbn);
				setGYR(egbn, LightColor.GYR_G);
			}
			if (!isPriorityVehicleInInactiveApproach(egbn) && isPriorityVehicleInActiveApproach(egbn)) {
				// Grant green light to current active approach if it has a priority vehicle and inactive approaches do not have priority vehicle
				setGYR(egbn, LightColor.GYR_G);
			}

			if (anEdgeInActiveApproach.lightColor == LightColor.GYR_G) {
				if (Settings.trafficLightTiming == TrafficLightTiming.DYNAMIC) {
					if (!isTrafficExistAtNonActiveStreet(egbn)) {
						continue;
					} else if (egbn.trafficSignalTimerGYR <= egbn.trafficSignalAccumulatedGYRTime) {
						// Switch to yellow if traffic waiting at conflicting approach
						if (!isTrafficExistAtActiveStreet(egbn)) {
							setGYR(egbn, LightColor.GYR_Y);
						} else {
							// Without conflicting traffic: increment green light time if possible; change to yellow immediately if max green time passed
							if (egbn.trafficSignalAccumulatedGYRTime < LightColor.GYR_G.maxDynamicTime) {
								egbn.trafficSignalTimerGYR += secEachStep;
							} else {
								setGYR(egbn, LightColor.GYR_Y);
							}
						}
					}
				} else if ((Settings.trafficLightTiming == TrafficLightTiming.FIXED)
						&& (egbn.trafficSignalTimerGYR <= egbn.trafficSignalAccumulatedGYRTime)) {
					setGYR(egbn, LightColor.GYR_Y);
				}
			} else if ((anEdgeInActiveApproach.lightColor == LightColor.GYR_Y)
					&& (egbn.trafficSignalTimerGYR <= egbn.trafficSignalAccumulatedGYRTime)) {
				setGYR(egbn, LightColor.GYR_R);
			} else if ((anEdgeInActiveApproach.lightColor == LightColor.GYR_R
					|| anEdgeInActiveApproach.lightColor == LightColor.KEEP_RED)
					&& (egbn.trafficSignalTimerGYR <= egbn.trafficSignalAccumulatedGYRTime)) {
				// Starts GYR cycle for next group of edges	(Switching Phase)
				egbn.phaseIndex = (egbn.phaseIndex + 1) % egbn.phases.size();
				setGYR(egbn, LightColor.GYR_G);
			}

			// Reset vehicle detection flag at all edges
			for (final List<Edge> edgeGroup : egbn.phases) {
				for (final Edge edge : edgeGroup) {
					edge.isDetectedVehicleForLight = false;
				}
			}
		}

	}

	int getEdgeGroupIndexOfPriorityInactiveApproach(TrafficLightCluster egbn) {
		for (int i = 0; i < egbn.phases.size(); i++) {
			if (i == egbn.phaseIndex) {
				continue;
			}
			for (Edge e : egbn.phases.get(i)) {
				if (e.isEdgeContainsPriorityVehicle()) {
					return i;
				}
			}
		}
		return -1;
	}

}
