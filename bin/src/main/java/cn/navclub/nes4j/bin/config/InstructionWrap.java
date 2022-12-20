package cn.navclub.nes4j.bin.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
public class InstructionWrap {
    //Instruction size
    private final int size;
    //Execute instruction need cycle
    private final int cycle;
    //Instruction open code
    private final byte openCode;
    //Memory address model
    private final AddressMode addressMode;

    private Instruction instruction;

    public InstructionWrap(byte openCode, int size, int cycle, AddressMode addressMode) {
        this.size = size;
        this.cycle = cycle;
        this.openCode = openCode;
        this.addressMode = addressMode;
    }

    public InstructionWrap(byte openCode, int size, int cycle, Instruction instruction) {
        this(openCode, size, cycle, AddressMode.Implied);
        this.instruction = instruction;
    }


    public static InstructionWrap create(byte openCode, int size, int cycle, AddressMode addressMode) {
        return new InstructionWrap(openCode, size, cycle, addressMode);
    }
}