package traffic.light;

import common.Settings;
import traffic.road.Edge;

import java.util.List;

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

/**
 * A cluster of lights consists of one or more phases.  At any time, only one of the phases
 * is in a green-yellow-red cycle. This phase is called the active phase. All other phases remain in red color.
 */
public class TrafficLightCluster {

    public List<Phase> phases;
    /**
     * This identifies the active street, i.e., a street in green-yellow-red
     * cycle. Non-active streets always get red lights.
     */
    public int phaseIndex;
    double trafficSignalTimerGYR;
    double trafficSignalAccumulatedGYRTime;

    public TrafficLightCluster(final List<Phase> phases) {
        this.phases = phases;
    }

    public Phase getPhase(int phaseIndex){
        return phases.get(phaseIndex);
    }

    public Phase getActivePhase(){
        return phases.get(phaseIndex);
    }



    public int getEdgeGroupIndexOfPriorityInactiveApproach() {
        for (int i = 0; i < phases.size(); i++) {
            if (i == phaseIndex) {
                continue;
            }
            for (Edge e : getPhase(i).getEdges()) {
                if (e.isEdgeContainsPriorityVehicle()) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Check whether the current active approach has a priority vehicle.
     */
    boolean isPriorityVehicleInActiveApproach() {
        for (final Edge e : getActivePhase().getEdges()) {
            if (e.isEdgeContainsPriorityVehicle()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether inactive approaches have a priority vehicle.
     */
    boolean isPriorityVehicleInInactiveApproach() {
        for (int i = 0; i < phases.size(); i++) {
            for (final Edge e : getPhase(i).getEdges()) {
                if (e.isEdgeContainsPriorityVehicle()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check whether there is vehicle coming to the current approach under
     * active control.
     */
    boolean isTrafficExistAtActiveStreet() {
        for (final Edge e : getActivePhase().getEdges()) {
            if (e.isDetectedVehicleForLight) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether there is incoming vehicle at conflicting approaches.
     */
    boolean isTrafficExistAtNonActiveStreet() {
        for (int i = 0; i < phases.size(); i++) {
            if (i == phaseIndex) {
                continue;
            }
            for (final Edge e : getPhase(i).getEdges()) {
                if (e.isDetectedVehicleForLight) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reset timer of all light groups.
     *
     */
    public void resetGYR() {
        for (final Phase phase : phases) {
            for (final Edge edge : phase.getEdges()) {
                edge.lightColor = LightColor.GYR_G;
            }
        }
        trafficSignalTimerGYR = LightColor.GYR_G.minDynamicTime;
        trafficSignalAccumulatedGYRTime = 0;
    }

    /**
     * Set the color of an active street and initialize the timer for the color.
     * Non-active streets get red lights.
     */
    public void setGYR(final LightColor type) {
        for (int i = 0; i < phases.size(); i++) {
            if (i == phaseIndex) {
                for (final Edge edge : getPhase(i).getEdges()) {
                    edge.lightColor = type;
                }
            } else {
                for (final Edge edge : getPhase(i).getEdges()) {
                    edge.lightColor = LightColor.KEEP_RED;
                }
            }
        }
        trafficSignalAccumulatedGYRTime = 0;
        if (Settings.trafficLightTiming == TrafficLightTiming.DYNAMIC) {
            trafficSignalTimerGYR = type.minDynamicTime;
        } else if (Settings.trafficLightTiming == TrafficLightTiming.FIXED) {
            trafficSignalTimerGYR = type.fixedTime;
        }
    }

}
