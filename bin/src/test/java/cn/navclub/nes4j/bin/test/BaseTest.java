package cn.navclub.nes4j.bin.test;

import cn.navclub.nes4j.bin.core.NES;
import cn.navclub.nes4j.bin.core.CPU;

public class BaseTest {

    protected CPU createInstance(byte[] rpg, byte[] ch, byte[] data, int pc) {
        return this.createNES(rpg, ch, data, pc).getCpu();
    }

    protected NES createNES(byte[] rpg, byte[] ch, byte[] data, int pc) {
        var nes = new NES(rpg, ch);
        if (data != null) {
            var bus = nes.getBus();
            for (int i = 0; i < data.length; i++) {
                bus.writeByte(i, data[i]);
            }
        }
        nes.test(pc);
        return nes;
    }
}
