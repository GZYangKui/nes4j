package cn.navclub.nes4j.bin.enums;

import cn.navclub.nes4j.bin.config.AddressModel;
import cn.navclub.nes4j.bin.model.Instruction6502;


/**
 * 6502 指令集
 */
public enum CPUInstruction {
    ADC(new Instruction6502[]{
            Instruction6502.create((byte) 0x69, cn.navclub.nes4j.bin.config.AddressModel.Immediate),
            Instruction6502.create((byte) 0x65, cn.navclub.nes4j.bin.config.AddressModel.ZeroPage),
            Instruction6502.create((byte) 0x75, cn.navclub.nes4j.bin.config.AddressModel.ZeroPage_X),
            Instruction6502.create((byte) 0x6D, cn.navclub.nes4j.bin.config.AddressModel.Absolute),
            Instruction6502.create((byte) 0x7D, cn.navclub.nes4j.bin.config.AddressModel.Absolute_X),
            Instruction6502.create((byte) 0x79, cn.navclub.nes4j.bin.config.AddressModel.Absolute_Y),
            Instruction6502.create((byte) 0x61, cn.navclub.nes4j.bin.config.AddressModel.Indirect_X),
            Instruction6502.create((byte) 0x71, cn.navclub.nes4j.bin.config.AddressModel.Indirect_Y),
    }),
    /*
     *
     * 加载一字节数据进入累加器并视情况而定是否需要设置零或者负数flags.
     *
     * {@link https://www.nesdev.org/obelisk-6502-guide/reference.html#LDA}
     *
     */
    LDA(new Instruction6502[]{
            Instruction6502.create((byte) 0xA9, cn.navclub.nes4j.bin.config.AddressModel.Immediate),
            Instruction6502.create((byte) 0xA5, cn.navclub.nes4j.bin.config.AddressModel.ZeroPage),
            Instruction6502.create((byte) 0xB5, cn.navclub.nes4j.bin.config.AddressModel.ZeroPage_X),
            Instruction6502.create((byte) 0xAD, cn.navclub.nes4j.bin.config.AddressModel.Absolute),
            Instruction6502.create((byte) 0xBD, cn.navclub.nes4j.bin.config.AddressModel.Absolute_X),
            Instruction6502.create((byte) 0xB9, cn.navclub.nes4j.bin.config.AddressModel.Absolute_Y),
            Instruction6502.create((byte) 0xA1, cn.navclub.nes4j.bin.config.AddressModel.Indirect_X),
            Instruction6502.create((byte) 0xB1, AddressModel.Indirect_Y),
    });
    private final Instruction6502 instruction6502;
    private final Instruction6502[] instruction6502s;

    CPUInstruction(Instruction6502 instruction6502, Instruction6502[] instruction6502s) {
        this.instruction6502 = instruction6502;
        this.instruction6502s = instruction6502s;
    }

    CPUInstruction(Instruction6502[] instruction6502s) {
        this.instruction6502 = null;
        this.instruction6502s = instruction6502s;
    }

    CPUInstruction(Instruction6502 instruction6502) {
        this.instruction6502s = null;
        this.instruction6502 = instruction6502;
    }
}
