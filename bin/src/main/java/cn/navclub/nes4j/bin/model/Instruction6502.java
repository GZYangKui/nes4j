package cn.navclub.nes4j.bin.model;

import cn.navclub.nes4j.bin.config.AddressModel;

public class Instruction6502 {
    private final byte openCode;
    private final AddressModel addressModel;

    public Instruction6502(byte openCode, AddressModel addressModel) {
        this.openCode = openCode;
        this.addressModel = addressModel;
    }

    public Instruction6502(byte openCode) {
        this.openCode = openCode;
        this.addressModel = null;
    }

    public static Instruction6502 create(byte openCode, AddressModel addressModel) {
        return new Instruction6502(openCode, addressModel);
    }
}
