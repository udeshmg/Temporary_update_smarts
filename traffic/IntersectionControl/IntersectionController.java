package traffic.IntersectionControl;

import traffic.road.Node;

public abstract class IntersectionController {

    protected Node node = null;
    protected boolean isTriggered = false;
    protected double controlRegion;
    public abstract void computeSchedule(double timeNow);
    public void notifyController(){
        isTriggered = true;
    };

    public double getControlRegion() {
        return controlRegion;
    }

    public void setControlRegion(double controlRegion) {
        this.controlRegion = controlRegion;
    }
}
