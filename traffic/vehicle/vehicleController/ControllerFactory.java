package traffic.vehicle.vehicleController;

import common.Settings;
import traffic.vehicle.Vehicle;
import java.util.Random;

public class ControllerFactory {
    Random rand;

    public ControllerFactory(){
         rand = new Random();
    }

    public VehicleController getController(Vehicle v, Settings settings){
        int number = 0; //rand.nextInt(2);
        if (number == 0)
            return new HumanController_4(v, settings);
        else if (number == 1)
            return new HumanController_2(v, settings);
        else
            return new HumanController_2(v, settings);
    }
}
