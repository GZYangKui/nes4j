package cn.navclub.nes4j.bin.test.impl;

import cn.navclub.nes4j.bin.core.CPU;
import cn.navclub.nes4j.bin.core.CPUStatus;
import cn.navclub.nes4j.bin.test.BaseTest;
import cn.navclub.nes4j.bin.util.ByteUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * CPU指令测试
 */
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
        Assertions.assertTrue(status.contain(CPUStatus.ZF));
        Assertions.assertTrue(status.contain(CPUStatus.CF));
        Assertions.assertFalse(status.contain(CPUStatus.OF));
    }


    @Test
    void test_0xe9_sbc() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 0x01,
                ByteUtil.overflow(0xE9), 0x0A
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 247);
    }

    @Test
    void test_0xe9_sbc_carry() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 0x0A,
                ByteUtil.overflow(0xE9), 0x0B
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0Xff);
    }

    @Test
    void test_0xe9_sbc_b16_overflow() {
        var data = new byte[]{
                0x00, 0x02, 0x00, 0x03, 0x00
        };
        var rpg = new byte[]{
                ByteUtil.overflow(0xa5), 0x01,
                ByteUtil.overflow(0xe5), 0x03,

                //STA
                ByteUtil.overflow(0x85), 0x05,

                ByteUtil.overflow(0xa5), 0x02,
                ByteUtil.overflow(0xe5), 0x04,

                //STA
                ByteUtil.overflow(0x85), 0x06,
        };
        var cpu = this.createInstance(rpg, data);
        var bus = cpu.getBus();
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.OF));
        Assertions.assertEquals(bus.readInt(0x05), Math.pow(2,16)-1);
    }

    @Test
    void test_0xe9_sbc_overflow() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xa9), 0x50,
                ByteUtil.overflow(0xe9), ByteUtil.overflow(0xb0)
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 160);
    }


    @Test
    void test_0xe9_sbc_without_carry() {
        var data = new byte[]{
                0x00, 0x02, 0x05, 0x03, 0x01
        };
        var rpg = new byte[]{
                ByteUtil.overflow(0xa5), 0x01,
                ByteUtil.overflow(0xe5), 0x03,
                //STA
                ByteUtil.overflow(0x85), 0x05,
                ByteUtil.overflow(0xa5), 0x02,
                ByteUtil.overflow(0xe5), 0x04,
                //STA
                ByteUtil.overflow(0x85), 0x06,
        };
        var cpu = this.createInstance(rpg, data);
        var bus = cpu.getBus();
        Assertions.assertEquals(bus.readInt(0x05), 1023);
    }

    @Test
    void test_0xe9_sbc_neg() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xa9), -2,
                ByteUtil.overflow(0xe9), -3
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), Byte.toUnsignedInt((byte) -5));
    }

    @Test
    void test_0x0a_asl_accumulator() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xa9), 0x01,
                0x0a
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0x02);
    }

    @Test
    void test_0x06_asl_memory() {
        var rpg = new byte[]{
                0x06, 0x01,
                ByteUtil.overflow(0xa5), 0x01
        };
        var cpu = this.createInstance(rpg, new byte[]{0x00, 0x0f});
        Assertions.assertEquals(cpu.getRa(), 30);
    }

    @Test
    void test_0x0a_asl_carry_neg_flag() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xa9), ByteUtil.overflow(0xff),
                0x0a
        };
        var cpu = this.createInstance(rpg);
        var status = cpu.getStatus();
        Assertions.assertEquals(cpu.getRa(), 0xfe);
        Assertions.assertTrue(status.contain(CPUStatus.CF));
        Assertions.assertTrue(status.contain(CPUStatus.NF));
    }

    @Test
    void test_0x4c_jmp_absolute() {
        var rpg = new byte[]{
                0x4c, 0x0A, ByteUtil.overflow(0x80)
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getPc(), 0x800A);
    }

    @Test
    void test_0x6c_jmp_indirect() {
        var rpg = new byte[]{
                0x6c, 0x00, 0x00
        };
        var data = new byte[]{
                0x0A, ByteUtil.overflow(0x80)
        };
        var cpu = this.createInstance(rpg, data);
        Assertions.assertEquals(cpu.getPc(), 0x800A);
    }

    CPU createInstance(byte[] rpg, byte[] data) {
        return this.createInstance(rpg, new byte[]{}, data, 0x8000);
    }

    CPU createInstance(byte[] rpg) {
        return this.createInstance(rpg, null);
    }
}
