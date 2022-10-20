package cn.navclub.nes4j.bin.enums;

import lombok.Getter;

/**
 * NMI、IRQ属于外部输入硬件中断
 */
@Getter
public enum CPUInterrupt {
    //PPU
    NMI(2, 0xfffa),
    //APU
    IRQ(2, 0xfffe),
    //CPU
    BRK(1, 0xfffe);

    private final int cycle;
    private final int vector;

    CPUInterrupt(int cycle, int vector) {
        this.cycle = cycle;
        this.vector = vector;
    }
}
