package cn.navclub.nes4j.app.audio;

import cn.navclub.nes4j.bin.apu.Player;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;

import javax.sound.sampled.*;
import java.util.concurrent.locks.LockSupport;


@SuppressWarnings("all")
public class JavaXAudio implements Player {
    private final byte[] sample;
    private final byte[] buffer;
    private final Line.Info info;
    private final AudioFormat format;
    private final SourceDataLine line;
    private int ldx;
    //Current fill index
    private int index;
    private final Thread thread;
    private volatile boolean stop;
    private final static int SAMPLE_SIZE = 55;

    private static final LoggerDelegate log = LoggerFactory.logger(JavaXAudio.class);


    public JavaXAudio(Integer sampleRate) throws LineUnavailableException {
        this.sample = new byte[SAMPLE_SIZE];
        this.buffer = new byte[SAMPLE_SIZE];
        this.thread = new Thread(this::exec);
        this.format = new AudioFormat(sampleRate, 8, 1, false, false);
        this.info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(format);
        line.start();

        this.thread.start();
    }

    @Override
    public void output(byte sample) {
        this.buffer[this.index] = sample;
        this.index++;
        if (this.index == SAMPLE_SIZE) {
            this.index = 0;
            System.arraycopy(this.buffer, 0, this.sample, 0, SAMPLE_SIZE);
            LockSupport.unpark(this.thread);
        }
        this.index %= SAMPLE_SIZE;
    }


    private void exec() {
        while (!this.stop) {
            LockSupport.park();
            this.line.write(this.sample, 0, SAMPLE_SIZE);
        }
    }

    @Override
    public void stop() {
        this.stop = true;
        LockSupport.unpark(this.thread);
        this.line.close();
    }

    @Override
    public void reset() {
        this.index = 0;
    }
}
