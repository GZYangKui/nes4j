package cn.navclub.nes4j.bin.core.registers;

import cn.navclub.nes4j.bin.enums.PStatus;
import lombok.Getter;

/**
 * PPU状态寄存器
 */
@Getter
public class StatusRegister {
    private byte bits;

    public StatusRegister() {
        this.bits = 0;
    }

    public void vbank(boolean set) {
        this.update(PStatus.V_BLANK_OCCUR, set);
    }

    public void sprintCount(boolean set) {
        this.update(PStatus.SPRITE_COUNT, set);
    }

    public void spriteZeroHit(boolean set) {
        this.update(PStatus.SPRITE_ZERO_HIT, set);
    }

    public void vramDWrite(boolean set) {
        this.update(PStatus.VRAM_WRITE_DISABLE, set);
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
