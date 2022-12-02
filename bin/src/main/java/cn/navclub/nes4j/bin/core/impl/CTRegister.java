package cn.navclub.nes4j.bin.core.impl;

import cn.navclub.nes4j.bin.core.SRegister;
import cn.navclub.nes4j.bin.enums.NameTMirror;
import cn.navclub.nes4j.bin.enums.PControl;
import lombok.extern.slf4j.Slf4j;

/**
 * PPU控制寄存器
 *
 * <code>
 * 7  bit  0
 * ---- ----
 * VPHB SINN
 * |||| ||||
 * |||| ||++- Base nametable address
 * |||| ||    (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)
 * |||| |+--- VRAM address increment per CPU read/write of PPUDATA
 * |||| |     (0: add 1, going across; 1: add 32, going down)
 * |||| +---- Sprite pattern table address for 8x8 sprites
 * ||||       (0: $0000; 1: $1000; ignored in 8x16 mode)
 * |||+------ Background pattern table address (0: $0000; 1: $1000)
 * ||+------- Sprite size (0: 8x8 pixels; 1: 8x16 pixels – see PPU OAM#Byte 1)
 * |+-------- PPU master/slave select
 * |          (0: read backdrop from EXT pins; 1: output color on EXT pins)
 * +--------- Generate an NMI at the start of the
 * vertical blanking interval (0: off; 1: on)
 * </code>
 */
@Slf4j
public class CTRegister extends SRegister {

    public CTRegister() {
        this.bits = 0;
    }

    /**
     * Get current name table address
     */
    public NameTMirror nameTableAddr() {
        return NameTMirror.values()[this.bits & 0x03];
    }

    public int VRamIncrement() {
        return this.contain(PControl.VRAM_INCREMENT) ? 32 : 1;
    }

    public int spritePattern8() {
        return this.contain(PControl.SPRITE_PATTERN_ADDR) ? 0x1000 : 0x000;
    }

    public int spritePattern16(int index) {
        //
        //
        // 8x16 sprites use different pattern tables based on their index number. If the index number is
        // even the sprite data is in the first pattern table at $0000, otherwise it is in the second pattern
        // table at $1000.
        //
        return (index & 0x01) == 0 ? 0x0000 : 0x1000;
    }

    public int bkNamePatternTable() {
        return this.contain(PControl.BKG_PATTERN_TABLE) ? 0x1000 : 0x000;
    }

    public int spriteSize() {
        return this.contain(PControl.SPRITE_SIZE) ? 0x10 : 0x08;
    }

    public boolean generateVBlankNMI() {
        return this.contain(PControl.V_BANK_NMI);
    }

    public void update(byte bits) {
        this.bits = bits;
    }
}
