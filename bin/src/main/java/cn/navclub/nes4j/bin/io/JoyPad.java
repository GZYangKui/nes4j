package cn.navclub.nes4j.bin.io;

import cn.navclub.nes4j.bin.util.ByteUtil;

/**
 * <a href="https://www.nesdev.org/wiki/Standard_controller"> Standard controller</a>
 */
public class JoyPad {
    private int index;
    private byte bits;
    //Record if high strobe
    private boolean strobe;

    public void write(byte b) {
        this.strobe = (b & 1) == 1;
        if (this.strobe) {
            this.index = 0;
        }
    }

    public byte read() {
        if (this.index > 7) {
            return 1;
        }
        var b = ((this.bits & 0xff) & (1 << this.index)) >> this.index;
        if (!this.strobe) {
            this.index++;
        }
        return (byte) b;
    }

    public void updateBtnStatus(JoypadButton action, boolean press) {
        if (press) {
            this.bits |= (1 << action.ordinal());
        } else {
            this.bits &= (0xff - (int) (Math.pow(2, action.ordinal())));
        }
    }

    @Override
    public String toString() {
        return ByteUtil.toBinStr(this.bits);
    }

    public enum JoypadButton {
        //A
        BTN_A,
        //B
        BTN_B,
        //Select
        BTN_SE,
        //Start
        BTN_ST,
        //UP
        BTN_UP,
        //Down
        BTN_DN,
        //Left
        BTN_LF,
        //Right
        BTN_RT;

        @Override
        public String toString() {
            return switch (this) {
                case BTN_A -> "A";
                case BTN_B -> "B";
                case BTN_DN -> "Down";
                case BTN_LF -> "Left";
                case BTN_RT -> "Right";
                case BTN_SE -> "Select";
                case BTN_ST -> "Start";
                case BTN_UP -> "Up";
            };
        }
    }

}
