package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.apu.APU;
import cn.navclub.nes4j.bin.apu.Player;
import cn.navclub.nes4j.bin.core.*;
import cn.navclub.nes4j.bin.core.impl.NRMapper;
import cn.navclub.nes4j.bin.core.impl.UXMapper;
import cn.navclub.nes4j.bin.debug.Debugger;
import cn.navclub.nes4j.bin.config.CPUInterrupt;
import cn.navclub.nes4j.bin.eventbus.EventBus;
import cn.navclub.nes4j.bin.function.TCallback;
import cn.navclub.nes4j.bin.io.Cartridge;
import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.Frame;
import cn.navclub.nes4j.bin.ppu.PPU;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Getter
public class NES {
    private final Bus bus;
    private final CPU cpu;
    private final APU apu;
    private final PPU ppu;
    private final Mapper mapper;
    private final Thread thread;
    private final JoyPad joyPad;
    private final JoyPad joyPad1;
    private final Cartridge cartridge;
    private final EventBus eventBus;
    private final TCallback<Frame, JoyPad, JoyPad> gameLoopCallback;
    //cpu stall cycle
    private int stall;
    @Setter
    private int speed;
    private long cycles;
    @Getter
    private long instructions;
    private Debugger debugger;
    private volatile boolean stop;
    @Getter
    private final Class<? extends Player> player;

    private NES(NESBuilder builder) {
        if (builder.buffer != null) {
            this.cartridge = new Cartridge(builder.buffer);
        } else {
            this.cartridge = new Cartridge(builder.file);
        }
        this.speed = 6;
        this.joyPad = new JoyPad();
        this.joyPad1 = new JoyPad();
        this.player = builder.player;
        this.eventBus = new EventBus();
        this.thread = Thread.currentThread();
        this.gameLoopCallback = builder.gameLoopCallback;
        this.mapper = this.cartridge.getMapper().newProvider(this.cartridge);

        this.apu = new APU(this);
        this.ppu = new PPU(this, cartridge.getChrom(), cartridge.getMirrors());

        this.bus = new Bus(this, joyPad, joyPad1);


        this.cpu = new CPU(this);
    }

    public void execute() {
        this.reset();
        while (!stop) {
            this.execute0();
        }
    }

    private void execute0() {
        var cycles = 0;
        if (this.stall > 0) {
            this.stall--;
            cycles = 1;
        } else {
            if (this.debugger != null && this.debugger.hack(this)) {
                //lock current program process
                LockSupport.park();
            }
            cycles += this.cpu.next();
            this.instructions++;
        }
        for (int i = 0; i < cycles; i++) {
            this.apu.tick();
            this.ppu.tick();
        }
        this.cycles += cycles;
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
     * Reset NES all core component
     */
    public void reset() {
        //Reset release debug lock
        this.release();
        this.cycles = 0;
        this.apu.reset();
        this.ppu.reset();
        this.cpu.reset();
    }

    /**
     * {@link APU} AND {@link  PPU} trigger IRQ AND NMI interrupt.
     *
     * @param interrupt interrupt type
     */
    public void interrupt(CPUInterrupt interrupt) {
        this.cpu.interrupt(interrupt);
    }

    public void videoOutput(Frame frame) {
        LockSupport.parkNanos(this.speed * 1000000L);
        if (this.gameLoopCallback != null) {
            this.gameLoopCallback.accept(frame, this.joyPad, this.joyPad1);
            frame.clear();
        }
    }

    public void setStall(int stall) {
        this.stall += stall;
    }


    public void stop() {
        this.stop = true;
        this.bus.stop();
        LockSupport.unpark(this.thread);
    }

    /**
     * 在debug模式下用于执行下一个断点所用
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
        private Class<? extends Player> player;
        private TCallback<Frame, JoyPad, JoyPad> gameLoopCallback;

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

        public NESBuilder player(Class<? extends Player> clazz) {
            this.player = clazz;
            return this;
        }

        public NESBuilder gameLoopCallback(TCallback<Frame, JoyPad, JoyPad> gameLoopCallback) {
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
