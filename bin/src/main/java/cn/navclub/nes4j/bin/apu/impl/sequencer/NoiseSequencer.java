package cn.navclub.nes4j.bin.apu.impl.sequencer;

import cn.navclub.nes4j.bin.apu.Sequencer;
import lombok.Setter;

/**
 *
 *
 *
 * bit:  14 13 12 11 10 9 8 7 6 4 3 2 1     0
 *        ^                   |       |     |
 *        |                   v       v     |
 *        |                 \"1"""""""0"/   |
 *        |     $400E.7 ---->\   Mux   /    |
 *        |                   \_______/     |
 *        |                       |         |
 *        |       /"""""//<-------'         |
 *        `------( XOR ((                   |
 *                \_____\\<-----------------'
 *
 *
 */
public class NoiseSequencer implements Sequencer {
    @Setter
    private int mode;

    @Override
    public int value() {
        return this.mode;
    }

    @Override
    public void tick(int cycle) {}
}
