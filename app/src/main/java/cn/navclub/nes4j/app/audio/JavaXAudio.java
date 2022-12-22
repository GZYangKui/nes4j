package cn.navclub.nes4j.app.audio;

import cn.navclub.nes4j.bin.apu.Player;

import javax.sound.sampled.*;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;

@SuppressWarnings("all")
public class JavaXAudio implements Player {
    private final byte[] sample;
    private final Line.Info info;
    private final AudioFormat format;
    private final SourceDataLine line;

    private int index;


    public JavaXAudio() throws LineUnavailableException {
        this.sample = new byte[735 * 2];
        this.format = new AudioFormat(44100, 8, 1, false, false);
        this.info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) AudioSystem.getLine(info);

        line.open(format);
        line.start();
    }

    @Override
    public void output(float sample) {
        this.sample[this.index++] = int8(Math.round(sample * 0xff));
        if (this.index == this.sample.length) {
            this.index = 0;
            var s = this.line.write(this.sample, 0, this.sample.length);
        }
    }

    @Override
    public void stop() {
        this.line.close();
    }
}
