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

    private List<Phase> phases;
    private int activePhase;
    double timeForColor;
    double spentTimeInColor;

    public TrafficLightCluster(final List<Phase> phases) {
        this.phases = phases;
    }

    public List<Phase> getPhases() {
        return phases;
    }

    public Phase getPhase(int phaseIndex){
        return phases.get(phaseIndex);
    }

    public void setActivePhase(int activePhase) {
        this.activePhase = activePhase;
    }

    public Phase getActivePhase(){
        return phases.get(activePhase);
    }

    public LightColor getActivePhaseColor(){
        return getActivePhase().getEdges().get(0).lightColor;
    }

    public void updateLights(){
        double secEachStep = 1 / Settings.numStepsPerSecond;
        spentTimeInColor += secEachStep;

        LightColor activePhaseColor = getActivePhaseColor();
        if (isPriorityVehicleInInactiveApproach() && !isPriorityVehicleInActiveApproach()) {
            // Grant green light to an inactive approach it has priority vehicle and the current active approach does not have one
            activePhase = getEdgeGroupIndexOfPriorityInactiveApproach();
            setGYR(LightColor.GYR_G);
        }
        if (!isPriorityVehicleInInactiveApproach() && isPriorityVehicleInActiveApproach()) {
            // Grant green light to current active approach if it has a priority vehicle and inactive approaches do not have priority vehicle
            setGYR(LightColor.GYR_G);
        }

        if(activePhaseColor == LightColor.GYR_G){
            if (Settings.trafficLightTiming == TrafficLightTiming.DYNAMIC) {
                if (!isTrafficExistAtNonActiveStreet()) {
                    timeForColor += secEachStep;
                } else if (timeForColor <= spentTimeInColor) {
                    // Switch to yellow if traffic waiting at conflicting approach
                    if(isTrafficExistAtActiveStreet() && spentTimeInColor < LightColor.GYR_G.maxDynamicTime){
                        timeForColor += secEachStep;
                    }
                }
            }
        }

        if(timeForColor <= spentTimeInColor) {
            if (activePhaseColor == LightColor.GYR_G) {
                setGYR(LightColor.GYR_Y);
            } else if (activePhaseColor == LightColor.GYR_Y) {
                setGYR(LightColor.GYR_R);
            } else if ((activePhaseColor == LightColor.GYR_R || activePhaseColor == LightColor.KEEP_RED)) {
                // Starts GYR cycle for next group of edges	(Switching Phase)
                activePhase = (activePhase + 1) % phases.size();
                setGYR(LightColor.GYR_G);
            }
        }

        // Reset vehicle detection flag at all edges
        for (Phase phase : phases) {
            for (final Edge edge : phase.getEdges()) {
                edge.isDetectedVehicleForLight = false;
            }
        }
    }


    public int getEdgeGroupIndexOfPriorityInactiveApproach() {
        for (int i = 0; i < phases.size(); i++) {
            if (i == activePhase) {
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
            if (i == activePhase) {
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
     * Set the color of an active street and initialize the timer for the color.
     * Non-active streets get red lights.
     */
    public void setGYR(final LightColor type) {
        for (int i = 0; i < phases.size(); i++) {
            if (i == activePhase) {
                for (final Edge edge : getPhase(i).getEdges()) {
                    edge.lightColor = type;
                }
            } else {
                for (final Edge edge : getPhase(i).getEdges()) {
                    edge.lightColor = LightColor.KEEP_RED;
                }
            }
        }
        spentTimeInColor = 0;
        if (Settings.trafficLightTiming == TrafficLightTiming.DYNAMIC) {
            timeForColor = type.minDynamicTime;
        } else if (Settings.trafficLightTiming == TrafficLightTiming.FIXED) {
            timeForColor = type.fixedTime;
        }
    }

}
