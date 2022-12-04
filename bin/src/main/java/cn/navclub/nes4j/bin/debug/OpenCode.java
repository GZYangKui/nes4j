package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.config.CPUInstruction;

public record OpenCode(int index, CPUInstruction instruction, Operand operand) {
}