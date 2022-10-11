package cn.navclub.nes4j.bin.test;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.CPU;

public class BaseTest {

    protected CPU createInstance(byte[] rpg, byte[] ch, int pc) {
        var nes = new NES(rpg, ch);
        nes.test(pc);
        return nes.getCpu();
    }
}
