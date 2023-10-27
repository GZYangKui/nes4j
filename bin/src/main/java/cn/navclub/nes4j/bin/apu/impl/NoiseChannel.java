package cn.navclub.nes4j.bin.apu.impl;

import cn.navclub.nes4j.bin.apu.Channel;
import cn.navclub.nes4j.bin.apu.Envelope;
import cn.navclub.nes4j.bin.apu.impl.sequencer.NoiseSequencer;
import cn.navclub.nes4j.bin.apu.APU;
import lombok.Getter;

/**
 * <h1>Noise channel</h1>
 * <pre>
 *     +---------+    +---------+    +---------+
 *     |  Timer  |--->| Random  |    | Length  |
 *     +---------+    +---------+    +---------+
 *                         |              |
 *                         v              v
 *     +---------+        |\             |\         +---------+
 *     |Envelope |------->| >----------->| >------->|   DAC   |
 *     +---------+        |/             |/         +---------+
 * </pre>
 */
@Getter
public class NoiseChannel extends Channel<NoiseSequencer> {
    private static final int[] LOOK_TABLE = {
            0x004,
            0x008,
            0x010,
            0x020,
            0x040,
            0x060,
            0x080,
            0x0a0,
            0x0ca,
            0x0fe,
            0x17c,
            0x1fc,
            0x2fa,
            0x3f8,
            0x3f2,
            0xfe4
    };
    private final Envelope envelope;

    public NoiseChannel(APU apu) {
        super(apu, new NoiseSequencer());
        this.envelope = new Envelope();
    }

    @Override
    public void write(int address, byte b) {
        if (address == 0x400c) {
            this.envelope.update(b);

            if (this.envelope.shareFBit()) {
                this.lengthCounter.setHalt((b & 0x20) != 0);
            }
        }
        //
        // Register $400E sets the random generator mode and timer period based on a 4-bit
        // index into a period table:
        //
        //    m--- iiii       mode, period index
        //
        //    i   timer period
        //    ----------------
        //    0     $004
        //    1     $008
        //    2     $010
        //    3     $020
        //    4     $040
        //    5     $060
        //    6     $080
        //    7     $0A0
        //    8     $0CA
        //    9     $0FE
        //    A     $17C
        //    B     $1FC
        //    C     $2FA
        //    D     $3F8
        //    E     $7F2
        //    F     $FE4
        //
        if (address == 0x400e) {
            var index = b & 0x0f;
            var mode = (b & 0x80) >> 7;
            this.timer.setPeriod(LOOK_TABLE[index]);
            this.sequencer.setMode(mode);
        }

        if (address == 0x400f) {
            this.envelope.reset();
            if (this.enable) {
                this.lengthCounter.setCounter(b >>> 3);
            }
        }
    }

    /**
     * <p> The mixer receives the current envelope volume except when</p>
     *
     * <li>Bit 0 of the shift register is set, or</li>
     * <li>The length counter is zero</li>
     * <li>Within the mixer, the DMC level has a noticeable effect on the noise's level.</li>
     */
    @Override
    public int output() {
        if (!this.enable
                || this.sequencer.value() == 1
                || this.lengthCounter.getCounter() == 0) {
            return 0;
        }
        return this.envelope.getVolume();
    }

    @Override
    public int readState() {
        return this.lengthCounter.stateVal() << 3;
    }
}
