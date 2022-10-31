package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.util.ByteUtil;

/**
 * APU Channel
 */
public class Channel {
    private final byte[] buffer;
    private final ChannelType type;

    public Channel(ChannelType type) {
        this.type = type;
        this.buffer = new byte[4];
    }

    public void update(int address, byte b) {
        var pos = address % type.offset;
        this.buffer[pos] = b;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        var len = this.buffer.length;
        for (int i = 0; i < len; i++) {
            sb.append(ByteUtil.toBinStr(this.buffer[i]));
            if (i != len - 1) {
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    public enum ChannelType {
        PULSE(0x4000),
        PULSE1(0x4004),
        TRIANGLE(0x4008),
        NOISE(0x400c),
        DMC(0x4010);

        private final int offset;

        ChannelType(int offset) {
            this.offset = offset;
        }
    }
}
