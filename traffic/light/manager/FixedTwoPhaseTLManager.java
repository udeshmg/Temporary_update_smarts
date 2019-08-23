package traffic.light.manager;

import traffic.TrafficNetwork;
import traffic.light.LightColor;
import traffic.light.Phase;
import traffic.light.TrafficLightCluster;
import traffic.light.phase.FixedDefaultFourPhase;
import traffic.light.phase.FixedDefaultTwoPhase;
import traffic.light.phase.TLPhaseHandler;
import traffic.light.LightPeriod;
import traffic.light.TLSchedule;
import traffic.light.schedule.FixedSchedule;
import traffic.light.schedule.TLScheduleHandler;

import java.util.*;

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
public class FixedTwoPhaseTLManager extends TLManager {


    @Override
    public void setHandlers() {
        setHorizon(90);
//        setPhaseHandler(new FixedDefaultTwoPhase(getClusters()));
        setPhaseHandler(new FixedDefaultFourPhase(getClusters()));
        setScheduleHandler(new FixedSchedule());
    }
}
