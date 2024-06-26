package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.apu.Player;
import cn.navclub.nes4j.bin.config.AudioSampleRate;
import cn.navclub.nes4j.bin.config.NMapper;
import cn.navclub.nes4j.bin.config.TV;
import cn.navclub.nes4j.bin.core.*;
import cn.navclub.nes4j.bin.debug.Debugger;
import cn.navclub.nes4j.bin.config.CPUInterrupt;
import cn.navclub.nes4j.bin.function.GameLoopCallback;
import cn.navclub.nes4j.bin.io.Cartridge;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.Frame;
import cn.navclub.nes4j.bin.ppu.PPU;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

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

    private int fps;
    private int tfps;
    //cpu stall cycle
    private int stall;
    //APU mute
    @Setter
    @Getter
    private boolean mute;
    private Debugger debugger;
    private long lastFrameTime;
    private volatile boolean stop;
    private volatile boolean reset;
    @SuppressWarnings("all")
    private BlockingQueue<CPUInterrupt> queue;
    private final NesConsoleHook hook;
    @Getter
    private final Class<? extends Player> player;

    private NesConsole(Builder builder) {
        if (builder.buffer != null) {
            this.cartridge = new Cartridge(builder.buffer);
        } else {
            this.cartridge = new Cartridge(builder.file);
        }
        this.mute = false;
        this.reset = true;
        this.hook = builder.hook;
        this.joyPad = new JoyPad();
        this.joyPad1 = new JoyPad();
        this.player = builder.player;
        this.thread = Thread.currentThread();
        this.queue = new LinkedBlockingQueue<>(10);
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
            var tmp = this.stall;
            this.stall = 0;
            while (tmp-- > 0) {
                this.APU_PPuSync();
            }
            //Test line number has break point and block game loop
            if (this.debugger != null && this.debugger.hack(this)) {
                LockSupport.park();
            }
            this.cpu.next();
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
        this.fps = 0;
        this.tfps = 0;
        this.stall = 0;
        this.apu.reset();
        this.ppu.reset();
        this.cpu.reset();
        this.bus.reset();
        this.reset = false;
        this.lastFrameTime = 0;
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
        //Due to hook design immutable if hook was null direct return?
        if (hook == null) {
            return;
        }
        this.tfps++;
        if (this.lastFrameTime == 0) {
            this.lastFrameTime = nano;
        }
        var span = nano - this.lastFrameTime;
        if (span > 1000_000_000) {
            this.fps = this.tfps;
            this.tfps = 0;
            this.lastFrameTime = nano;
        }
        this.hook.callback(this.fps, renderEnable, frame, this.joyPad, this.joyPad1);
    }

    public void setStall(int span) {
        this.stall += span;
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

    public int TVFps() {
        return this.cartridge.getTv() == TV.NTSC ? 60 : 50;
    }

    public void APU_PPuSync() {
        this.apu.tick();
        this.ppu.tick();
    }


    public static class Builder {
        private File file;
        private byte[] buffer;
        private NesConsoleHook hook;
        private AudioSampleRate sampleRate;
        private Class<? extends Player> player;

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

        public Builder sampleRate(AudioSampleRate sampleRate) {
            this.sampleRate = sampleRate;
            return this;
        }

        public Builder hook(NesConsoleHook hook) {
            this.hook = hook;
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
