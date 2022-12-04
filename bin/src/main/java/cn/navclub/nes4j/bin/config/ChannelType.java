package cn.navclub.nes4j.bin.config;

import lombok.Getter;

@Getter
public enum ChannelType {
    PULSE(0x4000, .5),
    PULSE1(0x4004, .5),
    TRIANGLE(0x4008, 1),
    NOISE(0x400c, .5),
    DMC(0x4010, 1);

    private final int offset;
    private final double multiple;

    ChannelType(int offset, double multiple) {
        this.offset = offset;
        this.multiple = multiple;
    }
}
