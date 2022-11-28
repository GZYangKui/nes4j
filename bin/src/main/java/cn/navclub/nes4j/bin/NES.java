package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.core.*;
import cn.navclub.nes4j.bin.debug.Debugger;
import cn.navclub.nes4j.bin.enums.NameMirror;
import cn.navclub.nes4j.bin.function.TCallback;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Getter
public class NES {
    static {
        System.loadLibrary("nes4j");
    }

    private final Bus bus;
    private final CPU cpu;
    private final Thread thread;
    private final Cartridge cartridge;
    private final Debugger debugger;

    private volatile boolean stop;


    private NES(NESBuilder builder) {
        if (builder.buffer != null) {
            this.cartridge = new Cartridge(builder.buffer);
        } else {
            this.cartridge = new Cartridge(builder.file);
        }

        this.debugger = builder.debugger;
        this.thread = Thread.currentThread();
        this.bus = new Bus(this.cartridge, builder.gameLoopCallback);
        this.cpu = new CPU(this.bus);

        if (this.debugger != null) {
            this.debugger.inject(this);
            CompletableFuture.runAsync(() -> this.debugger.buffer(cartridge.getRgbrom(), 0));
        }
    }

    public void execute() {
        this.cpu.reset();
        while (!stop) {
            if (this.bus.getStall() > 0)
                this.bus.tick(1);
            else {
                //fire ppu or apu interrupt
                this.cpu.interrupt(this.bus.getInterrupt());
                var programCounter = this.cpu.getPc();
                if (this.debugger != null && this.debugger.hack(this.bus)) {
                    //lock current program process
                    LockSupport.park();
                }
                //Check program counter whether in legal memory area
                if (programCounter < 0x8000 || programCounter >= 0x10000) {
                    throw new RuntimeException("Text memory area except in 0x8000 to 0xffff current 0x" + Integer.toHexString(programCounter));
                }
                this.cpu.next();
            }
        }
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
