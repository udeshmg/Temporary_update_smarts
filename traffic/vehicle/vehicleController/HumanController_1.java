package traffic.vehicle.vehicleController;

import common.Settings;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.Vehicle;
import traffic.vehicle.VehicleType;
import traffic.vehicle.carfollow.IDM;
import java.util.Random;

public class HumanController_1 extends VehicleController {
    protected int num_steps = 0;
    protected int counter = 0;
    protected double stepsPerSecond = 0;

    public HumanController_1(Vehicle vehicle, Settings settings){
        // Vehicle is not used in current implementation
        this.stepsPerSecond = settings.numStepsPerSecond;
    }

    public HumanController_1(){
        // Vehicle is not used in current implementation
        this.stepsPerSecond = 5;
    }


    public static void main(final String[] args) {
        Settings settings = new Settings();
        Vehicle vehicle = new Vehicle(settings);
        vehicle.type = VehicleType.CAR;
        vehicle.driverProfile = DriverProfile.NORMAL;
        VehicleController controller =  new HumanController_1(vehicle, settings);

        long seed = 25;
        Random random = new Random(seed);
        for (int i = 0 ; i < 60 ; i++) {
            System.out.println(random.nextInt(15)+30);
        }

        //for (int i=0; i<100; i++){
        //    int command = controller.computePaddleCommand(vehicle);
        //    double acc = IDM.computeAccelerationBasedOnCommand(vehicle, command);
        //
        //    vehicle.speed += acc/settings.numStepsPerSecond;
        //    if (vehicle.speed > 22) vehicle.speed = 22;
        //
        //    System.out.println("step: " + i + " speed: " + vehicle.speed + " acc: " + acc);
        //}


    }

    public int computePaddleCommand(Vehicle vehicle) {
        counter += 1;
        if (counter%stepsPerSecond == 0)
            num_steps += 1;

        if (num_steps < 12){
            if (vehicle.speed < 4) return 1;
            else return 1;
        }
        else return 1;
    }

}
