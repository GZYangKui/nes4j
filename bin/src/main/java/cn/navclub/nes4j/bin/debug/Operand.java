package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.config.AddressMode;

/**
 *
 * 封装操作数
 * @param mode 寻址模式
 * @param lsb 低位字节
 * @param msb 高位字节
 */
public record Operand(AddressMode mode, byte lsb, byte msb) {
    public static final Operand DEFAULT_OPERAND = new Operand(AddressMode.Implied, (byte) 0, (byte) 0);
}
