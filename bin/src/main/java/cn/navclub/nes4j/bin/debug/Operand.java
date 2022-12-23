package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.config.AddressMode;

/**
 * Wrap 6502 operand
 *
 * @param mode Address mode
 * @param lsb  The lower byte
 * @param msb  The upper byte
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public record Operand(AddressMode mode, byte lsb, byte msb) {
    public static final Operand DEFAULT_OPERAND = new Operand(AddressMode.Implied, (byte) 0, (byte) 0);
}
