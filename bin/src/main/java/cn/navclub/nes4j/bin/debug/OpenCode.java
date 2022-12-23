package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.config.Instruction;

/**
 * @param index       Memory address
 * @param instruction 6502 instruction
 * @param operand     Memory address value
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public record OpenCode(int index, Instruction instruction, Operand operand) {
}