package cn.whiteg.bnes.render;

import com.grapeshot.halfnes.ui.ControllerInterface;

public class PlayerController implements ControllerInterface {
    private int latchByte = 0;
    private int controllerByte = 0;
    private int outByte = 0;

    public PlayerController() {
    }

    public void strobe() {
        this.outByte = this.latchByte & 1;
        this.latchByte = this.latchByte >> 1 | 256;
    }

    public void output(boolean var1) {
        this.latchByte = this.controllerByte;
    }

    public int peekOutput() {
        return this.latchByte;
    }

    public int getbyte() {
        return this.outByte;
    }

    public void resetButtons() {
        this.controllerByte = 0;
    }

    public void releaseButton(Button button) {
        this.controllerByte &= ~button.digit;
    }

    public void pressButton(Button button) {
        if (button != null) this.controllerByte |= button.digit;
    }

    public boolean statusButton(Button button) {
        return (this.controllerByte & button.digit) != 0;
    }

    public enum Button {
        A(1),
        B(2),
        SELECT(4),
        START(8),
        UP(16),
        DOWN(32),
        LEFT(64),
        RIGHT(128);

        public final int digit;

        Button(int digit) {
            this.digit = digit;
        }
    }
}
