package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.function.CycleDriver;

/**
 * Abstract nes system core component common function.
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public interface Component extends CycleDriver {
    /**
     * Write a byte to address
     */
    void write(int address, byte b);

    /**
     * Read a byte from address
     */
    byte read(int address);

    /**
     * When {@link cn.navclub.nes4j.bin.NES} instance stop call
     */
    default void stop() {

    }

    /**
     * When {@link cn.navclub.nes4j.bin.NES} was reset call
     */
    default void reset() {

    }

    /**
     * Snapshot current component status info
     *
     * @return Current status info data
     */
    default byte[] snapshot() {
        return null;
    }

    /**
     * Recovery current component to target status
     *
     * @param snapshot Status data
     */
    default void load(byte[] snapshot) {

    }
}
