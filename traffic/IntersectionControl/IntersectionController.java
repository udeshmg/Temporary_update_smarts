package traffic.IntersectionControl;

import traffic.road.Node;

public abstract class IntersectionController {

    protected Node node = null;
    protected double controlRegion;
    public abstract void computeSchedule(double timeNow);

}
