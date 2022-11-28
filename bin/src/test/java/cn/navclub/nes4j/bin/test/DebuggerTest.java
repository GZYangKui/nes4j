package cn.navclub.nes4j.bin.test;

import cn.navclub.nes4j.bin.core.Cartridge;
import cn.navclub.nes4j.bin.debug.OpenCodeFormat;
import org.junit.jupiter.api.Test;

import java.io.File;

public class DebuggerTest {
    @Test
    void testFormatOpenCode() {
        var cartridge = new Cartridge(new File("nes/helloworld.nes"));
        OpenCodeFormat.formatOpenCode(cartridge.getRgbrom());
    }
}
