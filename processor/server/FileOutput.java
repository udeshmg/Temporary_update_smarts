package processor.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;

import common.Settings;
import common.SysUtil;
import processor.communication.message.SerializableRouteDump;
import processor.communication.message.SerializableRouteDumpPoint;
import processor.communication.message.SerializableTrajectory;

public class FileOutput {
	FileOutputStream fosLog;
	FileOutputStream fosTrajectory;
	FileOutputStream fosRoute;

	/**
	 * Close output file
	 */
	public void close() {
		try {
			if (fosLog != null) {
				fosLog.close();
			}
			if (fosTrajectory != null) {
				fosTrajectory.close();
			}
			if (fosRoute != null) {
				outputStringToFile(fosRoute, "</data>");
				fosRoute.close();
			}
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void init() {
		if (Settings.isOutputTrajectory) {
			initTrajectoryOutputFile();
		}
		if (Settings.isOutputInitialRoutes) {
			initRouteOutputFile();
		}
		if (Settings.isOutputSimulationLog) {
			initSimLogOutputFile();
		}
	}

	File getNewFile(String prefix) {
		String fileName = prefix + SysUtil.getTimeStampString() + ".txt";
		File file = new File(fileName);
		int counter = 0;
		while (file.exists()) {
			counter++;
			fileName = prefix + SysUtil.getTimeStampString() + "_" + counter + ".txt";
			file = new File(fileName);
		}
		return file;
	}

	void initRouteOutputFile() {
		try {
			final File file = getNewFile(Settings.prefixOutputRoutePlan);
			fosRoute = new FileOutputStream(file, true);
			outputStringToFile(fosRoute,
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + System.getProperty("line.separator"));
			outputStringToFile(fosRoute, "<data>" + System.getProperty("line.separator"));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void initSimLogOutputFile() {

		try {
			final File file = getNewFile(Settings.prefixOutputSimLog);
			// Print column titles
			fosLog = new FileOutputStream(file, true);
			outputStringToFile(fosLog, "Time Stamp, Real Time(s), Simulation Time(s), # of Worker-Worker Connections"
					+ System.getProperty("line.separator"));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	void initTrajectoryOutputFile() {
		try {
			final File file = getNewFile(Settings.prefixOutputTrajectory);
			// Print column titles
			fosTrajectory = new FileOutputStream(file, true);
			outputStringToFile(fosTrajectory,
					"Trajectory ID,Vehicle ID,Time Stamp,Latitude,Longitude" + System.getProperty("line.separator"));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Output trajectory of individual vehicles
	 */
	public void outputTrajectories(HashMap<String, TreeMap<Double, double[]>> allTrajectories) {
		if (Settings.isOutputTrajectory) {
			int trajectoryId = 0;
			for (String vehicleId : allTrajectories.keySet()) {
				// Trajectory counter	
				trajectoryId++;
				outputStringToFile(fosTrajectory, String.valueOf(trajectoryId));
				outputStringToFile(fosTrajectory, ",");
				outputStringToFile(fosTrajectory, vehicleId);
				outputStringToFile(fosTrajectory, ",");
				TreeMap<Double, double[]> points = allTrajectories.get(vehicleId);
				ArrayList<Double> timeStamps = new ArrayList<Double>(points.keySet());
				for (int i = 0; i < timeStamps.size(); i++) {
					if (i > 0) {
						outputStringToFile(fosTrajectory, ",,");
					}
					double timeStamp = timeStamps.get(i);
					outputStringToFile(fosTrajectory, String.valueOf(timeStamp));
					outputStringToFile(fosTrajectory, ",");
					double[] point = points.get(timeStamp);
					outputStringToFile(fosTrajectory, String.valueOf(point[0]));
					outputStringToFile(fosTrajectory, ",");
					outputStringToFile(fosTrajectory, String.valueOf(point[1]));

					outputStringToFile(fosTrajectory, System.getProperty("line.separator"));

				}
			}
		}
	}

	public void outputRoutes(final ArrayList<SerializableRouteDump> routes) {
		if (Settings.isOutputInitialRoutes) {
			try {
				final StringBuilder sb = new StringBuilder();
				for (final SerializableRouteDump route : routes) {
					sb.append("<vehicle ");
					sb.append("id=\"" + route.vehicleId + "\" type=\"" + route.type + "\" start_time=\""
							+ route.startTime + "\" driverProfile=\"" + route.driverProfile + "\">"
							+ System.getProperty("line.separator"));
					for (final SerializableRouteDumpPoint point : route.routeDumpPoints) {
						sb.append("<node ");
						if (point.stopDuration == 0) {
							sb.append("id=\"" + point.nodeId + "\"/>" + System.getProperty("line.separator"));
						} else {
							sb.append("id=\"" + point.nodeId + "\" stopover=\"" + point.stopDuration + "\"/>"
									+ System.getProperty("line.separator"));
						}
					}
					sb.append("</vehicle>" + System.getProperty("line.separator"));

				}
				outputStringToFile(fosRoute, sb.toString());
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void outputSimLog(final int stepCurrent, final double simulationTimeCounter, final int totalNumFellowsOfWorker) {
		final Date date = new Date();

		if (Settings.isOutputSimulationLog && (fosLog != null)) {
			outputStringToFile(fosLog, date.toString());
			outputStringToFile(fosLog, ",");
			outputStringToFile(fosLog, String.valueOf(stepCurrent / Settings.numStepsPerSecond));
			outputStringToFile(fosLog, ",");
			outputStringToFile(fosLog, String.valueOf(simulationTimeCounter));
			outputStringToFile(fosLog, ",");
			outputStringToFile(fosLog, String.valueOf(totalNumFellowsOfWorker));
			outputStringToFile(fosLog, System.getProperty("line.separator"));
		}
	}

	void outputStringToFile(final FileOutputStream fos, final String str) {

		final byte[] dataInBytes = str.getBytes();

		try {
			fos.write(dataInBytes);
			fos.flush();
		} catch (final IOException e) {
		}
	}

}
