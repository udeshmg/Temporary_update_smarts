package processor.communication.externalMessage;

public class VehicleControl {

    private int index = 0;
    private int paddleCommand = 0;

    /**
     * Add more controls here.
     */

    public int getPaddleCommand() {
        return paddleCommand;
    }

    public void setPaddleCommand(int paddleCommand) {
        this.paddleCommand = paddleCommand;
    }


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

}
