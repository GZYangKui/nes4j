package cn.navclub.nes4j.bin.apu;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public interface Player {
    /**
     * When apu component product a sample will call this method
     *
     * @param sample Audio sample
     */
    void output(byte sample);

    /**
     * When game was close will call this method release resource
     */
    default void stop() {

    }


    default void reset() {

    }

    static Player newInstance(Class<? extends Player> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
