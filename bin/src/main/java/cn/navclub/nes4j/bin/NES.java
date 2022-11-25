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

    //程序计数器重置地址
    private static final int PC_RESET = 0xfffc;
    //程序栈重置地址
    private static final int STACK_RESET = 0xfd;

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


        this.bus = new Bus(
                cartridge.getMapper(),
                cartridge.getMirrors(),
                cartridge.getRgbrom(),
                cartridge.getChrom(),
                builder.gameLoopCallback
        );
        this.cpu = new CPU(this.bus, builder.stackRest, builder.pcReset);
        this.bus.setCpu(this.cpu);
    }

    public NES(byte[] rpgrom, byte[] chrom, int pcReset, int stackReset) {
        this.cartridge = null;

        this.bus = new Bus(NameMirror.VERTICAL, rpgrom, chrom);
        this.cpu = new CPU(this.bus, stackReset, pcReset);
        this.bus.setCpu(this.cpu);
    }

    public NES(byte[] rpg, byte[] ch) {
        this(rpg, ch, PC_RESET, STACK_RESET);
    }


    public void execute() {
        this.cpu.reset();
        while (!stop) {
            this.cpu.next();
        }
    }

    public void test(int pcStart) {
        this.cpu.setPc(pcStart);
        while (loop()) {
            this.cpu.next();
        }
    }

    public void stop() {
        this.stop = true;
        this.bus.getApu().stop();
    }

    private boolean loop() {
        var temp = this.cpu.getPc();
        var rpgSize = this.bus.rpgSize() + 0x8000;
        if (temp >= 0xC000 && rpgSize <= 0xC000) {
            temp = 0x8000 + (temp - 0xC000);
        }
        return temp < rpgSize;
    }


    public static class NESBuilder {
        private File file;
        private byte[] buffer;
        private int pcReset;
        private int stackRest;
        private TCallback<PPU, JoyPad, JoyPad> gameLoopCallback;

        public NESBuilder() {
            this.pcReset = PC_RESET;
            this.stackRest = STACK_RESET;
        }

        public NESBuilder pcReset(int pcReset) {
            this.pcReset = pcReset;
            return this;
        }

        public NESBuilder stackReset(int stackRest) {
            this.stackRest = stackRest;
            return this;
        }

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
