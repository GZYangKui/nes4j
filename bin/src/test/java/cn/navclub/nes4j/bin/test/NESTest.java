package cn.navclub.nes4j.bin.test;


import cn.navclub.nes4j.bin.core.JoyPad;
import cn.navclub.nes4j.bin.core.NES;
import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.screen.Frame;
import cn.navclub.nes4j.bin.screen.Render;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.function.BiConsumer;

public class NESTest {
    @Test
    void testNesFile() {
        var render = new Render();
        var frame = new Frame();
        BiConsumer<PPU, JoyPad> gameLoopCallback = (ppu, joyPad) -> {
            render.render(ppu, frame);
        };

        var nes = NES.NESBuilder.newBuilder()
                .file(new File("nes/snow_bros.nes"))
                .gameLoopCallback(gameLoopCallback)
                .build();
        nes.execute();
    }

    @Test
    void test_palette_ram() {
        var nes = NES.NESBuilder.newBuilder()
                .file(new File("nes/ppu/palette_ram.nes"))
                .build();
        nes.execute();
    }

    @Test
    void test_vram_access() {
        var nes = NES.NESBuilder.newBuilder()
                .file(new File("nes/ppu/vram_access.nes"))
                .build();
        nes.execute();
    }
}
