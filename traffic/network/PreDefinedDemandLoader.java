package traffic.network;

public class PreDefinedDemandLoader extends TemporalDistributor {

    private int counter = 0;
    private int maxCounter = 2;
    private int [][] demandMatrix = {{20,2,20,20,20,20,20,20,20,20,2,38,2,38,2,38,2,38,2,38},
                                     {20,20,18,1,16,1,12,1},
                                     {1,15,1,18,1,16,1,12},
                                     {1,4,3,9,14,3,3},
                                     {5,12,12,9,6,12}};
    private int trafficType = 0;

    public PreDefinedDemandLoader(int maxCounter) {
        this.maxCounter = maxCounter;
    }

    @Override
    public int getCurrentVehicleLimit(int amount, int currentStep, int maxStep) {

        selectTrafficType(currentStep);
        int numVehicles = 0;
        if (currentStep < 8000)
            numVehicles =  demandMatrix[trafficType][counter];
        else
            numVehicles = 0;

        iterate();
        return numVehicles;

    }

    private void iterate(){
        counter ++;
        if (counter == maxCounter){ counter = 0;}
    }

    private void selectTrafficType(int currentTime){
        switch (currentTime/2000){
            case 0:
                trafficType = 0;
                break;
            case 1:
                trafficType = 1;
                break;
            case 2:
                trafficType = 2;
                break;
            case 3:
                trafficType = 3;
                break;
            default:
                trafficType = 3;
        }
    }
}
