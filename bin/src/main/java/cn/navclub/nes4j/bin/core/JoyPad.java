package cn.navclub.nes4j.bin.core;

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
        if (((this.bits & 0xff) & 0b1000) != 0) {
            System.out.println("选中按下");
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
        //b
        BTN_A,
        //a
        BTN_B,
        //select
        BTN_SE,
        //start
        BTN_ST,
        //up
        BTN_UP,
        //down
        BTN_DN,
        //left
        BTN_LF,
        //right
        BTN_RT
    }

}
