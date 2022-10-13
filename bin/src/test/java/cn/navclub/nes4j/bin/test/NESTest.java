package cn.navclub.nes4j.bin.test;


import cn.navclub.nes4j.bin.NES;
import org.junit.jupiter.api.Test;

import java.io.File;

public class NESTest {
    @Test
    void testNesFile() {
        var nes = NES.NESBuilder.newBuilder()
                .file(new File("nes/snow_bros.nes"))
                .build();
        nes.execute();
    }

    @Test
    void test_palette_ram(){
        var nes = NES.NESBuilder.newBuilder()
                .file(new File("nes/ppu/palette_ram.nes"))
                .build();
        nes.execute();
    }

    @Test
    void test_vram_access(){
        var nes = NES.NESBuilder.newBuilder()
                .file(new File("nes/ppu/vram_access.nes"))
                .build();
        nes.execute();
    }
}
