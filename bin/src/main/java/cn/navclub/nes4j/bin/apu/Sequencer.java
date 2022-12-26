package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;

/**
 * A sequencer continuously loops over a sequence of values or events. When clocked, the next item in the sequence
 * is generated. In this APU documentation, clocking a sequencer usually means either advancing to the next step in
 * a waveform, or the event sequence of the Frame Counter device.
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public interface Sequencer extends CycleDriver {
    /**
     * Current sequencer value
     *
     * @return Return current sequencer value
     */
    int value();
}
