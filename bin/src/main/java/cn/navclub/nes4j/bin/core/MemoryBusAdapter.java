package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.config.AddressMode;
import lombok.Getter;


import static cn.navclub.nes4j.bin.util.BinUtil.u8add;


/**
 * {@link MemoryBus} CPU access adapter
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class MemoryBusAdapter implements Bus {
    private final CPU cpu;
    private final MemoryBus bus;
    private final NesConsole console;
    //Page cross product extra cycle
    @Getter
    private int bulking;
    //Sync system other component consumer cycle
    private int decrement;

    public MemoryBusAdapter(CPU cpu, NesConsole console) {
        this.cpu = cpu;
        this.console = console;
        this.bus = console.getBus();
    }

    public int getAbsAddr(AddressMode mode) {
        var regX = this.cpu.getRx();
        var regY = this.cpu.getRy();
        var proCounter = this.cpu.getPc();
        return switch (mode) {
            case Immediate -> proCounter;
            case ZeroPage -> this.ReadU8(proCounter);
            case Absolute, Indirect -> {
                var base = this.readInt(proCounter);
                if (mode == AddressMode.Indirect) {
                    base = this.readInt(base);
                }
                yield base;
            }
            case ZeroPage_X -> u8add(this.ReadU8(proCounter), regX);
            case ZeroPage_Y -> u8add(this.ReadU8(proCounter), regY);
            case Absolute_X -> {
                var base = this.readInt(proCounter);
                var addr = base + regX;
                this.pageCross(base, addr);
                yield addr;
            }
            case Absolute_Y -> {
                var base = this.readInt(proCounter);
                var addr = base + regY;
                this.pageCross(base, addr);
                yield addr;
            }
            case Indirect_X -> {
                var base = this.ReadU8(proCounter);
                var ptr = u8add(base, regX);
                yield this.readInt(ptr);
            }
            case Indirect_Y -> {
                var base = this.ReadU8(proCounter);
                base = this.readInt(base);
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

    @Override
    public void WriteU8(int address, int value) {
        this.APU_PPUSync();
        this.bus.WriteU8(address, value);
    }

    @Override
    public int ReadU8(int address) {
        this.APU_PPUSync();
        return this.bus.ReadU8(address);
    }

    @Override
    public byte read(int address) {
        this.APU_PPUSync();
        return this.bus.read(address);
    }

    @Override
    public void write(int address, byte value) {
        this.APU_PPUSync();
        this.bus.write(address, value);
    }

    public void increment() {
        this.bulking++;
        this.APU_PPUSync();
    }

    @Override
    public int readInt(int address) {
        this.APU_PPUSync();
        return this.bus.readInt(address);
    }

    private void APU_PPUSync() {
        this.decrement--;
        this.console.getPpu().tick();
        this.console.getApu().tick();
    }


    public void reset() {
        this.bulking = 0;
        this.decrement = 0;
    }

    public byte directRead(int addr) {
        return this.bus.read(addr);
    }

    public int calOffset() {
        return this.decrement + this.bulking;
    }
}
