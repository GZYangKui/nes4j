package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NESFile;
import lombok.Getter;

import java.io.File;

@Getter
public class NES {
    //程序计数器重置地址
    private static final int PC_RESET = 0xFFFC;
    //程序栈重置地址
    private static final int STACK_RESET = 0xFD;

    private final Bus bus;
    private final CPU cpu;
    private final PPU ppu;
    private final NESFile file;


    private NES(NESBuilder builder) {
        if (builder.buffer != null) {
            this.file = new NESFile(builder.buffer);
        } else {
            this.file = new NESFile(builder.file);
        }
        this.ppu = new PPU(file.getCh(), file.getMirrors());
        this.bus = new Bus(file.getRgb(), ppu);
        this.cpu = new CPU(this.bus, builder.stackRest, builder.pcReset);
    }

    public NES(byte[] rpg, byte[] ch, int pcReset, int stackReset) {
        this.file = null;
        this.ppu = new PPU(ch, 1);
        this.bus = new Bus(rpg, ppu);
        this.cpu = new CPU(this.bus, stackReset, pcReset);
    }

    public NES(byte[] rpg, byte[] ch) {
        this(rpg, ch, PC_RESET, STACK_RESET);
    }


    public void execute() {
        this.cpu.reset();
        while (true) {
            try {
                this.cpu.execute();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void test(int pcStart) {
        this.cpu.reset();
        this.cpu.setPc(pcStart);
        while (loop()) {
            this.cpu.execute();
        }
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

        public NES build() {
            return new NES(this);
        }

        public static NESBuilder newBuilder() {
            return new NESBuilder();
        }
    }
}