package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.enums.AddressMode;
import cn.navclub.nes4j.bin.util.MathUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * <a href="http://www.emulator101.com/6502-addressing-modes.html">6502 address mode</a>
 */
public class AddressModeProvider {
    private final CPU cpu;
    private final Bus bus;

    public AddressModeProvider(CPU cpu, Bus bus) {
        this.cpu = cpu;
        this.bus = bus;
    }

    public int getAbsAddr(AddressMode mode) {
        var regX = this.cpu.getRx();
        var regY = this.cpu.getRy();
        var proCounter = this.cpu.getPc();
        return switch (mode) {
            case Immediate -> proCounter;
            case ZeroPage -> this.bus.readUSByte(proCounter);
            case Absolute, Indirect -> {
                var base = this.bus.readInt(proCounter);
                if (mode == AddressMode.Indirect) {
                    base = this.bus.readInt(base);
                }
                yield base;
            }
            case ZeroPage_X -> MathUtil.unsignedAdd(this.bus.readUSByte(proCounter), regX);
            case ZeroPage_Y -> MathUtil.unsignedAdd(this.bus.readUSByte(proCounter), regY);
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
                var base = this.bus.readUSByte(proCounter);
                var ptr = MathUtil.unsignedAdd(base, regX);
                yield this.bus.readInt(ptr);
            }
            case Indirect_Y -> {
                var base = this.bus.readUSByte(proCounter);
                base = this.bus.readInt(base);
                var addr = base + regY;
                pageCross(base, addr);
                yield addr;
            }
            default -> 0;
        };
    }


    /**
     * 判断当前寻址模式下获取到数据是否跨页
     */
    private void pageCross(int base, int addr) {
        var pageCross = ((base & 0xff00) != (addr & 0xff00));
        if (!pageCross) {
            return;
        }
        this.bus.tick(1);
    }
}
