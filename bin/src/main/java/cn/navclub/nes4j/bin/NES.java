package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.core.*;
import cn.navclub.nes4j.bin.enums.NameMirror;
import cn.navclub.nes4j.bin.function.TCallback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@Getter
public class NES {
    static {
        System.loadLibrary("nes4j");
    }

    private final Bus bus;
    private final CPU cpu;
    private final Cartridge cartridge;

    private volatile boolean stop;


    private NES(NESBuilder builder) {
        if (builder.buffer != null) {
            this.cartridge = new Cartridge(builder.buffer);
        } else {
            this.cartridge = new Cartridge(builder.file);
        }

        this.bus = new Bus(this.cartridge, builder.gameLoopCallback);
        this.cpu = new CPU(this.bus);
    }

    public void execute() {
        this.cpu.reset();
        while (!stop) {
            if (this.bus.getStall() > 0)
                this.bus.tick(1);
            else
                this.cpu.next();
        }
    }

    public void stop() {
        this.stop = true;
        this.bus.stop();
    }

    public static class NESBuilder {
        private File file;
        private byte[] buffer;
        private TCallback<PPU, JoyPad, JoyPad> gameLoopCallback;

        public NESBuilder buffer(byte[] buffer) {
            this.buffer = buffer;
            return this;
        }

        public NESBuilder file(File file) {
            this.file = file;
            return this;
        }

        public NESBuilder file(String file) {
            this.file = new File(file);
            return this;
        }

        public NESBuilder gameLoopCallback(TCallback<PPU, JoyPad, JoyPad> gameLoopCallback) {
            this.gameLoopCallback = gameLoopCallback;
            return this;
        }

        public NES build() {
            return new NES(this);
        }

        public static NESBuilder newBuilder() {
            return new NESBuilder();
        }
    }
}
