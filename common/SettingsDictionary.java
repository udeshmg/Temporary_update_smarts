package common;

import processor.SimulationListener;
import traffic.network.ODDistributor;
import traffic.network.RandomODDistributor;
import traffic.network.TemporalDistributor;
import traffic.network.UniformTemporalDistributor;
import traffic.routing.Routing;

import java.util.HashMap;
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
 * Created by tmuthugama on 7/7/2019
 */
public class SettingsDictionary {

    private Map<String, SimulationListener> listenerMap;
    private Map<String, ODDistributor> odDistributorMap;
    private Map<String, TemporalDistributor> temporalDistributorMap;
    private Map<String, Routing> routingAlgoMap;

    public SettingsDictionary() {
        this.listenerMap = new HashMap<>();
        this.odDistributorMap = new HashMap<>();
        addODDistributor("Random", new RandomODDistributor());
        this.temporalDistributorMap = new HashMap<>();
        addTemporalDistributor("Uniform", new UniformTemporalDistributor());
        this.routingAlgoMap = new HashMap<>();
    }

    public void addSimulationListener(String key, SimulationListener listener){
        listenerMap.put(key, listener);
    }

    public void addODDistributor(String key, ODDistributor odDistributor){
        odDistributorMap.put(key, odDistributor);
    }

    public void addTemporalDistributor(String key, TemporalDistributor temporalDistributor){
        temporalDistributorMap.put(key, temporalDistributor);
    }

    public void addRoutingAlgorithm(String key, Routing algorithm){
        routingAlgoMap.put(key, algorithm);
    }

    public SimulationListener getSimulationListener(String key){
        return listenerMap.get(key);
    }

    public ODDistributor getODDistributor(String key){
        return odDistributorMap.get(key);
    }

    public TemporalDistributor getTemporalDistributor(String key){
        return temporalDistributorMap.get(key);
    }
}
