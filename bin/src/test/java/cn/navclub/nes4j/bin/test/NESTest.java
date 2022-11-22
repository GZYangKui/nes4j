package cn.navclub.nes4j.bin.test;


import cn.navclub.nes4j.bin.core.JoyPad;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.PPU;
import cn.navclub.nes4j.bin.function.TCallback;
import cn.navclub.nes4j.bin.screen.Frame;
import cn.navclub.nes4j.bin.screen.Render;
import org.junit.jupiter.api.Test;

import java.io.File;

public class NESTest {
    private int counter;

    @Test
    void testNesFile() {
        var frame = new Frame();
        var render = new Render();
        TCallback<PPU, JoyPad, JoyPad> gameLoopCallback = (ppu, joyPad, joyPad1) -> {
            render.render(ppu, frame);
        };

        var nes = NES.NESBuilder.newBuilder()
                .file(new File("nes/Super_Mario_Bros(J).nes"))
                .gameLoopCallback(gameLoopCallback)
                .build();
        nes.execute();
    }

    @Test
    void test_hello_world() {
        var frame = new Frame();
        var render = new Render();
        TCallback<PPU, JoyPad, JoyPad> gameLoopCallback = (ppu, joyPad, joyPad1) -> {
            render.render(ppu, frame);
        };

        var nes = NES.NESBuilder.newBuilder()
                .file(new File("nes/hello2.nes"))
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

    @Test
    void test_cpu_dummy_read() {
        var frame = new Frame();
        var render = new Render();
        TCallback<PPU, JoyPad, JoyPad> gameLoopCallback = (ppu, joyPad, joyPad1) -> {
            render.render(ppu, frame);
        };
        var nes = NES.NESBuilder.newBuilder()
                .gameLoopCallback(gameLoopCallback)
                .file(new File("nes/cpu_dummy_reads.nes"))
                .build();
        nes.execute();
    }
}
