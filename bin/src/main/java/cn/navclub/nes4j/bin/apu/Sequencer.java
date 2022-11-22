package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;

public interface Sequencer extends CycleDriver {
    /**
     *
     * 获取当前序列器生成的值
     *
     */
    int value();
}
