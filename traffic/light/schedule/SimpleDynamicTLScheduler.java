package traffic.light.schedule;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.light.LightColor;
import traffic.light.TrafficLightCluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2019, The University of Melbourne.
 * All rights reserved.
 * <p>
 * You are permitted to use the code for research purposes provided that the following conditions are met:
 * <p>
 * * You cannot redistribute any part of the code.
 * * You must give appropriate citations in any derived work.
 * * You cannot use any part of the code for commercial purposes.
 * * The copyright holder reserves the right to change the conditions at any time without prior notice.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * Created by tmuthugama on 8/9/2019
 */
public class SimpleDynamicTLScheduler extends TLScheduler{

    private Map<LightColor, Double> schedule;
    private double maxGreenTime;

    public SimpleDynamicTLScheduler(){
        schedule = new HashMap<>();
        schedule.put(LightColor.GYR_G, 10.0);
        schedule.put(LightColor.GYR_Y, 10.0);
        schedule.put(LightColor.GYR_R, 5.0);
        schedule.put(LightColor.KEEP_RED, 0.0);
        maxGreenTime = 180.0;
    }

    @Override
    public void init(List<TrafficLightCluster> clusters) {
        super.init(clusters);
        for (TrafficLightCluster cluster : getClusters()) {
            if(cluster.getActivePhase() == -1){
                cluster.setActivePhase(0);
                cluster.setActivePhaseSchedule(schedule);
                cluster.setGYR(LightColor.GYR_G);
            }
        }
    }

    @Override
    public void schedule(TrafficNetwork trafficNetwork, double timeNow) {
        double secEachStep = 1 / Settings.numStepsPerSecond;

        for (TrafficLightCluster cluster : getClusters()) {
            LightColor activePhaseColor = cluster.getActivePhaseColor();
//            if (cluster.hasInactivePhasePriorityVehicles() && !cluster.hasActivePhasePriorityVehicles()) {
//                // Grant green light to an inactive approach it has priority vehicle and the current active approach does not have one
//                cluster.setActivePhase(cluster.getInactivePhaseWithPriorityVehicles());
//                setGYR(LightColor.GYR_G);
//            }
//            if (!cluster.hasInactivePhasePriorityVehicles() && cluster.hasActivePhasePriorityVehicles()) {
//                // Grant green light to current active approach if it has a priority vehicle and inactive approaches do not have priority vehicle
//                setGYR(LightColor.GYR_G);
//            }
            double spentTime = cluster.getSpentTimeInColor() + secEachStep;
            if (activePhaseColor == LightColor.GYR_G && spentTime >= cluster.getTimeForColor()) {
                if(cluster.hasActivePhaseTraffic() && spentTime < maxGreenTime){
                    cluster.setTimeForColor(cluster.getTimeForColor() + secEachStep);
                }
            }
            if(cluster.getNextPhase() == -1){
                cluster.setNextPhase((cluster.getActivePhase() + 1) % cluster.getPhaseCount());
                cluster.setNextPhaseSchedule(schedule);
            }
        }
    }


}
