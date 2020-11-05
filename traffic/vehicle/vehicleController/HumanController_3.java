package traffic.vehicle.vehicleController;

import common.Settings;
import traffic.vehicle.Vehicle;

public class HumanController_3 extends VehicleController {
    protected int num_steps = 0;
    protected int counter = 0;
    protected double stepsPerSecond = 0;

    public HumanController_3(Vehicle vehicle, Settings settings){
        // Vehicle is not used in current implementation
        this.stepsPerSecond = settings.numStepsPerSecond;
    }

    @Override
    public int computePaddleCommand(Vehicle vehicle) {
        counter++;
        if (counter%stepsPerSecond == 0)
            num_steps++;

        if (num_steps < 5){
            return 1;
        }
        else if (num_steps < 10){
            return -1;
        }
        else return 1;
    }
}
