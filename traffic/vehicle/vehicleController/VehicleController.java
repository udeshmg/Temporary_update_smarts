package traffic.vehicle.vehicleController;

import common.Settings;
import processor.communication.externalMessage.VehicleControl;
import traffic.vehicle.Vehicle;

public abstract class VehicleController {


    public abstract int computePaddleCommand(Vehicle vehicle);
}
