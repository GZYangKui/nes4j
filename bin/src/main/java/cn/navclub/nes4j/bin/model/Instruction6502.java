package cn.navclub.nes4j.bin.model;

import cn.navclub.nes4j.bin.enums.AddressModel;
import cn.navclub.nes4j.bin.enums.CPUInstruction;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Instruction6502 {
    private final byte openCode;
    private AddressModel addressModel;
    @Setter
    private CPUInstruction instruction;

    public Instruction6502(byte openCode, AddressModel addressModel) {
        this.openCode = openCode;
        this.addressModel = addressModel;
    }

    public Instruction6502(byte openCode, CPUInstruction instruction) {
        this.openCode = openCode;
        this.instruction = instruction;

    }

    public Instruction6502(byte openCode) {
        this.openCode = openCode;
    }

    public static Instruction6502 create(byte openCode, AddressModel addressModel) {
        return new Instruction6502(openCode, addressModel);
    }
}
