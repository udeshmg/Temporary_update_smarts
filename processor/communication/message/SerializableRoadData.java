package processor.communication.message;

import processor.communication.externalMessage.RoadControl;

/**
 * Class that sends road controls to the server
 */

public class SerializableRoadData {

    public int index ;
    public boolean laneIncreased;
    public boolean laneIncreasedCompeted;
    public int speed;

    public SerializableRoadData(){}

    public SerializableRoadData(int index, boolean laneIncreased, boolean laneIncreasedCompeted, int speed) {
        super();
        this.index = index;
        this.laneIncreased = laneIncreased;
        this.laneIncreasedCompeted = laneIncreasedCompeted;
        this.speed = speed;
    }

    public SerializableRoadData(RoadControl roadControl) {
        super();
        this.index = roadControl.index;
        this.laneIncreased = roadControl.laneChange;
        this.laneIncreasedCompeted = false;
        this.speed = roadControl.speed;
    }
}
