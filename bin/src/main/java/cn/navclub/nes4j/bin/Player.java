package cn.navclub.nes4j.bin;

@FunctionalInterface
public interface Player {
    /**
     *
     * When apu component product a sample will call this method
     *
     * @param sample Audio sample
     */
    void output(float sample);
}
