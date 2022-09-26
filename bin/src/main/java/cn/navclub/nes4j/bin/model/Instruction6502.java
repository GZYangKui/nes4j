package cn.navclub.nes4j.bin.model;

import cn.navclub.nes4j.bin.enums.AddressMode;
import cn.navclub.nes4j.bin.enums.CPUInstruction;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Instruction6502 {
    private final byte openCode;
    private AddressMode addressMode;
    @Setter
    private CPUInstruction instruction;

    public Instruction6502(byte openCode, AddressMode addressMode) {
        this.openCode = openCode;
        this.addressMode = addressMode;
    }

    public Instruction6502(byte openCode, CPUInstruction instruction) {
        this.openCode = openCode;
        this.instruction = instruction;

    }

    public Instruction6502(byte openCode) {
        this.openCode = openCode;
    }

    public static Instruction6502 create(byte openCode, AddressMode addressMode) {
        return new Instruction6502(openCode, addressMode);
    }
}
