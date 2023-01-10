package cn.navclub.nes4j.app.audio;

import cn.navclub.nes4j.bin.apu.Player;

import javax.sound.sampled.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;

@SuppressWarnings("all")
public class JavaXAudio implements Player {
    private final byte[] sample;
    private final Line.Info info;
    private final AudioFormat format;
    private final SourceDataLine line;

    private int index;
    private Thread thread;
    private volatile boolean play;
    private volatile boolean stop;


    public JavaXAudio() throws LineUnavailableException {
        this.sample = new byte[735 * 2];
        this.format = new AudioFormat(44100, 8, 1, false, false);
        this.info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(format);
        line.start();

        CompletableFuture.runAsync((this::exec));
    }

    @Override
    public void output(float sample) {
        if (this.play) {
            return;
        }
        var value = int8(Math.round(sample * 0xff));
        this.sample[this.index++] = value;
        if (this.index == this.sample.length) {
            this.index = 0;
            LockSupport.unpark(this.thread);
            this.play = true;
        }
    }


    private void exec() {
        this.thread = Thread.currentThread();
        while (!this.stop) {
            LockSupport.park();
            this.line.write(this.sample, 0, this.sample.length);
            this.play = false;
        }
    }

    @Override
    public void stop() {
        this.stop = true;
        LockSupport.unpark(this.thread);
        this.line.close();
    }
}
