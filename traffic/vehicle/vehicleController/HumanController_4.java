package traffic.vehicle.vehicleController;

import common.Settings;
import traffic.vehicle.Vehicle;

import java.util.Random;

public class HumanController_4 extends VehicleController {
    protected int num_steps = 0;
    protected int counter = 0;
    protected double stepsPerSecond = 0;
    private Random rand = new Random();
    private int acc = 0;

    public HumanController_4(Vehicle vehicle, Settings settings){
        // Vehicle is not used in current implementation
        this.stepsPerSecond = settings.numStepsPerSecond;
        long seed = settings.randomTimingGenerator.nextInt(20000);
        System.out.println(seed);
        this.rand = new Random(seed);
    }

    @Override
    public int computePaddleCommand(Vehicle vehicle) {
        counter++;
        if (counter % stepsPerSecond == 0){
            acc = rand.nextInt(3);
            num_steps++;
        }
        if (num_steps < 2){ //initial steps
            return 1;
        }
        if (acc == 0 && vehicle.speed < 1){ // if speed is lower than 0.1 keep the same speed
            return 0;
        }
        return acc-1;
    }
}
