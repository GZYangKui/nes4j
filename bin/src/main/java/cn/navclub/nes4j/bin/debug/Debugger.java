package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.CPU;

/**
 * NES instance debugger function
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public interface Debugger {
    /**
     * When {@link CPU#next()} was executed before call
     *
     * @return If return {@code true} block current thread,otherwise do nothing.
     */
    boolean hack(NES context);

    /**
     * When {@link NES} instance rpg-rom data happen change call
     *
     * @param buffer Change after rpg data
     */
    void buffer(byte[] buffer);

    /**
     * When {@link NES} wsa created  call
     *
     * @param instance {@link NES} instance
     */
    void inject(NES instance);
}
