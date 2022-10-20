package cn.navclub.nes4j.bin.test.impl;

import cn.navclub.nes4j.bin.test.BaseTest;
import cn.navclub.nes4j.bin.util.ByteUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PPUTest extends BaseTest {
    @Test
    void test_load_ppu_data() {
        var rpg = new byte[]{
                (byte) 0xa9, 0x06,
                (byte) 0x8d, 0x06, 0x20,
                (byte) 0xa9, 0x00,
                (byte) 0x8d, 0x06, 0x20
        };
        var nes = this.createNES(rpg, new byte[]{}, null, 0x8000);
        var ppu = nes.getPpu();
        Assertions.assertEquals(ppu.getAddrVal(), 0x0006);
    }

    @Test
    void test_write_ppu_data() {
        var rpg = new byte[]{
                (byte) 0xa9, 0x7f,
                //STA
                (byte) 0x8d, 0x06, 0x20,
                (byte) 0xa9, 0x7f,
                //STA
                (byte) 0x8d, 0x06, 0x20,
                //STA
                (byte) 0x8d, 0x07, 0x20
        };
        var nes = this.createNES(rpg, new byte[]{}, null, 0x8000);
        var ppu = nes.getPpu();
    }
}
