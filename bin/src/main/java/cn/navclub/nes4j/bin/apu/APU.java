package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.core.Component;
import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.apu.impl.*;
import cn.navclub.nes4j.bin.config.CPUInterrupt;
import lombok.Getter;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;

/**
 * <a href="https://www.nesdev.org/wiki/APU">APU Document</a>
 */
public class APU implements Component {
    private static final float[] TND_TABLE;
    private static final float[] PULSE_TABLE;

    static {
        TND_TABLE = new float[203];
        PULSE_TABLE = new float[31];

        for (int i = 0; i < 203; i++) {
            if (i < 31) {
                PULSE_TABLE[i] = (float) (95.52 / ((8128.0) / (float) i + 100));
            }
            TND_TABLE[i] = (float) (163.67 / (24329.0 / (float) i + 100));
        }
    }

    @Getter
    private final NES context;
    private final DMChannel dmc;
    private final Player player;
    private final NoiseChannel noise;
    private final PulseChannel pulse1;
    private final PulseChannel pulse2;
    private final TriangleChannel triangle;
    private final FrameCounter frameCounter;

    public APU(NES context) {
        this.context = context;
        this.dmc = new DMChannel(this);
        this.noise = new NoiseChannel(this);
        this.triangle = new TriangleChannel(this);
        this.frameCounter = new FrameCounter(this);
        this.player = Player.newInstance(context.getPlayer());
        this.pulse1 = new PulseChannel(this, false);
        this.pulse2 = new PulseChannel(this, true);
    }

    @Override
    public void write(int address, byte b) {
        //
        //When $4015 is written to, the channels' length counter enable flags are set,
        //the DMC is possibly started or stopped, and the DMC's IRQ occurred flag is
        //cleared.
        //
        //    ---d nt21   DMC, noise, triangle, square 2, square 1
        //
        //If d is set and the DMC's DMA reader has no more sample bytes to fetch, the DMC
        //sample is restarted. If d is clear then the DMA reader's sample bytes remaining
        //is set to 0.
        //
        if (address == 0x4015) {

            this.pulse1.setEnable((b & 0x01) == 0x01);
            this.pulse2.setEnable((b & 0x02) == 0x02);
            this.triangle.setEnable((b & 0x04) == 0x04);
            this.noise.setEnable((b & 0x08) == 0x08);

            var dmcEnable = (b & 0x10) == 0x10;
            if (!dmcEnable) {
                this.dmc.setCurrentLength(0);
            }
            if (dmcEnable && this.dmc.getCurrentLength() == 0) {
                this.dmc.reset();
            }
            this.dmc.setEnable(dmcEnable);
            this.dmc.setIRQInterrupt(false);
        }
        //0x4000-0x4003 Square Channel1
        else if (address >= 0x4000 && address <= 0x4003) {
            this.pulse1.write(address, b);
        }
        //0x4004-0x4007  Square Channel2
        else if (address >= 0x4004 && address <= 0x4007) {
            this.pulse2.write(address, b);
        }
        //0x4008-0x400b Triangle channel
        else if (address >= 0x4008 && address <= 0x400b) {
            this.triangle.write(address, b);
        }
        //0x400c-0x400f Noise channel
        else if (address >= 0x400c && address <= 0x400f) {
            this.noise.write(address, b);
        }
        //0x4010-0x4013 DMC channel
        else if (address >= 0x4010 && address <= 0x4013) {
            this.dmc.write(address, b);
        }
        //Frame sequencer
        else if (address == 0x4017) {
            this.frameCounter.write(address, b);
            this.tick();
        }
    }

    @Override
    public byte read(int address) {
        if (address != 0x4015) {
            throw new RuntimeException("Write-only register not only read operation.");
        }
        //
        // When $4015 is read, the status of the channels' length counters and bytes
        // remaining in the current DMC sample, and interrupt flags are returned.
        // Afterwards the Frame Sequencer's frame interrupt flag is cleared.
        //    if-d nt21
        //
        //    IRQ from DMC
        //    frame interrupt
        //    DMC sample bytes remaining > 0
        //    noise length counter > 0
        //    triangle length counter > 0
        //    square 2 length counter > 0
        //    square 1 length counter > 0
        //
        var value = 0;

        var c0 = pulse1.getLengthCounter().getCounter();
        var c1 = pulse2.getLengthCounter().getCounter();
        var c2 = triangle.getLengthCounter().getCounter();
        var c3 = this.noise.getLengthCounter().getCounter();


        value |= (c0 > 0 ? 1 : 0);
        value |= (c1 > 0 ? 1 << 1 : 0);
        value |= (c2 > 0 ? 1 << 2 : 0);
        value |= (c3 > 0 ? 1 << 3 : 0);
        value |= (this.dmc.getCurrentLength() > 0 ? 1 << 4 : 0);
        value |= this.frameCounter.isInterrupt() ? 1 << 6 : 0;
        value |= this.dmc.isIRQInterrupt() ? 1 << 7 : 0;

        this.frameCounter.setInterrupt(false);

        return int8(value);
    }

    private long cycle;

    @Override
    public void tick() {
        this.cycle++;

        if (this.cycle % 2 == 0) {
            this.dmc.tick();
            this.pulse1.tick();
            this.pulse2.tick();
            this.noise.tick();
        }

        this.triangle.tick();

        this.frameCounter.tick();

        if (this.frameCounter.isOutput()) {
            var index = this.frameCounter.getIndex() - 1;
            if (index % 2 != 0) {
                this.pulse1.lengthTick();
                this.pulse2.lengthTick();
                this.noise.lengthTick();
                this.triangle.lengthTick();
            }

            this.noise.getEnvelope().tick();
            this.pulse1.getEnvelope().tick();
            this.pulse2.getEnvelope().tick();
            this.triangle.getLinearCounter().tick();
        }

        if ((this.cycle / 2) % 40 == 0) {
            var output = this.lookupSample();
            if (this.player != null && output != 0) {
                this.player.output(output);
            }
        }
        // At any time, if the interrupt flag is set, the CPU's IRQ line is continuously asserted
        // until the interrupt flag is cleared. The processor will continue on from where it was stalled.
        if (this.dmc.isIRQInterrupt()) {
            this.fireIRQ();
        }
    }

    /**
     * <pre>
     * Implementation Using Lookup Table
     * ---------------------------------
     * The formulas can be efficiently implemented using two lookup tables: a 31-entry
     * table for the two square channels and a 203-entry table for the remaining
     * channels (due to the approximation of tnd_out, the numerators are adjusted
     * slightly to preserve the normalized output range).
     *
     *     square_table [n] = 95.52 / (8128.0 / n + 100)
     *
     *     square_out = square_table [square1 + square2]
     *
     * The latter table is approximated (within 4%) by using a base unit close to the
     * DMC's DAC.
     *
     *     tnd_table [n] = 163.67 / (24329.0 / n + 100)
     *
     *     tnd_out = tnd_table [3 * triangle + 2 * noise + dmc]
     * </pre>
     */
    private float lookupSample() {
        var d0 = this.dmc.output();
        var n0 = this.noise.output();
        var p1 = this.pulse1.output();
        var p2 = this.pulse2.output();
        var t0 = this.triangle.output();

        var seqOut = PULSE_TABLE[p2 + p1];
        var tndOut = TND_TABLE[3 * t0 + 2 * n0 + d0];


        return tndOut + seqOut;
    }

    public void fireIRQ() {
        this.context.interrupt(CPUInterrupt.IRQ);
    }

    @Override
    public void stop() {
        if (this.player != null) {
            this.player.stop();
        }
    }
}
