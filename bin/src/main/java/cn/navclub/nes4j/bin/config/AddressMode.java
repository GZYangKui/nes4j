package cn.navclub.nes4j.bin.config;

/**
 * CPU寻址模式
 */
public enum AddressMode {
    Accumulator,
    Immediate,
    ZeroPage,
    ZeroPage_X,
    ZeroPage_Y,
    Absolute,
    Absolute_X,
    Absolute_Y,
    Indirect,
    Indirect_X,
    Indirect_Y,
    Implied,
    Relative
}
