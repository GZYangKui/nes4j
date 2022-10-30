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
        if (data > 0x3fff) {
            data = data & 0b11111111111111;
        }
        this.address[0] = (byte) (data >> 8);
        this.address[1] = (byte) data;
    }

    public void update(byte b) {
        if (this.latch) {
            this.address[0] = b;
        } else {
            this.address[1] = b;
        }

        var addr = this.get();
        if (addr > 0x3fff) {
            this.set(addr);
        }

        this.latch = !latch;
    }

    public void inc(int b) {
        this.set(this.toInt16() + b);
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
