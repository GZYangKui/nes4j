package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.core.*;
import cn.navclub.nes4j.bin.debug.Debugger;
import cn.navclub.nes4j.bin.config.CPUInterrupt;
import cn.navclub.nes4j.bin.function.TCallback;
import cn.navclub.nes4j.bin.io.Cartridge;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.PPU;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Getter
public class NES {

    private final Bus bus;
    private final CPU cpu;
    private final APU apu;
    private final PPU ppu;
    private final Thread thread;
    private final JoyPad joyPad;
    private final JoyPad joyPad1;
    private final Cartridge cartridge;
    private final Debugger debugger;
    private final TCallback<PPU, JoyPad, JoyPad> gameLoopCallback;

    //CPU延迟时钟
    private int stall;
    @Setter
    private int speed;
    private long cycles;
    @Getter
    private long instructions;
    private volatile boolean stop;
    private CPUInterrupt interrupt;
    @Getter
    private final Class<? extends Player> player;


    private NES(NESBuilder builder) {
        if (builder.buffer != null) {
            this.cartridge = new Cartridge(builder.buffer);
        } else {
            this.cartridge = new Cartridge(builder.file);
        }
        this.speed = 5;
        this.joyPad = new JoyPad();
        this.joyPad1 = new JoyPad();
        this.player = builder.player;
        this.debugger = builder.debugger;
        this.thread = Thread.currentThread();

        this.gameLoopCallback = builder.gameLoopCallback;

        this.apu = new APU(this);
        this.ppu = new PPU(this, cartridge.getChrom(), cartridge.getMirrors());

        this.bus = new Bus(this, joyPad, joyPad1);


        this.cpu = new CPU(this);

        if (this.debugger != null) {
            this.debugger.inject(this);
            CompletableFuture.runAsync(() -> this.debugger.buffer(cartridge.getRgbrom()));
        }
    }

    public void execute() {
        this.cpu.reset();
        while (!stop) {
            final int cycles;
            if (this.stall > 0) {
                this.stall--;
                cycles = 1;
            } else {
                //fire ppu or apu interrupt
                this.cpu.interrupt(this.getInterrupt());
                var programCounter = this.cpu.getPc();
                if (this.debugger != null && this.debugger.hack(this)) {
                    //lock current program process
                    LockSupport.park();
                }
                //Check program counter whether in legal memory area
                if (programCounter < 0x8000 || programCounter >= 0x10000) {
                    throw new RuntimeException("Text memory area except in 0x8000 to 0xffff current 0x" + Integer.toHexString(programCounter));
                }
                cycles = this.cpu.next();
                this.instructions++;
            }
            for (int i = 0; i < cycles; i++) {
                this.apu.tick();
                for (int j = 0; j < 3; j++) {
                    this.ppu.tick();
                }
            }
            this.cycles += cycles;
        }
    }

    public CPUInterrupt getInterrupt() {
        var temp = interrupt;
        if (temp != null) {
            this.interrupt = null;
        }
        return temp;
    }

    /**
     *
     * {@link APU} AND {@link  PPU} trigger IRQ AND NMI interrupt.
     *
     * @param interrupt interrupt type
     */
    public void interrupt(CPUInterrupt interrupt) {
        if (interrupt == CPUInterrupt.NMI && this.gameLoopCallback != null) {
            try {
                Thread.sleep(this.speed);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            this.gameLoopCallback.accept(this.ppu, this.joyPad, this.joyPad1);
        }
        if (this.interrupt == CPUInterrupt.NMI) {
            return;
        }
        this.interrupt = interrupt;
    }

    public void setStall(int stall) {
        this.stall += stall;
    }


    public void stop() {
        this.stop = true;
        this.bus.stop();
    }

    /**
     *
     * 在debug模式下用于执行下一个断点所用
     *
     */
    public synchronized void release() {
        if (this.thread == null) {
            return;
        }
        LockSupport.unpark(this.thread);
    }

    public static class NESBuilder {
        private File file;
        private byte[] buffer;
        private Debugger debugger;
        private Class<? extends Player> player;
        private TCallback<PPU, JoyPad, JoyPad> gameLoopCallback;

        public NESBuilder buffer(byte[] buffer) {
            this.buffer = buffer;
            return this;
        }

        public NESBuilder debugger(Debugger debugger) {
            this.debugger = debugger;
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

        public NESBuilder player(Class<? extends Player> clazz) {
            this.player = clazz;
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
