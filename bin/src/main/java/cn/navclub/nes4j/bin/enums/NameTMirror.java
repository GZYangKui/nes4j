package cn.navclub.nes4j.bin.enums;


public enum NameTMirror {
    L1(0x2000),
    L2(0x2400),
    L3(0x2800),
    L4(0x2c00);

    public final int address;

    NameTMirror(int address) {
        this.address = address;
    }
}
