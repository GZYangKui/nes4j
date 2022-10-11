package cn.navclub.nes4j.bin.test.impl;

import cn.navclub.nes4j.bin.core.CPU;
import cn.navclub.nes4j.bin.core.registers.CSRegister;
import cn.navclub.nes4j.bin.test.BaseTest;
import cn.navclub.nes4j.bin.util.ByteUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;



public class CPUTest extends BaseTest {
    @Test
    void lad() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 0x11,
        };
        var cpu = createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0x11);
    }

    @Test
    void test_0x69_adc() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 1,
                ByteUtil.overflow(0x69), 10,
        };
        var cpu = createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 11);
    }

    @Test
    void test_0x69_adc_carry_zero_flag() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), ByteUtil.overflow(0x81),
                ByteUtil.overflow(0x69), 0x7f
        };
        var cpu = createInstance(rpg);
        var status = cpu.getStatus();
        Assertions.assertEquals(cpu.getRa(), 0x00);
        Assertions.assertTrue(status.hasFlag(CSRegister.BIFlag.ZERO_FLAG));
        Assertions.assertTrue(status.hasFlag(CSRegister.BIFlag.CARRY_FLAG));
        Assertions.assertFalse(status.hasFlag(CSRegister.BIFlag.OVERFLOW_FLAG));
    }

    @Test
    void test_0x69_adc_overflow() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 0x7f,
                ByteUtil.overflow(0x69), 0x7f
        };
        var cpu = this.createInstance(rpg);
        var status = cpu.getStatus();
        Assertions.assertTrue(status.hasFlag(CSRegister.BIFlag.OVERFLOW_FLAG));
    }

    @Test
    void test_0xe9_sbc() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 0x10,
                ByteUtil.overflow(0xE9), 0x02
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0x0e);
    }

    @Test
    void test_0xe9_sbc_carry() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 0x0A,
                ByteUtil.overflow(0xE9), 0x0B
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0xff);
    }

//    @Test
//    void testNesFile() {
//        var nes = NES.NESBuilder.newBuilder()
//                .file(new File("nes/snow_bros.nes"))
//                .build();
//        nes.execute();
//    }

    CPU createInstance(byte[] rpg) {
        return this.createInstance(rpg, new byte[]{}, 0x8000);
    }
}
