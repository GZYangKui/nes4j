package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.config.*;
import cn.navclub.nes4j.bin.core.register.CPUStatus;
import cn.navclub.nes4j.bin.debug.OpenCodeFormat;
import cn.navclub.nes4j.bin.ppu.PPU;
import cn.navclub.nes4j.bin.util.BinUtil;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;
import static cn.navclub.nes4j.bin.util.MathUtil.u8add;
import static cn.navclub.nes4j.bin.util.MathUtil.u8sbc;

/**
 * 6502 cpu instance
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
@Slf4j
public class CPU {
    //栈开始位置
    public static final int STACK = 0x0100;
    //程序计数器重置地址
    private static final int PC_RESET = 0xfffc;
    //程序栈重置地址
    private static final int STACK_RESET = 0xfd;

    //累加寄存器
    @Getter
    private int ra;
    //X寄存器
    @Getter
    private int rx;
    //Y寄存器
    @Getter
    private int ry;
    //程序计数器
    @Getter
    private int pc;
    @Getter
    //栈指针寄存器,始终指向栈顶
    private int sp;
    private final Bus bus;
    private final AddrMProvider modeProvider;
    //cpu状态
    private final CPUStatus status;

    public CPU(NES context) {
        this.bus = context.getBus();
        this.status = new CPUStatus();
        this.modeProvider = new AddrMProvider(this, this.bus);
    }


    /**
     * 重置寄存器和程序计数器
     */
    public void reset() {
        this.rx = 0;
        this.ry = 0;
        this.ra = 0;
        this.sp = STACK_RESET;
        this.pc = this.bus.readInt(PC_RESET);
        this.status.setBits(int8(0b000100));
    }


    public void push(byte data) {
        this.bus.write(STACK + this.sp, data);
        this.sp = u8sbc(this.sp, 1);
    }

    public void pushInt(int data) {
        var lsb = data & 0xff;
        var msb = (data >> 8) & 0xff;
        this.push((byte) msb);
        this.push((byte) lsb);
    }

    public byte pop() {
        this.sp = u8add(this.sp, 1);
        return this.bus.read(STACK + this.sp);
    }

    public int popInt() {
        var lsb = this.pop() & 0xff;
        var msb = this.pop() & 0xff;
        return lsb | msb << 8;
    }

    /**
     * LDA指令实现
     */
    private void lda(AddressMode mode) {
        var address = this.modeProvider.getAbsAddr(mode);
        var value = this.bus.ReadU8(address);
        this.raUpdate(value);
    }

    private void raUpdate(int value) {
        //强制将累加器的值控制在一个字节内
        value &= 0xff;
        //Set ra
        this.ra = value;
        //Update Zero and negative flag
        this.NZUpdate(value);
    }

    /**
     * 更新负标识和零标识
     */
    private void NZUpdate(int result) {
        var b = (result & 0xff);
        this.status.update(ICPUStatus.ZERO, (b == 0));
        this.status.update(ICPUStatus.NEGATIVE, (b >> 7) == 1);
    }

    /**
     * 逻辑运算 或、与、异或
     */
    private void logic(Instruction instruction, AddressMode addressMode) {
        var address = this.modeProvider.getAbsAddr(addressMode);
        var a = this.ra;
        var b = this.bus.ReadU8(address);
        var c = switch (instruction) {
            case EOR -> a ^ b;
            case ORA -> a | b;
            case AND -> a & b;
            default -> a;
        };
        this.raUpdate(c);
    }

    private void lsr(AddressMode mode) {
        var addr = 0;
        var operand = mode == AddressMode.Accumulator
                ? this.ra
                : this.bus.ReadU8(addr = this.modeProvider.getAbsAddr(mode));

        this.status.update(ICPUStatus.CARRY, (operand & 1) == 1);
        operand >>= 1;
        if (mode == AddressMode.Accumulator) {
            this.raUpdate(operand);
        } else {
            this.bus.WriteU8(addr, operand);
            this.NZUpdate(operand);
        }
    }

    private void rol(AddressMode mode) {
        var bit = 0;
        var addr = 0;
        var value = 0;
        var updateRA = (mode == AddressMode.Accumulator);
        if (updateRA) {
            value = this.ra;
        } else {
            addr = this.modeProvider.getAbsAddr(mode);
            value = this.bus.ReadU8(addr);
        }
        bit = value >> 7;
        value <<= 1;
        value |= this.status.get(ICPUStatus.CARRY);
        this.status.update(ICPUStatus.CARRY, bit == 1);
        if (updateRA) {
            this.raUpdate(value);
        } else {
            this.bus.WriteU8(addr, value);
            this.NZUpdate(value);
        }
    }

    private void ror(AddressMode mode) {
        var addr = 0;
        var value = this.ra;
        var rora = mode == AddressMode.Accumulator;
        if (!rora) {
            addr = this.modeProvider.getAbsAddr(mode);
            value = this.bus.ReadU8(addr);
        }
        var oBit = value & 1;
        value >>= 1;
        value |= (this.status.get(ICPUStatus.CARRY) << 7);
        this.status.update(ICPUStatus.CARRY, oBit == 1);
        if (rora) {
            this.raUpdate(value);
        } else {
            this.bus.WriteU8(addr, value);
            this.NZUpdate(value);
        }
    }

    private void asl(InstructionWrap instruction6502) {
        int b;
        var a = (instruction6502.getAddressMode() == AddressMode.Accumulator);
        var address = -1;
        if (a) {
            b = this.ra;
        } else {
            address = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
            b = this.bus.ReadU8(address);
        }
        //更新进位标识
        this.status.update(ICPUStatus.CARRY, (b >> 7) == 1);
        //左移1位
        b = b << 1;
        if (a) {
            this.raUpdate(b);
        } else {
            this.NZUpdate(b);
            this.bus.WriteU8(address, b);
        }
    }

    private void push(InstructionWrap instruction6502) {
        var instruction = instruction6502.getInstruction();
        if (instruction == Instruction.PHA) {
            this.push((byte) this.ra);
        } else {
            var flags = this.status.copy();
            flags.set(ICPUStatus.BREAK_COMMAND, ICPUStatus.EMPTY);
            this.push(flags.getBits());
        }
    }

    private void pull(InstructionWrap instruction6502) {
        var instruction = instruction6502.getInstruction();
        var value = this.pop();
        if (instruction == Instruction.PLA) {
            this.raUpdate(value);
        } else {
            this.status.setBits(value);
            this.status.set(ICPUStatus.EMPTY);
            this.status.clear(ICPUStatus.BREAK_COMMAND);
        }
    }

    private void cmp(InstructionWrap instruction6502) {
        final int a;
        final Instruction instruction = instruction6502.getInstruction();
        if (instruction == Instruction.CMP) {
            a = this.ra;
        } else if (instruction == Instruction.CPX) {
            a = this.rx;
        } else {
            a = this.ry;
        }
        var address = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
        var m = this.bus.ReadU8(address);
        //设置Carry Flag
        this.status.update(ICPUStatus.CARRY, a >= m);
        //更新cpu状态
        this.NZUpdate(u8sbc(a, m));
    }

    private void inc(Instruction instruction, AddressMode mode) {
        final int result;
        if (instruction == Instruction.INC) {
            var address = this.modeProvider.getAbsAddr(mode);
            var m = this.bus.ReadU8(address);
            result = u8add(m, 1);
            this.bus.WriteU8(address, result);
        } else if (instruction == Instruction.INX) {
            this.rx = result = u8add(this.rx, 1);
        } else {
            this.ry = result = u8add(this.ry, 1);
        }
        this.NZUpdate(result);
    }


    private void adc(AddressMode mode, boolean sbc) {
        var addr = this.modeProvider.getAbsAddr(mode);
        var b = this.bus.read(addr);
        if (sbc) {
            b = (byte) (-b - 1);
        }
        var value = b & 0xff;
        var sum = this.ra + value + this.status.get(ICPUStatus.CARRY);
        this.status.update(ICPUStatus.CARRY, sum > 0xff);
        var result = sum & 0xff;
        this.status.update(ICPUStatus.OVERFLOW, (((b & 0xff ^ result) & (result ^ this.ra)) & 0x80) != 0);
        this.raUpdate(result);
    }


    private void sbc(AddressMode mode) {
        this.adc(mode, true);
    }

    private void loadXY(InstructionWrap instruction6502) {
        var address = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
        var data = this.bus.ReadU8(address);
        if (instruction6502.getInstruction() == Instruction.LDX) {
            this.rx = data;
        } else {
            this.ry = data;
        }
        this.NZUpdate(data);
    }

    private void branch(boolean condition) {
        //条件不成立不跳转分支
        if (!condition) {
            return;
        }
        this.modeProvider.increment();

        var b = this.bus.read(this.pc);
        var jump = this.pc + 1 + b;
        var base = this.pc + 1;

        // Page-cross check
        this.modeProvider.pageCross(base, jump);

        this.pc = jump;
    }

    private void bit(InstructionWrap instruction6502) {
        var address = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
        var value = this.bus.ReadU8(address);
        this.status.update(ICPUStatus.ZERO, (this.ra & value) == 0);
        this.status.update(ICPUStatus.NEGATIVE, (value >> 7) == 1);
        this.status.update(ICPUStatus.OVERFLOW, (value >> 6) == 1);
    }

    private void dec(InstructionWrap instruction6502) {
        var instruction = instruction6502.getInstruction();
        var value = switch (instruction) {
            case DEC -> {
                var address = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
                var b = u8sbc(this.bus.ReadU8(address), 1);
                this.bus.WriteU8(address, b);
                yield b;
            }
            case DEX -> {
                this.rx = u8sbc(this.rx, 1);
                yield this.rx;
            }
            default -> {
                this.ry = u8sbc(this.ry, 1);
                yield this.ry;
            }
        };
        this.NZUpdate(value);
    }

    public int interrupt(CPUInterrupt interrupt) {
        if (interrupt == null
                //When set, all interrupts except the NMI are inhibited.
                || (this.status.contain(ICPUStatus.INTERRUPT_DISABLE) && interrupt != CPUInterrupt.NMI)) {
            return 0;
        }

        this.pushInt(this.pc);

        var flag = this.status.copy();

        //https://www.nesdev.org/wiki/Status_flags#The_B_flag
        flag.set(ICPUStatus.EMPTY);
        flag.update(ICPUStatus.BREAK_COMMAND, interrupt == CPUInterrupt.BRK);

        this.push(flag.getBits());

        //Automatically set by the CPU when an IRQ is triggered, and restored to its previous state by RTI.
        if (interrupt == CPUInterrupt.IRQ) {
            this.status.set(ICPUStatus.INTERRUPT_DISABLE);
        }

        this.pc = this.bus.readInt(interrupt.getVector());

        return interrupt.getCycle();
    }

    public int next() {
        this.modeProvider.setCycles(0);
        var openCode = this.bus.read(this.pc);
        var state = (++this.pc);

        if (openCode == 0x00) {
            return this.interrupt(CPUInterrupt.BRK);
        }

        var instruction6502 = Instruction.getInstance(openCode);
        var mode = instruction6502.getAddressMode();
        var instruction = instruction6502.getInstruction();

//        System.out.println(Integer.toHexString(this.pc - 1) + ":" + instruction + "(" + BinUtil.toHexStr(instruction6502.getOpenCode()) + ")");

        if (instruction == Instruction.JMP) {
            this.pc = this.modeProvider.getAbsAddr(mode);
        }

        if (instruction == Instruction.RTI) {
            this.status.setBits(this.pop());

            this.status.set(ICPUStatus.EMPTY);
            this.status.clear(ICPUStatus.BREAK_COMMAND);

            this.pc = this.popInt();
        }
        if (instruction == Instruction.JSR) {
            this.pushInt(this.pc + 1);
            this.pc = this.bus.readInt(this.pc);
        }

        if (instruction == Instruction.RTS) {
            this.pc = this.popInt() + 1;
        }

        if (instruction == Instruction.LDA) {
            this.lda(mode);
        }

        //加减运算
        if (instruction == Instruction.ADC) {
            this.adc(instruction6502.getAddressMode(), false);
        }

        if (instruction == Instruction.SBC) {
            this.sbc(instruction6502.getAddressMode());
        }

        //或、与、异或逻辑运算
        if (instruction == Instruction.AND
                || instruction == Instruction.ORA
                || instruction == Instruction.EOR) {
            this.logic(instruction6502.getInstruction(), instruction6502.getAddressMode());
        }

        //push累加寄存器/状态寄存器
        if (instruction == Instruction.PHA || instruction == Instruction.PHP) {
            this.push(instruction6502);
        }
        //pull累加寄存器/状态寄存器
        if (instruction == Instruction.PLA || instruction == Instruction.PLP) {
            this.pull(instruction6502);
        }

        //左移1位
        if (instruction == Instruction.ASL) {
            this.asl(instruction6502);
        }

        if (instruction == Instruction.ROL) {
            this.rol(mode);
        }

        //右移一位
        if (instruction == Instruction.ROR) {
            this.ror(mode);
        }

        //刷新累加寄存器值到内存
        if (instruction == Instruction.STA) {
            var addr = this.modeProvider.getAbsAddr(mode);
            if (this.pc - 1 == 0xe992) {
                var str = "555";
            }
            this.bus.WriteU8(addr, this.ra);
        }

        //刷新y寄存器值到内存
        if (instruction == Instruction.STY) {
            this.bus.WriteU8(this.modeProvider.getAbsAddr(mode), this.ry);
        }

        //刷新x寄存器值到内存中
        if (instruction == Instruction.STX) {
            this.bus.WriteU8(this.modeProvider.getAbsAddr(mode), this.rx);
        }

        //清除进位标识
        if (instruction == Instruction.CLC) {
            this.status.clear(ICPUStatus.CARRY);
        }

        //清除Decimal model
        if (instruction == Instruction.CLD) {
            this.status.clear(ICPUStatus.DECIMAL_MODE);
        }

        //清除中断标识
        if (instruction == Instruction.CLI) {
            this.status.clear(ICPUStatus.INTERRUPT_DISABLE);
        }

        if (instruction == Instruction.CLV) {
            this.status.clear(ICPUStatus.OVERFLOW);
        }

        if (instruction == Instruction.CMP
                || instruction == Instruction.CPX
                || instruction == Instruction.CPY) {
            this.cmp(instruction6502);
        }

        if (instruction == Instruction.INC
                || instruction == Instruction.INX
                || instruction == Instruction.INY) {
            this.inc(instruction6502.getInstruction(), instruction6502.getAddressMode());
        }


        if (instruction == Instruction.LDX || instruction == Instruction.LDY) {
            this.loadXY(instruction6502);
        }

        if (instruction == Instruction.BIT) {
            this.bit(instruction6502);
        }

        //By negative flag to jump
        if (instruction == Instruction.BPL || instruction == Instruction.BMI) {
            this.branch((instruction == Instruction.BMI) == this.status.contain(ICPUStatus.NEGATIVE));
        }

        //By zero flag to jump
        if (instruction == Instruction.BEQ || instruction == Instruction.BNE) {
            this.branch((instruction == Instruction.BEQ) == this.status.contain(ICPUStatus.ZERO));
        }

        //By overflow flag to jump
        if (instruction == Instruction.BVC || instruction == Instruction.BVS) {
            this.branch((instruction == Instruction.BVS) == this.status.contain(ICPUStatus.OVERFLOW));
        }

        if (instruction == Instruction.BCS || instruction == Instruction.BCC) {
            this.branch((instruction == Instruction.BCS) == this.status.contain(ICPUStatus.CARRY));
        }

        if (instruction == Instruction.DEC
                || instruction == Instruction.DEX
                || instruction == Instruction.DEY) {
            this.dec(instruction6502);
        }


        if (instruction == Instruction.SEC) {
            this.status.set(ICPUStatus.CARRY);
        }

        if (instruction == Instruction.SED) {
            this.status.set(ICPUStatus.DECIMAL_MODE);
        }

        if (instruction == Instruction.SEI) {
            this.status.set(ICPUStatus.INTERRUPT_DISABLE);
        }

        if (instruction == Instruction.TAX) {
            this.rx = this.ra;
            this.NZUpdate(this.rx);
        }
        if (instruction == Instruction.TAY) {
            this.ry = this.ra;
            this.NZUpdate(this.ry);
        }
        if (instruction == Instruction.TSX) {
            this.rx = this.sp;
            this.NZUpdate(this.rx);
        }
        if (instruction == Instruction.TXA) {
            this.raUpdate(this.rx);
        }

        if (instruction == Instruction.TXS) {
            this.sp = this.rx;
        }

        if (instruction == Instruction.TYA) {
            this.raUpdate(this.ry);
        }

        if (instruction == Instruction.SLO) {
            this.asl(instruction6502);
            this.logic(Instruction.ORA, instruction6502.getAddressMode());
        }

        if (instruction == Instruction.ISC) {
            this.inc(Instruction.INC, instruction6502.getAddressMode());
            this.sbc(instruction6502.getAddressMode());
        }

        if (instruction == Instruction.RLA) {
            this.rol(mode);
            this.adc(mode, false);
        }

        if (instruction == Instruction.ALR) {
            this.logic(Instruction.AND, mode);
            this.lsr(mode);
        }

        if (instruction == Instruction.ANC) {
            this.adc(mode, false);
            this.status.update(ICPUStatus.CARRY, this.status.contain(ICPUStatus.NEGATIVE));
        }

        if (instruction == Instruction.XAA) {
            this.raUpdate(this.rx);
            var addr = this.modeProvider.getAbsAddr(mode);
            var b = this.bus.ReadU8(addr);
            this.raUpdate(b & this.ra);
        }

        if (instruction == Instruction.ARR) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var b = this.bus.ReadU8(addr);
            this.raUpdate(b & this.ra);
            this.ror(AddressMode.Accumulator);
            var result = this.ra;
            var b5 = (result >> 5 & 1);
            var b6 = (result >> 6 & 1);
            this.status.update(ICPUStatus.CARRY, b6 == 1);
            this.status.update(ICPUStatus.OVERFLOW, (b5 ^ b6) == 1);
            this.NZUpdate(result);
        }

        if (instruction == Instruction.DCP) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var value = this.bus.ReadU8(addr);
            value = u8sbc(value, 1);
            this.bus.WriteU8(addr, value);
            if (value <= this.ra) {
                this.status.set(ICPUStatus.CARRY);
            }
            this.NZUpdate(u8sbc(this.ra, value));
        }

        if (instruction == Instruction.LAS) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var value = this.bus.ReadU8(addr);
            value = value & this.sp;
            this.rx = value;
            this.sp = value;
            this.raUpdate(value);
        }

        if (instruction == Instruction.LAX) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var value = this.bus.ReadU8(addr);
            this.raUpdate(value);
            this.rx = value;
        }

        if (instruction == Instruction.SRE || instruction == Instruction.LSR) {
            this.lsr(instruction6502.getAddressMode());
            if (instruction == Instruction.SRE) {
                this.logic(Instruction.EOR, instruction6502.getAddressMode());
            }
        }

        if (instruction == Instruction.SHX) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var value = this.rx & u8add((addr >> 8) & 0xff, 1);
            this.bus.WriteU8(addr, value);
        }

        if (instruction == Instruction.TAX || instruction == Instruction.LXA) {
            if (instruction == Instruction.LXA) {
                this.lda(mode);
            }
            this.rx = this.ra;
            this.NZUpdate(this.rx);
        }

        if (instruction == Instruction.SAX) {
            var data = this.ra & this.rx;
            var addr = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
            this.bus.WriteU8(addr, data);
        }

        //根据是否发生重定向来判断是否需要更改程序计数器的值
        if (this.pc == state) {
            this.pc += (instruction6502.getSize() - 1);
        }

        return instruction6502.getCycle() + this.modeProvider.getCycles();
    }

    public byte getStatus() {
        return this.status.getBits();
    }
}
