package traffic.vehicle.vehicleController;

import common.Settings;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;
import traffic.vehicle.carfollow.IDM;

public class HumanController_2 extends VehicleController {
    protected int num_steps = 0;
    protected int counter = 0;
    protected double stepsPerSecond = 0;

    public HumanController_2(Vehicle vehicle, Settings settings){
        // Vehicle is not used in current implementation
        this.stepsPerSecond = settings.numStepsPerSecond;
    }
    public static void main(final String[] args) {
        Settings settings = new Settings();
        Vehicle vehicle = new Vehicle(settings);
        vehicle.type = VehicleType.CAR;
        VehicleController controller = new HumanController_2(vehicle, settings);

        for (int i = 0; i < 150; i++) {
            int command = controller.computePaddleCommand(vehicle);
            double acc = IDM.computeAccelerationBasedOnCommand(vehicle, command);

            vehicle.speed += acc / settings.numStepsPerSecond;
            if (vehicle.speed > 22) vehicle.speed = 22;
            if (vehicle.speed < 0) vehicle.speed = 0;

            System.out.println("step: " + i + " speed: " + vehicle.speed + " acc: " + acc);
        }
    }

    @Override
    public int computePaddleCommand(Vehicle vehicle) {
        counter++;
        if (counter%stepsPerSecond == 0)
            num_steps++;

        if (num_steps < 8){
            return 1;
        }
        else if (num_steps < 15){
            return -1;
        }
        else return 1;
    }
}
