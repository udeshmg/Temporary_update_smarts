package processor.communication.externalMessage;

import java.util.ArrayList;

public class SimulatorExternalControlObjects {
    public ArrayList<RoadControl> edges;
    public ArrayList<VehicleControl> vehicles;
    //public ArrayList<IntersectionControl> intersections;

    SimulatorExternalControlObjects(){
        edges = new ArrayList<>();
        vehicles = new ArrayList<>();
    }

}
