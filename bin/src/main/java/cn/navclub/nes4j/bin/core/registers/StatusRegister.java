package cn.navclub.nes4j.bin.core.registers;

import cn.navclub.nes4j.bin.enums.PStatus;

/**
 * PPU状态寄存器
 */
public class StatusRegister {
    private int bits;

    public StatusRegister() {
        this.bits = 0;
    }

    public void updateVBlank(boolean set) {
        this.update(PStatus.V_BLANK_OCCUR, set);
    }

    private void update(PStatus status, boolean set) {
        var index = status.ordinal();
        if (set) {
            this.bits |= (1 << index);
        } else {
            this.bits &= (0xff - (int) (Math.pow(2, index)));
        }
    }
}
