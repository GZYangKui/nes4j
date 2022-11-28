package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.enums.CPUInstruction;

public record OpenCode(int index, CPUInstruction instruction, String operator) {
}