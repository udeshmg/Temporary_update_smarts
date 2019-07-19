package traffic.network;

import common.Settings;
import traffic.TrafficNetwork;
import traffic.road.Edge;
import traffic.road.RoadNetwork;

import java.util.ArrayList;
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
 * Created by tmuthugama on 6/18/2019
 */
public class RandomODDistributor extends ODDistributor{



    @Override
    public Edge getStartEdge(TrafficNetwork trafficNetwork, List<Edge> possibleStartEdges) {
        return possibleStartEdges.get(getRandom().nextInt(possibleStartEdges.size()));
    }

    @Override
    public Edge getEndEdge(TrafficNetwork trafficNetwork, List<Edge> possibleEndEdges) {
        return possibleEndEdges.get(getRandom().nextInt(possibleEndEdges.size()));
    }

    @Override
    public List<double[]> getSourceWidows(RoadNetwork network) {
        return Settings.guiSourceWindowsForInternalVehicle;
    }

    @Override
    public List<double[]> getDestinationWidows(RoadNetwork network) {
        return Settings.guiDestinationWindowsForInternalVehicle;
    }

    @Override
    public List<double[]> getSourceDestinationWidows(RoadNetwork network) {
        return Settings.guiSourceDestinationWindowsForInternalVehicle;
    }
}
