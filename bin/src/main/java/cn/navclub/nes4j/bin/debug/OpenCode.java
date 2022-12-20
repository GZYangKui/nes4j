package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.config.Instruction;

public record OpenCode(int index, Instruction instruction, Operand operand) {
}