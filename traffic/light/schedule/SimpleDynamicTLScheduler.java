package traffic.light.schedule;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.light.LightColor;
import traffic.light.Movement;
import traffic.light.Phase;
import traffic.light.TrafficLightCluster;
import traffic.road.Edge;

import java.util.ArrayList;
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

    private Map<LightColor, Double> fixedPeriods;
    private double horizon = 90;
    private Map<TrafficLightCluster, List<Phase>> fixedPhases;
    private double maxGreenTime;

    public SimpleDynamicTLScheduler(){
        fixedPeriods = new HashMap<>();
        fixedPeriods.put(LightColor.GYR_G, 10.0);
        fixedPeriods.put(LightColor.GYR_Y, 10.0);
        fixedPeriods.put(LightColor.GYR_R, 5.0);
        fixedPeriods.put(LightColor.KEEP_RED, 0.0);
        fixedPhases = new HashMap<>();
        maxGreenTime = 180.0;
    }

    @Override
    public void init(List<TrafficLightCluster> clusters) {
        super.init(clusters);
        for (TrafficLightCluster cluster : getClusters()) {
            Map<String, Phase> groups = new HashMap<>();
            for (Movement movement : cluster.getMovements()) {
                String groupName = movement.getControlEdge().name;
                if(!groups.containsKey(groupName)){
                    groups.put(groupName, new Phase());
                }
                groups.get(groupName).addMovement(movement);
            }
            fixedPhases.put(cluster, new ArrayList<>(groups.values()));

            updateSchedule(cluster,  0);
            cluster.updateLights(0);
        }
    }

    @Override
    public void schedule(TrafficNetwork trafficNetwork, double timeNow) {

        for (TrafficLightCluster cluster : getClusters()) {
//            if (cluster.hasInactivePhasePriorityVehicles() && !cluster.hasActivePhasePriorityVehicles()) {
//                // Grant green light to an inactive approach it has priority vehicle and the current active approach does not have one
//                cluster.setActivePhase(cluster.getInactivePhaseWithPriorityVehicles());
//                setGYR(LightColor.GYR_G);
//            }
//            if (!cluster.hasInactivePhasePriorityVehicles() && cluster.hasActivePhasePriorityVehicles()) {
//                // Grant green light to current active approach if it has a priority vehicle and inactive approaches do not have priority vehicle
//                setGYR(LightColor.GYR_G);
//            }
            extendPhase(cluster, timeNow);
            updateSchedule(cluster, timeNow);

        }
    }

    public void extendPhase(TrafficLightCluster cluster, double timeNow){
        TLSchedule schedule = cluster.getLightSchedule();
        LightPeriod current = schedule.getCurrentPeriod();
        if(current != null){
            if(current.getColor() == LightColor.GYR_G && timeNow >= current.getEnd()){
                if(hasActivePhaseTraffic(current.getPhase()) && current.getDur() < maxGreenTime){
                    schedule.extendDuration(current, 1);
                }
            }
        }
        // Reset vehicle detection flag at all edges
        for (Phase phase : fixedPhases.get(cluster)) {
            for (Movement m : phase.getMovements()) {
                m.getControlEdge().isDetectedVehicleForLight = false;
            }
        }
    }

    public void updateSchedule(TrafficLightCluster cluster, double timeNow){
        TLSchedule existing = cluster.getLightSchedule();
        LightPeriod end = existing.getEndPeriod();
        List<Phase> phases = fixedPhases.get(cluster);
        double scheduleRemainder = 0;
        if(end != null){
            scheduleRemainder = end.getEnd() - timeNow;
        }
        while (horizon - scheduleRemainder > 0){
            Phase phase;
            double t;
            if(end != null){
                int i = phases.indexOf(end.getPhase());
                phase = phases.get((i + 1) % phases.size());
                t = end.getEnd();
            }else{
                phase = phases.get(0);
                t = 0;
            }
            for (LightColor color : fixedPeriods.keySet()) {
                double dur = fixedPeriods.get(color);
                existing.addLightPeriod(new LightPeriod(phase, color, t, t + dur));
                t = t + dur;
            }
            end = existing.getEndPeriod();
            scheduleRemainder = end.getEnd() - timeNow;
        }
    }


    public boolean hasActivePhaseTraffic(Phase phase) {
        for (Movement m : phase.getMovements()) {
            if (m.getControlEdge().isDetectedVehicleForLight) {
                return true;
            }
        }
        return false;
    }

    boolean hasInactivePhaseTraffic(Phase active, List<Phase> phases) {
        int activePhase = phases.indexOf(active);
        for (int i = 0; i < phases.size(); i++) {
            if (i == activePhase) {
                continue;
            }
            for (Movement m : phases.get(i).getMovements()) {
                if (m.getControlEdge().isDetectedVehicleForLight) {
                    return true;
                }
            }
        }
        return false;
    }


}
