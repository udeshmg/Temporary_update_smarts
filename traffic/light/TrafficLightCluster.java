package traffic.light;

import common.Settings;
import traffic.road.Edge;

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

/**
 * A cluster of lights consists of one or more phases.  At any time, only one of the phases
 * is in a green-yellow-red cycle. This phase is called the active phase. All other phases remain in red color.
 */
public class TrafficLightCluster {

    private List<Phase> phases;
    private int activePhase = -1;
    private int nextPhase = -1;
    private double timeForColor;
    private double spentTimeInColor;
    private Map<LightColor, Double> activePhaseSchedule;
    private Map<LightColor, Double> nextPhaseSchedule;

    public TrafficLightCluster(final List<Phase> phases) {
        this.phases = phases;
        activePhaseSchedule = new HashMap<>();
        nextPhaseSchedule = new HashMap<>();
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

    public double getTimeForColor() {
        return timeForColor;
    }

    public void setTimeForColor(double timeForColor) {
        this.timeForColor = timeForColor;
    }

    public void setNextPhase(int nextPhase) {
        this.nextPhase = nextPhase;
    }

    public int getNextPhase() {
        return nextPhase;
    }

    public int getActivePhase(){
        return activePhase;
    }

    public int getPhaseCount(){
        return phases.size();
    }

    public List<Edge> getActivePhaseEdges(){
        return phases.get(activePhase).getEdges();
    }

    public LightColor getActivePhaseColor(){
        return getActivePhaseEdges().get(0).lightColor;
    }

    public boolean hasToChangeColor(){
        return timeForColor <= spentTimeInColor;
    }

    public double getSpentTimeInColor() {
        return spentTimeInColor;
    }

    public void setActivePhaseSchedule(Map<LightColor, Double> activePhaseSchedule) {
        this.activePhaseSchedule.clear();
        this.activePhaseSchedule.putAll(activePhaseSchedule);
    }

    public void setNextPhaseSchedule(Map<LightColor, Double> nextPhaseSchedule) {
        this.nextPhaseSchedule.clear();
        this.nextPhaseSchedule.putAll(nextPhaseSchedule);
    }

    public void updateLights(){
        double secEachStep = 1 / Settings.numStepsPerSecond;
        spentTimeInColor += secEachStep;

        LightColor activePhaseColor = getActivePhaseColor();
        if(hasToChangeColor()) {
            if (activePhaseColor == LightColor.GYR_G) {
                setGYR(LightColor.GYR_Y);
            } else if (activePhaseColor == LightColor.GYR_Y) {
                setGYR(LightColor.GYR_R);
            } else if ((activePhaseColor == LightColor.GYR_R || activePhaseColor == LightColor.KEEP_RED)) {
                // Starts GYR cycle for next group of edges	(Switching Phase)
                activePhase = nextPhase;
                activePhaseSchedule.putAll(nextPhaseSchedule);
                nextPhase = -1;
                nextPhaseSchedule.clear();
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
        timeForColor = activePhaseSchedule.get(type);
    }

    public int getInactivePhaseWithPriorityVehicles() {
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

    boolean hasActivePhasePriorityVehicles() {
        for (final Edge e : getActivePhaseEdges()) {
            if (e.isEdgeContainsPriorityVehicle()) {
                return true;
            }
        }
        return false;
    }

    boolean hasInactivePhasePriorityVehicles() {
        for (int i = 0; i < phases.size(); i++) {
            for (final Edge e : getPhase(i).getEdges()) {
                if (e.isEdgeContainsPriorityVehicle()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasActivePhaseTraffic() {
        for (final Edge e : getActivePhaseEdges()) {
            if (e.isDetectedVehicleForLight) {
                return true;
            }
        }
        return false;
    }

    boolean hasInactivePhaseTraffic() {
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

}
