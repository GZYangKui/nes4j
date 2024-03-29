package cn.navclub.nes4j.bin.apu.impl.sequencer;

import cn.navclub.nes4j.bin.apu.Sequencer;
import lombok.Setter;

/**
 * <pre>
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
 * The shift register is clocked by the timer and the vacated bit 14 is filled
 * with the exclusive-OR of *pre-shifted* bits 0 and 1 (mode = 0) or bits 0 and 6
 * (mode = 1), resulting in 32767-bit and 93-bit sequences, respectively.
 * </pre>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class NoiseSequencer implements Sequencer {
    @Setter
    private int mode;

    private int sequence;

    public NoiseSequencer() {
        this.reset();
    }

    @Override
    public int value() {
        return this.sequence & 0x01;
    }

    /**
     * <pre>
     * The shift register is 15 bits wide, with bits numbered
     * 14 - 13 - 12 - 11 - 10 - 9 - 8 - 7 - 6 - 5 - 4 - 3 - 2 - 1 - 0
     *
     * When the timer clocks the shift register, the following actions occur in order:
     *
     * Feedback is calculated as the exclusive-OR of bit 0 and one other bit: bit 6 if Mode flag is set, otherwise bit 1.
     * The shift register is shifted right by one bit.
     * Bit 14, the leftmost bit, is set to the feedback calculated earlier.
     * This results in a pseudo-random bit sequence, 32767 steps long when Mode flag is clear,
     * and randomly 93 or 31 steps long otherwise. (The particular 31- or 93-step sequence depends on where in the 32767-step sequence the shift register was when Mode flag was set).
     * </pre>
     */
    @Override
    public void tick() {
        var index = this.mode == 0 ? 1 : 6;
        var a = this.sequence & 0x01;
        var b = (this.sequence >> index) & 0x01;
        this.sequence >>= 1;
        this.sequence |= ((a ^ b) << 14);
    }

    @Override
    public void reset() {
        this.mode = 0;
        //On power-up, the shift register is loaded with the value 1.
        this.sequence = 1;
    }
}
