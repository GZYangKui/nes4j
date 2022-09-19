package cn.navclub.nes4j.bin.config;

/**
 *
 * CPU寻址模式
 *
 */
public enum AddressModel {
    Accumulator,
    Immediate,
    ZeroPage,
    ZeroPage_X,
    ZeroPage_Y,
    Absolute,
    Absolute_X,
    Absolute_Y,
    Indirect_X,
    Indirect_Y,
    NoneAddressing,
}
