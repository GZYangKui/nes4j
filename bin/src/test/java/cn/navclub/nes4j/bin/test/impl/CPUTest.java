package cn.navclub.nes4j.bin.test.impl;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.CPU;
import cn.navclub.nes4j.bin.enums.CPUStatus;
import cn.navclub.nes4j.bin.test.BaseTest;
import cn.navclub.nes4j.bin.util.ByteUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * CPU指令测试
 */
public class CPUTest extends BaseTest {
    private static final int PC_OFFSET = 0x8000;

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
                //LDA
                (byte) 0xa9, 0x10,
                0x69, 0x02,
        };
        var cpu = createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0x12);
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 4);
    }

    @Test
    void test_0x69_adc_carry_zero_flag() {
        var rpg = new byte[]{
                //LDA
                (byte) 0xa9, (byte) 0x81,
                //ADC
                0x69, 0x7f
        };
        var cpu = createInstance(rpg);
        var status = cpu.getStatus();
        Assertions.assertEquals(cpu.getRa(), 0x00);
        Assertions.assertTrue(status.contain(CPUStatus.ZF));
        Assertions.assertTrue(status.contain(CPUStatus.CF));
        Assertions.assertFalse(status.contain(CPUStatus.OF));
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 4);
    }

    @Test
    void test_0x69_adc_overflow_carry_flag() {
        var rpg = new byte[]{
                //LDA
                (byte) 0xa9, (byte) 0x8a,
                //ADC
                0x69, (byte) 0x8a
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0x14);
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.OF));
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.CF));
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 4);
    }


    @Test
    void test_0xe9_sbc() {
        var rpg = new byte[]{
                ByteUtil.overflow(0xa9), 0x10,
                ByteUtil.overflow(0xe9), 0x02
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.CF));
        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.NF));
        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.OF));
        Assertions.assertEquals(cpu.getRa(), 0x0d);
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 4);
    }

    @Test
    void test_0xe9_sbc_neg() {
        var rpg = new byte[]{
                (byte) 0xa9, 0x02,
                (byte) 0xe9, 0x03
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0xfe);
        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.CF));
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.NF));
    }

    @Test
    void test_0xe9_sbc_overflow() {
        var rpg = new byte[]{
                (byte) 0xa9, 0x50,
                (byte) 0xe9, (byte) 0xb0
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0x9f);
        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.CF));
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.NF));
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.OF));
    }

    @Test
    void test_0x0a_asl_accumulator() {
        var rpg = new byte[]{
                (byte) 0xa9, (byte) 0b11010010,
                0x0a
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 3);
        Assertions.assertEquals(cpu.getRa(), 0b10100100);
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.CF));
    }

    @Test
    void test_0x06_asl_memory() {
        var nes = this.createNES(new byte[]{
                0x06, 0x10
        });
        var bus = nes.getBus();
        bus.write(0x10, (byte) 0b01000001);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(bus.readUSByte(0x10), 0b10000010);
        Assertions.assertTrue(nes.getCpu().getStatus().contain(CPUStatus.NF));
    }

    @Test
    void test_0x06_asl_memory_flags() {
        var nes = this.createNES(new byte[]{
                0x06, 0x10
        });
        var bus = nes.getBus();
        bus.writeInt(0x10, 0b10000000);
        nes.test(PC_OFFSET);
        var status = nes.getCpu().getStatus();
        Assertions.assertEquals(bus.read(0x10), 0b0);
        Assertions.assertTrue(status.contain(CPUStatus.CF));
        Assertions.assertTrue(status.contain(CPUStatus.ZF));
        Assertions.assertFalse(status.contain(CPUStatus.NF));
    }

    @Test
    void test_0xf6_inc_memory_zero_page_x() {
        var nes = this.createNES(new byte[]{
                (byte) 0xa2, 0x01,
                (byte) 0xf6, 0x0f
        });
        var bus = nes.getBus();
        bus.writeUSByte(0x10, 127);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(bus.readUSByte(0x10), 128);
        Assertions.assertTrue(nes.getCpu().getStatus().contain(CPUStatus.NF));
    }

    @Test
    void test_0x46_lsr_memory_flags() {
        var nes = this.createNES(new byte[]{
                0x46, 0x10
        });
        var bus = nes.getBus();
        bus.write(0x10, (byte) 0b00000001);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(bus.read(0x10), 0b0);
        Assertions.assertTrue(nes.getCpu().getStatus().contain(CPUStatus.CF));
        Assertions.assertTrue(nes.getCpu().getStatus().contain(CPUStatus.ZF));
        Assertions.assertFalse(nes.getCpu().getStatus().contain(CPUStatus.NF));
    }

    @Test
    void test_0x2e_rol_memory_flags() {
        var nes = this.createNES(new byte[]{
                0x2e, 0x10, 0x15
        });
        var bus = nes.getBus();
        bus.write(0x1510, (byte) 0b10000001);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(bus.read(0x1510), 0b00000010);
        Assertions.assertTrue(nes.getCpu().getStatus().contain(CPUStatus.CF));
    }

    @Test
    void test_0x2e_rol_memory_flags_carry() {
        var nes = this.createNES(new byte[]{
                0x2e, 0x10, 0x15
        });
        var status = nes.getCpu().getStatus();
        status.set(CPUStatus.CF);
        var bus = nes.getBus();
        bus.write(0x1510, (byte) 0b00000001);
        nes.test(PC_OFFSET);
        Assertions.assertFalse(status.contain(CPUStatus.CF));
    }

    @Test
    void est_0x6e_ror_memory_flags_carry() {
        var nes = this.createNES(new byte[]{
                0x6e, 0x10, 0x15
        });
        var status = nes.getCpu().getStatus();
        status.set(CPUStatus.CF);
        var bus = nes.getBus();
        bus.write(0x1510, (byte) 0b01000010);
        nes.test(PC_OFFSET);
        Assertions.assertFalse(status.contain(CPUStatus.CF));
        Assertions.assertEquals(bus.readUSByte(0x1510), 0b10100001);
    }

    @Test
    void test_0x6e_zero_flag() {
        var nes = this.createNES(new byte[]{
                0x6e, 0x10, 0x15
        });
        var status = nes.getCpu().getStatus();
        status.set(CPUStatus.CF);
        var bus = nes.getBus();
        bus.write(0x1510, (byte) 0b00000001);
        nes.test(PC_OFFSET);
        Assertions.assertFalse(status.contain(CPUStatus.ZF));
    }

    @Test
    void test_0x6a_ror_accumulator_zero_flag() {
        var cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 0x01,
                0x6a
        });
        Assertions.assertEquals(cpu.getRa(), 0);
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.CF));
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.ZF));
    }

    @Test
    void test_0xbe_ldx_absolute_y() {

        var nes = this.createNES(new byte[]{
                //LDY
                (byte) 0xa0, 0x66,
                //LDX
                (byte) 0xbe, 0x00, 0x11
        });
        var bus = nes.getBus();
        bus.write(0x1166, (byte) 55);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(nes.getCpu().getRx(), 55);
    }

    @Test
    void test_0xb4_ldy_zero_page_x() {
        var nes = this.createNES(new byte[]{
                //ldx
                (byte) 0xa2, 0x06,
                //ldy
                (byte) 0xb4, 0x60,
        });
        var bus = nes.getBus();
        bus.write(0x66, (byte) 55);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(nes.getCpu().getRy(), 55);
    }

    @Test
    void test_0xc8_iny() {
        var nes = this.createNES(new byte[]{
                //ldy
                (byte) 0xa0, 127,
                //INY
                (byte) 0xc8
        });
        nes.test(PC_OFFSET);
        Assertions.assertEquals(nes.getCpu().getRy(), 128);
        Assertions.assertTrue(nes.getCpu().getStatus().contain(CPUStatus.NF));
    }

    @Test
    void test_0xe8_inx() {
        var nes = this.createNES(new byte[]{
                //ldx
                (byte) 0xa2, (byte) 0xff,
                //INY
                (byte) 0xe8
        });
        nes.test(PC_OFFSET);
        Assertions.assertEquals(nes.getCpu().getRx(), 0);
        Assertions.assertTrue(nes.getCpu().getStatus().contain(CPUStatus.ZF));
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
        var nes = this.createNES(new byte[]{
                0x6c, 0x20, 0x01
        });
        nes.getBus().write(0x0120, (byte) 0xfc);
        nes.getBus().write(0x0121, (byte) 0xba);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(nes.getCpu().getPc(), 0xbafc);
    }

    @Test
    void test_0x48_pha() {
        var rpg = new byte[]{
                (byte) 0xa9, 100,
                0x48
        };
        var cpu = this.createInstance(rpg);
        var bus = cpu.getBus();
        Assertions.assertEquals(cpu.getSp(), cpu.getStackReset() - 1);
        Assertions.assertEquals(bus.readUSByte(CPU.STACK + cpu.getStackReset()), 100);
    }

    @Test
    void test_0x68_pla() {
        var rpg = new byte[]{
                //LAD
                (byte) 0xa9, (byte) 0xff,
                //PHA
                0x48,
                //LAD
                (byte) 0xa9, 0x00,
                //PLA
                0x68
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertEquals(cpu.getRa(), 0xff);
        Assertions.assertEquals(cpu.getSp(), cpu.getStackReset());
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 6);
    }

    @Test
    void test_0x48_pla_flags() {
        var rpg = new byte[]{
                //LDA
                (byte) 0xa9, 0x00,
                //PHA
                0x48,
                //LDA
                (byte) 0xa9, (byte) 0xff,
                //PLA
                0x68
        };
        var cpu = this.createInstance(rpg);
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.ZF));
        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.NF));
    }

    @Test
    void test_stack_overflow() {
        var cpu = this.createInstance(new byte[]{0x68, 0x68, 0x68});
        Assertions.assertEquals(cpu.getSp(), 0);
    }

    @Test
    void test_0x18_clc() {
        var nes = this.createNES(new byte[]{0x18});
        var cpu = nes.getCpu();
        var status = cpu.getStatus();
        status.set(CPUStatus.CF);
        Assertions.assertTrue(status.contain(CPUStatus.CF));
        nes.test(PC_OFFSET);
        Assertions.assertFalse(status.contain(CPUStatus.CF));
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 1);
    }

    @Test
    void test_0x38_sec() {
        var nes = this.createNES(new byte[]{0x38});
        var cpu = nes.getCpu();
        var status = cpu.getStatus();
        Assertions.assertFalse(status.contain(CPUStatus.CF));
        nes.test(PC_OFFSET);
        Assertions.assertTrue(status.contain(CPUStatus.CF));
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 1);
    }

    @Test
    void test_0x85_sta() {
        var cpu = this.createInstance(new byte[]{
                //LDA
                (byte) 0xa9, 100,
                //STA
                (byte) 0x85, 10
        });
        var b = cpu.getBus().read(10);
        Assertions.assertEquals(b, 100);
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 4);
    }

    @Test
    void test_0x95_sta() {
        var cpu = this.createInstance(new byte[]{
                //LDA
                (byte) 0xa9, 101,
                //LDX
                (byte) 0xa2, 0x50,
                //STA
                (byte) 0x95, 0x10
        });
        var b = cpu.getBus().read(0x60);
        Assertions.assertEquals(b, 101);
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 6);
    }

    @Test
    void test_0x8d_sta() {
        var cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 100,
                (byte) 0x8d, 0x00, 0x02
        });
        var b = cpu.getBus().read(0x0200);
        Assertions.assertEquals(b, 100);
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 5);
    }

    @Test
    void test_0x9d_sta_absolute_x() {
        var cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 101,
                (byte) 0xa2, 0x50,
                (byte) 0x9d, 0x00, 0x11
        });
        Assertions.assertEquals(cpu.getBus().read(0x1150), 101);
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 7);
    }

    @Test
    void test_0x99_sta_absolute_y() {
        var cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 101,
                (byte) 0Xa0, 0x66,
                (byte) 0x99, 0x00, 0x11
        });
        Assertions.assertEquals(cpu.getBus().read(0x1166), 101);
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 7);
    }

    @Test
    void test_0x81_sta() {
        var nes = this.createNES(new byte[]{
                (byte) 0xa9, 0x66,
                (byte) 0xa2, 0x02,
                (byte) 0x81, 0x00
        });
        var bus = nes.getBus();
        bus.write(0x02, (byte) 0x05);
        bus.write(0x03, (byte) 0x07);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(bus.read(0x0705), 0x66);
        Assertions.assertEquals(nes.getCpu().getPc(), PC_OFFSET + 6);
    }

    @Test
    void test_0x91_sta() {
        var nes = this.createNES(new byte[]{
                (byte) 0xa9, 0x66,
                (byte) 0xa0, 0x10,
                (byte) 0x91, 0x02
        });
        var bus = nes.getBus();
        bus.write(0x02, (byte) 0x05);
        bus.write(0x03, (byte) 0x07);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(bus.read(0x0705 + 0x10), 0x66);
        Assertions.assertEquals(nes.getCpu().getPc(), PC_OFFSET + 6);
    }

    @Test
    void test_0x40_rti() {
        var nes = this.createNES(new byte[]{
                0x40
        });
        var cpu = nes.getCpu();
        var status = cpu.getStatus();
        status.setBits((byte) 0b11000001);
        cpu.setPc(0x8010);
        cpu.pushInt(cpu.getPc());
        cpu.pushByte(status.getBits());

        status.setBits((byte) 0);
        cpu.setPc(0);

        nes.test(PC_OFFSET);

        Assertions.assertEquals(status.getBits() & 0xff, 0b11000001);
        Assertions.assertEquals(cpu.getPc(), 0x8010);
    }

    @Test
    void test_0xaa_tax() {
        var cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 66,
                (byte) 0xaa
        });
        Assertions.assertEquals(cpu.getRa(), 66);
    }

    @Test
    void test_0xa8_tay() {
        var cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 66,
                (byte) 0xa8
        });
        Assertions.assertEquals(cpu.getRa(), 66);
    }

    @Test
    void test_0xba_tsx() {
        var cpu = this.createInstance(new byte[]{
                (byte) 0xba
        });
        Assertions.assertEquals(cpu.getRx(), cpu.getStackReset());
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.NF));
    }

    @Test
    void test_0x8a_txa() {
        var cpu = this.createInstance(new byte[]{
                (byte) 0xa2, 66,
                (byte) 0x8a
        });
        Assertions.assertEquals(cpu.getRa(), 66);
    }

    @Test
    void test_0x9a_txs() {
        var nes = this.createNES(new byte[]{
                (byte) 0x9a
        });
        var cpu = nes.getCpu();
        cpu.setRx(0);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(cpu.getSp(), 0);
        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.ZF));
    }

    @Test
    void test_0x98_tya() {
        var nes = this.createNES(new byte[]{
                (byte) 0x98
        });
        var cpu = nes.getCpu();
        cpu.setRy(66);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(cpu.getRa(), 66);
    }

    @Test
    void test_0x20_jsr() {
        var cpu = this.createInstance(new byte[]{
                0x20, 0x30, (byte) 0x80
        });
        Assertions.assertEquals(cpu.getPc(), 0x8030);
        Assertions.assertEquals(cpu.getSp(), cpu.getStackReset() - 0x02);
        var pos = cpu.popInt();
        Assertions.assertEquals(PC_OFFSET + 2, pos);
    }

    @Test
    void test_0xc9_cmp_immidiate() {
        var cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 0x06,
                (byte) 0xc9, 0x05
        });
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.CF));

        cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 0x06,
                (byte) 0xc9,
                0x06
        });
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.CF));
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.ZF));

        cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 0x06,
                (byte) 0xc9, 0x07
        });

        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.CF));
        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.ZF));
        Assertions.assertTrue(cpu.getStatus().contain(CPUStatus.NF));

        cpu = this.createInstance(new byte[]{
                (byte) 0xa9, 0x06,
                (byte) 0xc9, (byte) 0x90
        });

        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.CF));
        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.ZF));
        Assertions.assertFalse(cpu.getStatus().contain(CPUStatus.NF));
    }

    @Test
    void test_0xd0_bne() {
        //jump
        var cpu = this.createInstance(new byte[]{
                (byte) 0xd0, 0x04
        });
        Assertions.assertEquals(cpu.getPc(), PC_OFFSET + 0x06);

        //no jump
        var nes = this.createNES(new byte[]{
                (byte) 0xd0, 0x04
        });
        nes.getCpu().getStatus().set(CPUStatus.ZF);
        nes.test(PC_OFFSET);
        Assertions.assertEquals(nes.getCpu().getPc(), PC_OFFSET + 0x02);
    }

    @Test
    void test_0xd0_bne_snippet() {
        var cpu = this.createInstance(new byte[]{
                //LDX
                (byte) 0xa2, 0x08,
                //DEX
                (byte) 0xca,
                //INY
                (byte) 0xc8,
                //CPX
                (byte) 0xe0, 0x03,
                //BNE
                (byte) 0xd0, (byte) 0xfa,
                //BRK
                0x00
        });
    }


//    @Test
//    void test_0x60_rts() {
//        var cpu = this.createInstance(new byte[]{
//                //JSR 0x8003
//                0x20, 0x03, (byte) 0x80,
//                (byte) 0xa2, 0x05,
//                //RTS
//                0x60
//        });
//        Assertions.assertEquals(cpu.getPc(), 108);
//        Assertions.assertEquals(cpu.getSp(), cpu.getStackReset());
//        Assertions.assertEquals(cpu.getRx(), 0x5);
//    }


    CPU createInstance(byte[] rpg, byte[] data) {
        return this.createInstance(rpg, new byte[]{}, data, PC_OFFSET);
    }

    CPU createInstance(byte[] rpg) {
        return this.createInstance(rpg, null);
    }

    NES createNES(byte[] rpg) {
        return this.createOriginNES(rpg, null, null);
    }
}
