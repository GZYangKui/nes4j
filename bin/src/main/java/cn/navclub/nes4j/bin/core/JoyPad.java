package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.enums.ButtonAction;

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
        var b = this.bits & (1 << this.index) >> this.index;
        if (!this.strobe) {
            this.index++;
        }
        return (byte) b;
    }

    public void updateBtnStatus(ButtonAction action, boolean press) {
        if (press) {
            this.bits |= (1 << action.ordinal());
        } else {
            this.bits &= (0xff - (int) (Math.pow(2, action.ordinal())));
        }
    }

}
