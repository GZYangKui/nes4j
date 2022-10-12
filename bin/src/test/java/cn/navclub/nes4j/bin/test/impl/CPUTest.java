package cn.navclub.nes4j.bin.test.impl;

import cn.navclub.nes4j.bin.core.CPU;
import cn.navclub.nes4j.bin.enums.CPUStatus;
import cn.navclub.nes4j.bin.test.BaseTest;
import cn.navclub.nes4j.bin.util.ByteUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;



/**
 *
 * CPU指令测试
 *
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
    void test_0x69_adc_overflow() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 0x7f,
                ByteUtil.overflow(0x69), 0x7f
        };
        var cpu = this.createInstance(rpg);
        var status = cpu.getStatus();
        Assertions.assertTrue(status.contain(CPUStatus.OF));
    }

    @Test
    void test_0xe9_sbc() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 0x10,
                ByteUtil.overflow(0xE9), 0x02
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0x0d);
    }

    @Test
    void test_0xe9_sbc_carry() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xA9), 0x0A,
                ByteUtil.overflow(0xE9), 0x0B
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0xfe);
    }

    @Test
    void test_0xe9_sbc_negative() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xa9), 0x02,
                ByteUtil.overflow(0xe9), 0x03
        };
        var cpu = this.createInstance(rpg);
        var status = cpu.getStatus();
        Assertions.assertTrue(status.contain(CPUStatus.NF));
        Assertions.assertEquals(cpu.getRa(), 0xfe);
    }

    @Test
    void test_0xe9_sbc_overflow() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xa9), 0x50,
                ByteUtil.overflow(0xe9), ByteUtil.overflow(0xb0)
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0x9f);
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
