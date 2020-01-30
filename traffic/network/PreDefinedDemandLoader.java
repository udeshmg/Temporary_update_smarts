package traffic.network;

public class PreDefinedDemandLoader extends TemporalDistributor {

    private int counter = 0;
    private int maxCounter = 3;
    private int [] demandMatrix = {5,6,7};

    @Override
    public int getCurrentVehicleLimit(int amount, int currentStep, int maxStep) {
        iterate();
        return demandMatrix[counter];
    }

    private void iterate(){
        counter ++;
        if (counter == maxCounter){ counter = 0;}
    }
}
