package cn.navclub.nes4j.bin.function;

import cn.navclub.nes4j.bin.io.JoyPad;
import cn.navclub.nes4j.bin.ppu.Frame;

/**
 * Game loop callback
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
@FunctionalInterface
public interface GameLoopCallback {
    void accept(Integer a, Boolean b, Frame c, JoyPad d, JoyPad e);
}
