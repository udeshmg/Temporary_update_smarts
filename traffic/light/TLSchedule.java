package traffic.light;

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
 * Created by tmuthugama on 8/20/2019
 */
public class TLSchedule {

    private TreeMap<Long, LightPeriod> schedule;

    public TLSchedule() {
        this.schedule = new TreeMap<>();
    }

    public LightColor getLight(Movement movement, double time){
        Map.Entry<Long, LightPeriod> e = schedule.firstEntry();
        Long index = e.getKey();
        LightPeriod period = e.getValue();
        if(period.getEnd() < time){
            period = schedule.get(index + 1);
            schedule.remove(index);
        }
        if(period.getPhase().hasMovement(movement)){
            return period.getColor();
        }else{
            return LightColor.KEEP_RED;
        }
    }

    public LightPeriod getCurrentPeriod(){
        if(!schedule.isEmpty()) {
            return schedule.firstEntry().getValue();
        }
        return null;
    }

    public LightPeriod getEndPeriod(){
        if(!schedule.isEmpty()) {
            return schedule.lastEntry().getValue();
        }
        return null;
    }

    public void addLightPeriod(LightPeriod period){
        if(schedule.isEmpty()) {
            period.setId((long) 0);
            schedule.put(period.getId(), period);
        }else{
            period.setId(schedule.lastKey() + 1);
            schedule.put(period.getId(), period);
        }
    }

    public void extendDuration(LightPeriod period, double delta){
        period.addDur(delta);
        for (Long key : schedule.keySet()) {
            if(key > period.getId()){
                schedule.get(key).shift(delta);
            }
        }
    }

    public List<LightPeriod> getGreenPeriods(Movement movement){
        List<LightPeriod> periods = new ArrayList<>();
        for (LightPeriod lightPeriod : schedule.values()) {
            if(lightPeriod.getColor() == LightColor.GYR_G && lightPeriod.getPhase().hasMovement(movement)){
                periods.add(lightPeriod);
            }
        }
        return periods;
    }
}
