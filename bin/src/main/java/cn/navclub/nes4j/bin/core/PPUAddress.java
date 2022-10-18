package cn.navclub.nes4j.bin.core;

/**
 *
 * <a href="https://www.nesdev.org/wiki/PPU_programmer_reference#PPUADDR">PPU address</a>
 *
 */
public class PPUAddress {
    private int address;
    private boolean latch;

    public PPUAddress() {
        this.latch = true;
    }

    public void set(int data) {
        this.address = (data & 0xffff);
    }

    public int set(byte b) {
        final int temp;
        if (this.latch) {
            temp = this.address & (b << 8);
        } else {
            temp = this.address | b;
        }
        this.set(temp);
        this.latch = !latch;
        return address;
    }

    public int inc(int b) {
        this.set(this.address + b);
        return this.address;
    }

    public void reset() {
        this.latch = true;
    }

    public int get() {
        return this.address;
    }

    @Override
    public String toString() {
        return String.valueOf(this.address);
    }
}
