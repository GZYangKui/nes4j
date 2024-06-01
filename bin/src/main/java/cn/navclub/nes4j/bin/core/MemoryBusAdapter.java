package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NesConsole;
import cn.navclub.nes4j.bin.config.AddressMode;
import cn.navclub.nes4j.bin.config.WS6502;
import lombok.Getter;


import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static cn.navclub.nes4j.bin.util.BinUtil.u8add;


/**
 * {@link MemoryBus} CPU access adapter
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class MemoryBusAdapter implements Bus {
    @Getter
    private long cycles;
    private final CPU cpu;
    private int variation;
    private final MemoryBus bus;
    private final NesConsole console;

    public MemoryBusAdapter(CPU cpu, NesConsole console) {
        this.cpu = cpu;
        this.cycles = 0;
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
        this.SyncOtherComponent();
        this.bus.WriteU8(address, value);
    }

    @Override
    public int ReadU8(int address) {
        this.SyncOtherComponent();
        return this.bus.ReadU8(address);
    }

    @Override
    public byte read(int address) {
        this.SyncOtherComponent();
        return this.bus.read(address);
    }

    @Override
    public void write(int address, byte value) {
        this.SyncOtherComponent();
        this.bus.write(address, value);
    }

    public void increment() {
        this.variation++;
        this.SyncOtherComponent();
    }

    @Override
    public int readInt(int address) {
        this.SyncOtherComponent();
        return this.bus.readInt(address);
    }

    private void SyncOtherComponent() {
        this.cycles++;
        this.variation--;
        this.console.APU_PPuSync();
    }


    protected void _finally(WS6502 ws6502) {
        var tmp = ws6502.cycle() + this.variation;
        while (tmp-- > 0) {
            this.SyncOtherComponent();
        }
        this.variation = 0;
    }

    /**
     * Read rom UTF8 character end of <b>'\0'</b>
     */
    public String readUTF8Str(final int addr) {
        var index = 0;
        var size = 1024;
        var buffer = new byte[size];
        for (; ; ) {
            var b = this.bus.read(addr + index);
            if (b == '\0') {
                break;
            }
            if (index == size - 1) {
                var tmp = new byte[index + size];
                System.arraycopy(buffer, 0, tmp, 0, index);
                buffer = tmp;
            }
            buffer[index++] = b;
        }
        this.cpu.pc = addr + (index + 1);
        var tmp = new byte[index];
        System.arraycopy(buffer, 0, tmp, 0, index);
        return new String(tmp, StandardCharsets.UTF_8);
    }

    public byte read0(int addr) {
        return this.bus.read(addr);
    }
}
