package traffic.light;

import common.Settings;
import traffic.TrafficNetwork;

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
public class SimpleFixedTLScheduler extends TLScheduler{

    private Map<LightColor, Double> schedule;

    public SimpleFixedTLScheduler(){
        schedule = new HashMap<>();
        schedule.put(LightColor.GYR_G, 30.0);
        schedule.put(LightColor.GYR_Y, 10.0);
        schedule.put(LightColor.GYR_R, 5.0);
        schedule.put(LightColor.KEEP_RED, 0.0);
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
        for (TrafficLightCluster cluster : getClusters()) {
            if(cluster.getNextPhase() == -1){
                cluster.setNextPhase((cluster.getActivePhase() + 1) % cluster.getPhaseCount());
                cluster.setNextPhaseSchedule(schedule);
            }
        }
    }
}
