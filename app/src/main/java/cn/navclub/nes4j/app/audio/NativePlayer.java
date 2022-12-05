package cn.navclub.nes4j.app.audio;

import cn.navclub.nes4j.bin.apu.Player;

public class NativePlayer implements Player {
    private final float[] samples;
    private int index;

    public NativePlayer() {
        this.samples = new float[735*2];
        this.config("default", 1, 44100, 50000);
    }

    @Override
    public void output(float sample) {
        this.samples[this.index++] = sample;
        if (this.index >= this.samples.length) {
            this.index = 0;
            this.play(samples);
        }
    }

    /**
     * 调用Native模块关闭音频相关资源
     */
    public synchronized native void stop();

    /**
     * 调用Native模块播放音频样本
     *
     * @param samples 音频样本
     * @return 返回播放样本数量
     */
    private native long play(float[] samples);

    /**
     * 配置音频输出
     *
     * @param latency 延迟时长
     * @param device 输出设备
     * @param rate 音频采样率
     * @param channel 音频输出通道
     */
    private synchronized native void config(String device, int channel, int rate, int latency);
}
