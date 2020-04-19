package traffic.network;

public class PreDefinedDemandLoader extends TemporalDistributor {

    private int counter = 0;
    private int maxCounter = 2;
    private int demand = 20;
    private int freq = 4000;
    private int [][] demandMatrix;
    public int trafficType = 0;

    public int getFreq() {
        return freq;
    }

    public void setFreq(int freq) {
        this.freq = freq;
    }

    public PreDefinedDemandLoader(int maxCounter) {
        this.maxCounter = maxCounter;
    }
    public PreDefinedDemandLoader(int maxCounter, int freq) {
        this.maxCounter = maxCounter;
        this.freq = freq;
    }
    public PreDefinedDemandLoader(int maxCounter, int freq, int demand) {
        this.maxCounter = maxCounter;
        this.freq = freq;
        this.demand = demand;

        demandMatrix = new int[][]{{demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2},
            {2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand,2,demand},
            {15,1,18,1,16,1,12,1},
            {1,15,1,18,1,16,1,12},
            {1,4,3,9,14,3,3},
            {5,12,12,9,6,12}};

    }
    @Override
    public int getCurrentVehicleLimit(int amount, int currentStep, int maxStep) {

        selectTrafficType(currentStep);
        int numVehicles = 0;
        if (currentStep < 10000) {
            if (currentStep % freq < 500) {
                numVehicles = 0;
            }
            else {
                numVehicles = demandMatrix[trafficType][counter];
            }
        }
        else
            numVehicles = 0;

        iterate();
        return numVehicles;

    }

    private void iterate(){
        counter ++;
        if (counter == maxCounter){ counter = 0;}
    }

    public void selectTrafficType(int currentTime){

        if ((currentTime/freq)%2 == 0) trafficType = 1;
        else trafficType = 1;
    }

}
