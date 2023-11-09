package cn.navclub.nes4j.bin.config;

/**
 * Enum variable audio sample rate
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public enum AudioSampleRate {
    HZ11025(11025),
    HZ22050(22050),
    HZ44100(44100),
    HZ48000(48000),
    HZ96000(96000);

    public final int value;
    public final int sample;

    AudioSampleRate(int sample) {
        this.sample = sample;
        // 1.79 MHz / sample = value
        this.value = Math.floorDiv(1790000, sample);
    }
}
