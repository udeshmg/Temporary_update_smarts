package traffic.light.schedule;

import traffic.TrafficNetwork;
import traffic.light.*;
import traffic.light.phase.TLPhaseHandler;

import java.util.List;
import java.util.TreeMap;

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
 * Created by tmuthugama on 8/22/2019
 */
public class FixedSchedule extends TLScheduleHandler{

    private TreeMap<LightColor, Double> fixedPeriods;

    public FixedSchedule(){
        fixedPeriods = new TreeMap<>();
        fixedPeriods.put(LightColor.GYR_G, 30.0);
        fixedPeriods.put(LightColor.GYR_Y, 10.0);
        fixedPeriods.put(LightColor.GYR_R, 5.0);
    }

    @Override
    public void update(TrafficNetwork trafficNetwork, List<TrafficLightCluster> clusters, TLPhaseHandler phaseHandler, double horizon, double timeNow) {
        for (TrafficLightCluster cluster : clusters) {
            TLSchedule existing = cluster.getLightSchedule();
            List<Phase> phases = phaseHandler.getPhaseList(cluster, timeNow);
            updateSchedule(existing, phases, timeNow, horizon);
        }
    }

    public void updateSchedule(TLSchedule existing, List<Phase> phases, double timeNow, double horizon){
        LightPeriod end = existing.getEndPeriod();
        double scheduleRemainder = 0;
        if (end != null) {
            scheduleRemainder = end.getEnd() - timeNow;
        }
        while (horizon - scheduleRemainder > 0) {
            Phase phase;
            double t;
            if (end != null) {
                int i = phases.indexOf(end.getPhase());
                phase = phases.get((i + 1) % phases.size());
                t = end.getEnd();
            } else {
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
}
