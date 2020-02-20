package traffic.trafficData;
import com.opencsv.CSVReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


public class TrafficLoader {

    public final String SAMPLE_CSV_FILE_PATH = "C:/Users/pgunarathna/PycharmProjects/trafficSimulator/NycData/yellow_tripdata_2015-06.csv";
    public BufferedReader fileReader = null;
    private double lastTimeRead = 0;
    private double startTimeRead = 0;
    private double endTimeRead = 60;

    public TrafficLoader(){

        try {
            fileReader = new BufferedReader(new FileReader(SAMPLE_CSV_FILE_PATH));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        readHeader();
    }

    public double getStartTimeRead() {
        return startTimeRead;
    }

    public void setTimeLimitRead(double startTimeRead, double endTimeRead) {
        this.startTimeRead = startTimeRead;
        this.endTimeRead = endTimeRead;
    }

    public TrafficLoader(int startTimeRead){
        this.startTimeRead = startTimeRead;
        try {
            fileReader = new BufferedReader(new FileReader(SAMPLE_CSV_FILE_PATH));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        readHeader();
    }

    public static void main(String[] args) throws IOException {

        TrafficLoader tfl = new TrafficLoader();

        try {
            tfl.fileReader = new BufferedReader(new FileReader(tfl.SAMPLE_CSV_FILE_PATH));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        tfl.readHeader();

    }

    public void readHeader(){
        try {
            String[] dataFields = fileReader.readLine().split(",");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Coordinates> readUntilTime(int timeStep){

        ArrayList<Coordinates> vehicleCoordinates = new ArrayList<>();
        while (lastTimeRead <= timeStep + startTimeRead && (timeStep + startTimeRead  < endTimeRead)) {
            String[] dataFields = null;
            try {
                dataFields = fileReader.readLine().split(",");
            } catch (IOException e) {
                e.printStackTrace();
            }
            lastTimeRead = decodeTime(dataFields[1]);
            if ( lastTimeRead > startTimeRead) {
                //System.out.println("Time stamp: " + dataFields[1]);
                Coordinates coord = new Coordinates();
                coord.setLonStart(Double.parseDouble(dataFields[5]));
                coord.setLatStart(Double.parseDouble(dataFields[6]));
                coord.setLonEnd(Double.parseDouble(dataFields[9]));
                coord.setLatEnd(Double.parseDouble(dataFields[10]));

                vehicleCoordinates.add(coord);
            }
        }
        return vehicleCoordinates;
    }

    public int decodeTime(String time){
        int hour = 0;
        int minute = 0;
        Boolean hourReached = false;
        Boolean minuteReached = false;
        int multiplier = 1;

        for (int i = 0; i < time.length(); i++) {

            if (minuteReached) {
                minute = minute * multiplier + Character.getNumericValue(time.charAt(i));
                multiplier *= 10;
            }

            if (time.charAt(i) == ':'){
                minuteReached = true;
                hourReached = false;
                multiplier = 1;
            }

            if (hourReached) {
                hour = hour * multiplier + Character.getNumericValue(time.charAt(i));
                multiplier *= 10;
            }

            if (time.charAt(i) == ' ')
                hourReached = true;


        }

        return 60*hour+minute;
    }

}
