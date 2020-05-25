package processor.server.gui;

import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.List;

import traffic.road.Edge;
import traffic.road.RoadType;

public class DrawingObject {

	public static class EdgeObject {
		double startNodeLon, startNodeLat, endNodeLon, endNodeLat;
		int index, numLanes;
		String note = "";
		boolean leftLaneBlocked = false, rightLaneBlocked = false;
		boolean[] laneBlocks;
		double length;
		RoadType type;
		List<LaneObject> lanes;

		public EdgeObject(final double startNodeLon, final double startNodeLat, final double endNodeLon,
				final double endNodeLat, final int index, final int numLanes, final String note, final double length,
				final RoadType type, List<LaneObject> lanes) {
			super();
			this.startNodeLon = startNodeLon;
			this.startNodeLat = startNodeLat;
			this.endNodeLon = endNodeLon;
			this.endNodeLat = endNodeLat;
			this.index = index;
			this.numLanes = numLanes;
			this.note = note;
			laneBlocks = new boolean[this.numLanes];
			for (int i = 0; i < laneBlocks.length; i++) {
				laneBlocks[i] = false;
			}
			this.length = length;
			this.type = type;
			this.lanes = lanes;
		}

		public void updateNote(Edge edge){
			note = "";
			if (edge.name.length() > 0) {
				note += "\"" + edge.name + "\", ";
			}
			note += edge.type.name() + ", ";
			note += numLanes + " lane(s), ";
			note += (int) edge.length + "m, ";
			note += "'" + edge.startNode.index + "' to '" + edge.endNode.index + "', ";
			note += "Idx " + edge.index + ", ";
			note += (int) (edge.getFreeFlowSpeedAtPos() * 3.6) + " " + ( edge.getFreeFlowSpeedAtPos(0)* 3.6 ) + "kmh";
		}

	}



	public static class EdgeObjectComparator implements Comparator<EdgeObject> {
		@Override
		public int compare(final EdgeObject e1, final EdgeObject e2) {
			return e1.index < e2.index ? -1 : e1.index == e2.index ? 0 : 1;
		}
	}

	public static class LaneObject {
		double latStart, lonStart, latEnd, lonEnd;
		int index;

		public LaneObject(double latStart, double lonStart, double latEnd, double lonEnd) {
			this.latStart = latStart;
			this.lonStart = lonStart;
			this.latEnd = latEnd;
			this.lonEnd = lonEnd;
		}

		public LaneObject(double latStart, double lonStart, double latEnd, double lonEnd, int index) {
			this.latStart = latStart;
			this.lonStart = lonStart;
			this.latEnd = latEnd;
			this.lonEnd = lonEnd;
			this.index = index;
		}

		public void changeCoordinates(){
			double temp = latStart;
			latStart = latEnd;
			latEnd = temp;

			temp = lonStart;
			lonStart = lonEnd;
			lonEnd = temp;
		}
	}

	public static class NodeObject{
	    double lon,lat;
	    List<Point2D> polygon;

        public NodeObject(double lon, double lat, List<Point2D> polygon) {
            this.lon = lon;
            this.lat = lat;
            this.polygon = polygon;
        }
    }

	public static class IntersectionObject {
		double lon, lat;
		int edgeIndex;
		boolean isAtEdgeStart = true;

		public IntersectionObject(final double lon, final double lat, final int edgeIndex,
				final boolean isAtEdgeStart) {
			this.lon = lon;
			this.lat = lat;
			this.edgeIndex = edgeIndex;
			this.isAtEdgeStart = isAtEdgeStart;
		}
	}

	public static class TramStopObject {
		double lon, lat;

		public TramStopObject(final double lon, final double lat) {
			super();
			this.lon = lon;
			this.lat = lat;
		}
	}
}
