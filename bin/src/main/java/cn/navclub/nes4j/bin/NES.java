package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.core.*;
import cn.navclub.nes4j.bin.enums.NameMirror;
import cn.navclub.nes4j.bin.function.TCallback;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.function.Consumer;

@Slf4j
@Getter
public class NES {
    //程序计数器重置地址
    private static final int PC_RESET = 0xfffc;
    //程序栈重置地址
    private static final int STACK_RESET = 0xfd;

    private final Bus bus;
    private final CPU cpu;
    private final PPU ppu;
    private final NESFile file;
    @Getter
    private final JoyPad joyPad;
    @Getter
    private final JoyPad joyPad1;
    @Setter
    private volatile boolean stop;
    private final Consumer<Throwable> errorHandler;


    private NES(NESBuilder builder) {
        if (builder.buffer != null) {
            this.file = new NESFile(builder.buffer);
        } else {
            this.file = new NESFile(builder.file);
        }
        this.joyPad = new JoyPad();
        this.joyPad1 = new JoyPad();
        this.errorHandler = builder.errorHandler;
        this.ppu = new PPU(file.getCh(), file.getMirrors());
        this.bus = new Bus(file.getMapper(), file.getRgb(), ppu, builder.gameLoopCallback, joyPad, joyPad1);
        this.cpu = new CPU(this.bus, builder.stackRest, builder.pcReset);
    }

    public NES(byte[] rpg, byte[] ch, int pcReset, int stackReset) {
        this.file = null;
        this.errorHandler = null;
        this.joyPad = new JoyPad();
        this.joyPad1 = new JoyPad();
        this.ppu = new PPU(ch, NameMirror.VERTICAL);
        this.bus = new Bus(rpg, ppu, joyPad, joyPad1);
        this.cpu = new CPU(this.bus, stackReset, pcReset);
    }

    public NES(byte[] rpg, byte[] ch) {
        this(rpg, ch, PC_RESET, STACK_RESET);
    }


    public void execute() {
        this.cpu.reset();
        while (!stop) {
            try {
                this.cpu.next();
            } catch (Exception e) {
                if (this.errorHandler != null) {
                    this.stop = true;
                    this.errorHandler.accept(e);
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void test(int pcStart) {
        this.cpu.setPc(pcStart);
        while (loop()) {
            this.cpu.next();
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
        private Consumer<Throwable> errorHandler;
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

        public NESBuilder errorHandler(Consumer<Throwable> callable) {
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
