package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.core.SRegister;
import cn.navclub.nes4j.bin.enums.PControl;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * PPU控制寄存器
 *
 */
@Slf4j
public class ControlRegister extends SRegister {

    public ControlRegister() {
        this.bits = 0;
    }

    /**
     * 获取命名表地址
     */
    public int nameTableAddr() {
        return switch (this.bits & 0b11) {
            case 0 -> 0x2000;
            case 1 -> 0x2400;
            case 2 -> 0x2800;
            case 3 -> 0x2c00;
            default -> {
                log.debug("Name table not matching.");
                yield 0;
            }
        };
    }

    public int VRamIncrement() {
        return (!this.contain(PControl.VRAM_INCREMENT) ? 32 : 1);
    }

    public int spritePattern() {
        return this.contain(PControl.SPRITE_PATTERN_ADDR) ? 0x1000 : 0x000;
    }

    public int BPatternTable() {
        return this.contain(PControl.BACKGROUND_PATTERN_TABLE) ? 0x1000 : 0x000;
    }

    public int spriteSize() {
        return this.contain(PControl.SPRITE_SIZE) ? 0x0F : 0x08;
    }

    public int masterSlave() {
        return this.contain(PControl.MASTER_SLAVE) ? 1 : 0;
    }

    public boolean VBankNMI(){
        return this.contain(PControl.V_BANK_NMI);
    }

    public void update(byte bits){
        this.bits = bits;
    }
}
