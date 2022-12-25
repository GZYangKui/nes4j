package cn.navclub.nes4j.bin.config;

import cn.navclub.nes4j.bin.core.Bus;
import cn.navclub.nes4j.bin.core.CPU;
import lombok.Getter;
import lombok.Setter;

import static cn.navclub.nes4j.bin.util.MathUtil.u8add;

/**
 * <a href="http://www.emulator101.com/6502-addressing-modes.html">6502 address mode</a>
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class AddrMProvider {
    private final Bus bus;
    private final CPU cpu;
    @Getter
    @Setter
    private int cycles;


    public AddrMProvider(CPU cpu, Bus bus) {
        this.cpu = cpu;
        this.bus = bus;
    }

    public int getAbsAddr(AddressMode mode) {
        var regX = this.cpu.getRx();
        var regY = this.cpu.getRy();
        var proCounter = this.cpu.getPc();
        return switch (mode) {
            case Immediate -> proCounter;
            case ZeroPage -> this.bus.ReadU8(proCounter);
            case Absolute, Indirect -> {
                var base = this.bus.readInt(proCounter);
                if (mode == AddressMode.Indirect) {
                    base = this.bus.readInt(base);
                }
                yield base;
            }
            case ZeroPage_X -> u8add(this.bus.ReadU8(proCounter), regX);
            case ZeroPage_Y -> u8add(this.bus.ReadU8(proCounter), regY);
            case Absolute_X -> {
                var base = this.bus.readInt(proCounter);
                var addr = base + regX;
                this.pageCross(base, addr);
                yield addr;
            }
            case Absolute_Y -> {
                var base = this.bus.readInt(proCounter);
                var addr = base + regY;
                this.pageCross(base, addr);
                yield addr;
            }
            case Indirect_X -> {
                var base = this.bus.ReadU8(proCounter);
                var ptr = u8add(base, regX);
                yield this.bus.readInt(ptr);
            }
            case Indirect_Y -> {
                var base = this.bus.ReadU8(proCounter);
                base = this.bus.readInt(base);
                var addr = base + regY;
                pageCross(base, addr);
                yield addr;
            }
            default -> 0;
        };
    }


    /**
     * Judge whether the data obtained in the current addressing mode is spread across pages
     *
     * @param base Origin memory address
     * @param addr Target memory address
     */
    public void pageCross(int base, int addr) {
        var pageCross = ((base & 0xff00) != (addr & 0xff00));
        if (!pageCross) {
            return;
        }
        this.increment();
    }

    public void increment() {
        this.cycles++;
    }
}
