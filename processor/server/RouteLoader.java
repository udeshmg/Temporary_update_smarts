package processor.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import common.Settings;
import processor.communication.message.SerializableExternalVehicle;
import processor.communication.message.SerializableRouteLeg;
import traffic.road.Edge;
import traffic.road.Node;
import traffic.road.RoadNetwork;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.VehicleType;

/**
 * This class loads vehicle routes from external files and create vehicles based
 * on the routes.
 *
 */
public class RouteLoader {
	public class NodeIdComparator implements Comparator<NodeInfo> {
		@Override
		public int compare(final NodeInfo v1, final NodeInfo v2) {
			// TODO Auto-generated method stub
			return v1.getOsmId() > v2.getOsmId() ? -1 : v1.getOsmId() == v2.getOsmId() ? 0 : 1;
		}
	}

	ArrayList<String> vehicles = new ArrayList<>(150000);
	ArrayList<NodeInfo> idMappers = new ArrayList<>(300000);
	RoadNetwork roadNetwork;
	NodeIdComparator nodeIdComparator = new NodeIdComparator();

	ArrayList<WorkerMeta> workers;

	public RouteLoader(RoadNetwork roadNetwork, final ArrayList<WorkerMeta> workers) {
		this.roadNetwork = roadNetwork;
		this.workers = workers;
	}

	/**
	 * Append route of vehicles to the workers whose work area covers the first
	 * node of the route.
	 */
	void assignVehicleToWorker() {
		// Clear routes from previous loading
		for (final WorkerMeta worker : workers) {
			worker.externalRoutes.clear();
		}
		for (final String vehicle : vehicles) {
			SerializableExternalVehicle ev = SerializableExternalVehicle.createFromString(vehicle, roadNetwork, idMappers, nodeIdComparator);
			final Node routeStartNode = roadNetwork.edges.get(ev.route.get(0).edgeIndex).startNode;
			final WorkerMeta routeStartWorker = getWorkerAtRouteStart(routeStartNode);
			routeStartWorker.externalRoutes.add(ev);
		}
	}

	public WorkerMeta getWorkerAtRouteStart(final Node routeStartNode) {
		for (final WorkerMeta worker : workers) {
			if (worker.workarea.workCells.contains(routeStartNode.gridCell)) {
				return worker;
			}
		}
		return null;
	}

	void loadRoutes() {
		if (Settings.inputForegroundVehicleFile.length() > 0) {
			scanXML(Settings.inputForegroundVehicleFile, true);
		}
		if (Settings.inputBackgroundVehicleFile.length() > 0) {
			scanXML(Settings.inputBackgroundVehicleFile, false);
		}
		for (final Node node : roadNetwork.nodes) {
			idMappers.add(new NodeInfo(node.osmId, node.index));
		}
		Collections.sort(idMappers, nodeIdComparator);
		assignVehicleToWorker();
	}

	/**
	 * Scan the given XML file to get routes of vehicles.
	 */
	void scanXML(final String fileName, final boolean foreground) {
		try {

			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser saxParser = factory.newSAXParser();

			final DefaultHandler handler = new DefaultHandler() {

				StringBuilder sbOneV = new StringBuilder();

				@Override
				public void characters(final char ch[], final int start, final int length) {
				}

				@Override
				public void endElement(final String uri, final String localName, final String qName) {

					if (qName.equals("vehicle")) {
						vehicles.add(sbOneV.toString());
					}
				}

				@Override
				public void startElement(final String uri, final String localName, final String qName,
						final Attributes attributes) {

					if (qName.equals("vehicle")) {
						sbOneV.delete(0, sbOneV.length());
						// Vehicle is foreground or background?
						sbOneV.append(String.valueOf(foreground) + Settings.delimiterItem);
						// Vehicle ID
						sbOneV.append(attributes.getValue("id") + Settings.delimiterItem);
						// Vehicle VID
						sbOneV.append(attributes.getValue("vid") + Settings.delimiterItem);
						// Vehicle start time (earliest time the vehicle could be released from parking)
						String start_time = attributes.getValue("start_time");
						if (start_time == null) {
							start_time = "0";
						}
						sbOneV.append(start_time + Settings.delimiterItem);
						// Vehicle type
						String type = attributes.getValue("type");
						if (type == null) {
							type = VehicleType.CAR.name();
						}
						sbOneV.append(type + Settings.delimiterItem);
						// Vehicle driver profile
						String driverProfile = attributes.getValue("driverProfile");
						if (driverProfile == null) {
							driverProfile = DriverProfile.NORMAL.name();
						}
						sbOneV.append(driverProfile + Settings.delimiterItem);
						// Repeat rate of this vehicle
						String repeatRate = attributes.getValue("repeatPerSecond");
						if (repeatRate == null) {
							repeatRate = "0";
						}
						sbOneV.append(repeatRate + Settings.delimiterItem);
					}

					if (qName.equals("node")) {
						// Node ID on vehicle's route
						sbOneV.append(attributes.getValue("id") + "#");
						// Time length the vehicle needs to park at the node
						String stopover = attributes.getValue("stopover");
						if (stopover == null) {
							stopover = "0";
						}
						sbOneV.append(stopover + Settings.delimiterSubItem);
					}
				}

			};

			saxParser.parse(fileName, handler);

		} catch (final Exception exception) {
			System.out.println(exception);
		}
	}

}
