package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.apu.Player;
import cn.navclub.nes4j.bin.config.AudioSampleRate;
import cn.navclub.nes4j.bin.config.ICPUStatus;
import cn.navclub.nes4j.bin.core.*;
import cn.navclub.nes4j.bin.debug.Debugger;
import cn.navclub.nes4j.bin.config.CPUInterrupt;
import cn.navclub.nes4j.bin.function.FCallback;
import cn.navclub.nes4j.bin.io.Cartridge;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.Frame;
import cn.navclub.nes4j.bin.ppu.PPU;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

@Getter
public class NesConsole {
    private final CPU cpu;
    private final APU apu;
    private final PPU ppu;
    private final MemoryBus bus;
    private final Mapper mapper;
    private final Thread thread;
    private final JoyPad joyPad;
    private final JoyPad joyPad1;
    private final Cartridge cartridge;
    //Game loop callback function(Timestamp of frame occur,Pixel data,Render enable,Joy1,Joy2)
    private final FCallback<Long, Boolean, Frame, JoyPad, JoyPad> gameLoopCallback;
    //cpu stall cycle
    private int stall;
    @Getter
    private int speed;
    //APU mute
    @Setter
    @Getter
    private boolean mute;
    private long cycles;
    private long dcycle;
    private Debugger debugger;
    private volatile boolean stop;
    private volatile boolean reset;
    @SuppressWarnings("all")
    private BlockingQueue<CPUInterrupt> queue;
    @Getter
    private final Class<? extends Player> player;

    private NesConsole(Builder builder) {
        if (builder.buffer != null) {
            this.cartridge = new Cartridge(builder.buffer);
        } else {
            this.cartridge = new Cartridge(builder.file);
        }
        this.speed = 60;
        this.mute = false;
        this.reset = true;
        this.joyPad = new JoyPad();
        this.joyPad1 = new JoyPad();
        this.player = builder.player;
        this.thread = Thread.currentThread();
        this.queue = new LinkedBlockingQueue<>(10);
        this.gameLoopCallback = builder.gameLoopCallback;
        this.mapper = this.cartridge.getMapper().newProvider(this.cartridge, this);

        this.apu = new APU(builder.sampleRate, this);
        this.ppu = new PPU(this, cartridge.getMirrors());
        this.bus = new MemoryBus(this, joyPad, joyPad1);


        this.cpu = new CPU(this);
    }

    public void execute() {
        while (!stop) {
            //Check if reset flag was set and execute reset logic
            if (this.reset) {
                this.reset();
            }
            CPUInterrupt interrupt;
            while ((interrupt = queue.poll()) != null) {
                this.stall += this.cpu.NMI_IRQ_BRKInterrupt(interrupt);
            }
            this.execute0();
        }
    }

    private void execute0() {
        var tmp = this.stall;
        if (tmp == 0) {
            //Test line number has break point and block game loop
            if (this.debugger != null && this.debugger.hack(this)) {
                LockSupport.park();
                this.dcycle = 0;
            }
            tmp = this.cpu.next();
            this.cycles += this.cpu.getCycle();
            this.dcycle += this.cpu.getCycle();
        } else {
            this.stall = 0;
        }
        while (--tmp >= 0) {
            this.apu.tick();
            this.ppu.tick();
        }
    }

    public void setDebugger(Debugger debugger) {
        this.debugger = debugger;
        if (this.debugger != null) {
            this.debugger.inject(this);
            CompletableFuture.runAsync(() -> this.debugger.buffer(cartridge.getRgbrom()));
        }
    }

    /**
     * This is util method,read target address memory value
     *
     * @param address Memory address
     * @return Memory address value
     */
    public byte I8Read(int address) {
        return this.bus.read(address);
    }


    /**
     * Software reset NES all core component
     */
    public void SWReset() {
        this.reset = true;
    }

    private void reset() {
        this.stall = 0;
        this.dcycle = 0;
        this.cycles = 0;
        this.apu.reset();
        this.ppu.reset();
        this.cpu.reset();
        this.bus.reset();
        this.reset = false;
    }

    /**
     * {@link APU} AND {@link  PPU} trigger IRQ AND NMI interrupt.
     *
     * @param interrupt interrupt type
     */
    public void hardwareInterrupt(CPUInterrupt interrupt) {
        this.queue.add(interrupt);
    }


    public void videoOutput(long nano, boolean renderEnable, Frame frame) {
        if (gameLoopCallback == null) {
            return;
        }
        this.gameLoopCallback.accept(nano, renderEnable, frame, this.joyPad, this.joyPad1);
    }

    public void setStall(int span) {
        this.stall += span;
        this.dcycle += span;
    }


    public void stop() {
        this.stop = true;
        this.bus.stop();
        this.apu.stop();
        this.ppu.stop();
        LockSupport.unpark(this.thread);
    }

    /**
     * Execute next break line
     */
    public synchronized void release() {
        if (this.thread == null) {
            return;
        }
        LockSupport.unpark(this.thread);
    }

    /**
     * Manual change emulator speed
     *
     * @param span offset value
     */
    public void speed(int span) {
        var tmp = this.speed + span;
        if (tmp < 0) {
            tmp = 0;
        }
        this.speed = tmp;
    }


    public static class Builder {
        private File file;
        private byte[] buffer;
        private AudioSampleRate sampleRate;
        private Class<? extends Player> player;
        private FCallback<Long, Boolean, Frame, JoyPad, JoyPad> gameLoopCallback;

        public Builder buffer(byte[] buffer) {
            this.buffer = buffer;
            return this;
        }

        public Builder file(File file) {
            this.file = file;
            return this;
        }

        public Builder file(String file) {
            this.file = new File(file);
            return this;
        }

        public Builder player(Class<? extends Player> clazz) {
            this.player = clazz;
            return this;
        }

        public Builder gameLoopCallback(FCallback<Long, Boolean, Frame, JoyPad, JoyPad> gameLoopCallback) {
            this.gameLoopCallback = gameLoopCallback;
            return this;
        }

        public Builder sampleRate(AudioSampleRate sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public NesConsole build() {
            return new NesConsole(this);
        }

        public static Builder newBuilder() {
            return new Builder();
        }
    }
}
