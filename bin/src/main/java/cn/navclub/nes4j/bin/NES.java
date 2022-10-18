package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.core.*;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
    @Setter
    private volatile boolean stop;
    private final Function<Throwable, Boolean> errorHandler;


    private NES(NESBuilder builder) {
        if (builder.buffer != null) {
            this.file = new NESFile(builder.buffer);
        } else {
            this.file = new NESFile(builder.file);
        }
        this.errorHandler = builder.errorHandler;
        this.ppu = new PPU(file.getCh(), file.getMirrors());
        this.bus = new Bus(file.getRgb(), ppu, builder.gameLoopCallback);
        this.cpu = new CPU(this.bus, builder.stackRest, builder.pcReset);
    }

    public NES(byte[] rpg, byte[] ch, int pcReset, int stackReset) {
        this.file = null;
        this.errorHandler = null;
        this.ppu = new PPU(ch, 1);
        this.bus = new Bus(rpg, ppu);
        this.cpu = new CPU(this.bus, stackReset, pcReset);
    }

    public NES(byte[] rpg, byte[] ch) {
        this(rpg, ch, PC_RESET, STACK_RESET);
    }


    public void execute() {
        this.cpu.reset();
        while (!stop) {
            try {
                this.cpu.execute();
            } catch (Exception e) {
                stop = (this.errorHandler == null);
                if (!stop) {
                    stop = this.errorHandler.apply(e);
                }
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
        private Function<Throwable, Boolean> errorHandler;
        private BiConsumer<PPU, JoyPad> gameLoopCallback;

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

        public NESBuilder gameLoopCallback(BiConsumer<PPU, JoyPad> gameLoopCallback) {
            this.gameLoopCallback = gameLoopCallback;
            return this;
        }

        public NESBuilder errorHandler(Function<Throwable, Boolean> callable) {
            this.errorHandler = callable;
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
