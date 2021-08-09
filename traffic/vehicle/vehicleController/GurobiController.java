package traffic.vehicle.vehicleController;

import common.Settings;
import traffic.vehicle.DriverProfile;
import traffic.vehicle.Vehicle;

import java.util.Random;
//import gurobi.*;
import traffic.vehicle.VehicleType;
/*
public class GurobiController extends VehicleController{


    protected int num_steps = 0;
    protected int counter = 0;
    protected double stepsPerSecond = 0;
    private Random rand = new Random();
    private int acc = 0;


    public GurobiController(Vehicle vehicle, Settings settings){
        // Vehicle is not used in current implementation
        this.stepsPerSecond = settings.numStepsPerSecond;
        long seed = settings.randomTimingGenerator.nextInt(20000);
        System.out.println(seed);
        this.rand = new Random(seed);


    }

    public static void main(final String[] args) {
        Settings settings = new Settings();
        Vehicle vehicle = new Vehicle(settings);
        vehicle.type = VehicleType.CAR;
        vehicle.driverProfile = DriverProfile.NORMAL;
        VehicleController controller = new GurobiController(vehicle, settings);

        controller.computePaddleCommand(vehicle);



    }

    @Override
    public int computePaddleCommand(Vehicle vehicle) {
        try {
            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "mip1.log");
            env.start();

            GRBModel model = new GRBModel(env);

            // Create variables
            GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x");
            GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y");
            GRBVar z = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "z");

            // Set objective: maximize x + y + 2 z
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(1.0, x);
            expr.addTerm(1.0, y);
            expr.addTerm(2.0, z);
            model.setObjective(expr, GRB.MAXIMIZE);

            // Add constraint: x + 2 y + 3 z <= 4
            expr = new GRBLinExpr();
            expr.addTerm(1.0, x);
            expr.addTerm(2.0, y);
            expr.addTerm(3.0, z);
            model.addConstr(expr, GRB.LESS_EQUAL, 4.0, "c0");

            // Add constraint: x + y >= 1
            expr = new GRBLinExpr();
            expr.addTerm(1.0, x);
            expr.addTerm(1.0, y);
            model.addConstr(expr, GRB.GREATER_EQUAL, 1.0, "c1");

            // Optimize model
            model.optimize();

            System.out.println(x.get(GRB.StringAttr.VarName)
                    + " " + x.get(GRB.DoubleAttr.X));
            System.out.println(y.get(GRB.StringAttr.VarName)
                    + " " + y.get(GRB.DoubleAttr.X));
            System.out.println(z.get(GRB.StringAttr.VarName)
                    + " " + z.get(GRB.DoubleAttr.X));

            System.out.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

            // Dispose of model and environment
            model.dispose();
            env.dispose();

            return 1;
        } catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }
        return 1;
    }
}*/
