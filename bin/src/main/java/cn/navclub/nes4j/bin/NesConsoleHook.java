package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import cn.navclub.nes4j.bin.ppu.Frame;

public interface NesConsoleHook {
    LoggerDelegate LOG = LoggerFactory.logger(NesConsoleHook.class);

    /**
     * Game loop
     */
    void callback(Integer fps, boolean enableRender, Frame frame, JoyPad joyPad, JoyPad joyPad1);

    /**
     * If nes rom use emulator custom logger instruction call this function
     *
     * @param tStr  String template
     * @param value Eval value
     */
    default void logger(String tStr, String value) {
        LOG.info("Emulator say:{}", value);
    }
}
