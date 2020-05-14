package traffic.network;

import org.apache.commons.statistics.distribution.PoissonDistribution;

import java.util.Random;

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

    private Random rand;

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

        rand = new Random(5);

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
        if (true) {
            if (currentStep % 18000 < 15000) {
                numVehicles =  getPoissonRandom(10, rand);
            }
            else {
                numVehicles = 0; //demandMatrix[trafficType][counter];
            }

            if (currentStep%18000 == 0){
                rand = new Random(5);
            }

        }
        else
            numVehicles = 0;
        //System.out.println("Traffic: " + currentStep  + " " + demandMatrix[trafficType][0] + " and " +demandMatrix[trafficType][1]);
        iterate();
        return numVehicles;

    }


    public int getPoissonRandom(double mean, Random r) {
        double L = Math.exp(-mean);
        int k = 0;
        double p = 1.0;
        do {
            p = p * r.nextDouble();
            k++;
        } while (p > L);
        return k - 1;
    }

    private void iterate(){
        counter ++;
        if (counter == maxCounter){ counter = 0;}
    }

    public void selectTrafficType(int currentTime){
        //if (currentTime%freq == 0) {
        //    demandMatrix[trafficType][0] = getRandom().nextInt(36);
        //    demandMatrix[trafficType][1] = getRandom().nextInt(36);
        //}

        if ((currentTime/freq)%2 == 0) trafficType = 1;
        else trafficType = 0;
    }/**
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

        if ((currentTime/freq)%2 == 0) trafficType = 0;
        else trafficType = 1;
    }**/

}
