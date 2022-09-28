package cn.navclub.nes4j.bin.model;

import cn.navclub.nes4j.bin.enums.AddressMode;
import cn.navclub.nes4j.bin.enums.CPUInstruction;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class Instruction6502 {
    //指令大小
    private final int bytes;
    //执行该指令所需CPU时钟
    private final int cycle;
    //指令操作码
    private final byte openCode;
    //寻址模式
    private final AddressMode addressMode;
    @Setter
    private CPUInstruction instruction;

    public Instruction6502(byte openCode, int bytes, int cycle, AddressMode addressMode) {
        this.bytes = bytes;
        this.cycle = cycle;
        this.openCode = openCode;
        this.addressMode = addressMode;
    }

    public Instruction6502(byte openCode, int bytes, int cycle, CPUInstruction instruction) {
        this(openCode, bytes, cycle, AddressMode.NoneAddressing);
        this.instruction = instruction;
    }


    public static Instruction6502 create(byte openCode, int bytes, int cycle, AddressMode addressMode) {
        return new Instruction6502(openCode, bytes, cycle, addressMode);
    }
}
