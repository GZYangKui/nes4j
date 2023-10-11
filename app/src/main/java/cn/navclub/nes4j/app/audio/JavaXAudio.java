package cn.navclub.nes4j.app.audio;

import cn.navclub.nes4j.bin.apu.Player;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;

import javax.sound.sampled.*;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;

@SuppressWarnings("all")
public class JavaXAudio implements Player {
    private final byte[] sample;
    private final Line.Info info;
    private final AudioFormat format;
    private final SourceDataLine line;
    private int ldx;
    //Current fill index
    private int index;
    private Thread thread;
    private volatile boolean stop;
    private final static int SAMPLE_SIZE = 735 * 2;
    //Audio buffer size default 32kb
    private final static int DEF_BUF_SIZE = 32 * 1024;

    private static final LoggerDelegate log = LoggerFactory.logger(JavaXAudio.class);


    public JavaXAudio() throws LineUnavailableException {
        this.sample = new byte[DEF_BUF_SIZE];
        this.format = new AudioFormat(44100, 8, 1, false, false);
        this.info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(format);
        line.start();

        CompletableFuture.runAsync((this::exec));
    }

    @Override
    public synchronized void output(byte sample) {
        this.sample[this.index++] = sample;
        if (this.lcalculate() > SAMPLE_SIZE && thread != null) {
            LockSupport.unpark(this.thread);
        }
        index = index % DEF_BUF_SIZE;
    }


    private void exec() {
        var arr = new byte[DEF_BUF_SIZE];
        this.thread = Thread.currentThread();
        while (!this.stop) {
            LockSupport.park();
            final int length;
            synchronized (this) {
                length = lcalculate();
                if ((length + ldx > DEF_BUF_SIZE)) {
                    var tmp = DEF_BUF_SIZE - this.ldx;
                    System.arraycopy(this.sample, this.ldx, arr, 0, tmp);
                    System.arraycopy(this.sample, 0, arr, tmp, this.index);
                } else {
                    System.arraycopy(this.sample, this.ldx, arr, 0, length);
                }
                this.ldx = this.index;
            }
            this.line.write(arr, 0, length);
        }
    }

    private int lcalculate() {
        var len = this.index - this.ldx;
        if (len > 0) {
            return len;
        }
        return DEF_BUF_SIZE - ldx + index;
    }


    @Override
    public void stop() {
        this.stop = true;
        LockSupport.unpark(this.thread);
        this.line.close();
    }
}
