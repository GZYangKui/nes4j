package cn.navclub.nes4j.bin.core;

/**
 * <a href="https://www.nesdev.org/wiki/PPU_programmer_reference#PPUADDR">PPU address</a>
 */
public class PPUAddress {
    private boolean latch;
    //非小端序 高位字节->低位字节
    private final byte[] address;

    public PPUAddress() {
        this.latch = true;
        this.address = new byte[2];
    }

    public void set(int data) {
        this.address[0] = (byte) (data >> 8);
        this.address[1] = (byte) data;
    }

    public int update(byte b) {
        if (this.latch) {
            this.address[0] = b;
        } else {
            this.address[1] = b;
        }
        this.latch = !latch;
        return this.toInt16();
    }

    public int inc(int b) {
        var addr = this.toInt16() + b;
        this.set(addr);
        return addr;
    }

    public void reset() {
        this.latch = true;
    }

    public int get() {
        return this.toInt16();
    }

    @Override
    public String toString() {
        return String.format("0x%s", Integer.toHexString(this.toInt16()));
    }

    private int toInt16() {
        return (address[0] & 0xff) << 8 | (address[1] & 0xff);
    }
}
