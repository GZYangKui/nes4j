package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.config.*;
import cn.navclub.nes4j.bin.core.register.CPUStatus;
import cn.navclub.nes4j.bin.util.MathUtil;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static cn.navclub.nes4j.bin.util.MathUtil.u8add;
import static cn.navclub.nes4j.bin.util.MathUtil.u8sbc;


@Data
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
        this.status.reset();
        this.sp = STACK_RESET;
        this.pc = this.bus.readInt(PC_RESET);
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
        this.sp = MathUtil.u8add(this.sp, 1);
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
        this.status.update(ICPUStatus.ZF, (b == 0));
        this.status.update(ICPUStatus.NF, (b >> 7) == 1);
    }

    /**
     * 逻辑运算 或、与、异或
     */
    private void logic(CPUInstruction instruction, AddressMode addressMode) {
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

        this.status.update(ICPUStatus.CF, (operand & 1) == 1);
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
        value |= this.status.get(ICPUStatus.CF);
        this.status.update(ICPUStatus.CF, bit == 1);
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
        value |= (this.status.get(ICPUStatus.CF) << 7);
        this.status.update(ICPUStatus.CF, oBit == 1);
        if (rora) {
            this.raUpdate(value);
        } else {
            this.bus.WriteU8(addr, value);
            this.NZUpdate(value);
        }
    }

    private void asl(Instruction6502 instruction6502) {
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
        this.status.update(ICPUStatus.CF, (b >> 7) == 1);
        //左移1位
        b = b << 1;
        if (a) {
            this.raUpdate(b);
        } else {
            this.NZUpdate(b);
            this.bus.WriteU8(address, b);
        }
    }

    private void push(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        if (instruction == CPUInstruction.PHA) {
            this.push((byte) this.ra);
        } else {
            var flags = this.status._clone();
            flags.set(ICPUStatus.BK, ICPUStatus.BK2);
            this.push(flags.getBits());
        }
    }

    private void pull(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        var value = this.pop();
        if (instruction == CPUInstruction.PLA) {
            this.raUpdate(value);
        } else {
            this.status.setBits(value);
            this.status.set(ICPUStatus.BK2);
            this.status.clear(ICPUStatus.BK);
        }
    }

    private void cmp(Instruction6502 instruction6502) {
        final int a;
        final CPUInstruction instruction = instruction6502.getInstruction();
        if (instruction == CPUInstruction.CMP) {
            a = this.ra;
        } else if (instruction == CPUInstruction.CPX) {
            a = this.rx;
        } else {
            a = this.ry;
        }
        var address = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
        var m = this.bus.ReadU8(address);
        //设置Carry Flag
        this.status.update(ICPUStatus.CF, a >= m);
        //更新cpu状态
        this.NZUpdate(u8sbc(a, m));
    }

    private void inc(CPUInstruction instruction, AddressMode mode) {
        final int result;
        if (instruction == CPUInstruction.INC) {
            var address = this.modeProvider.getAbsAddr(mode);
            var m = this.bus.ReadU8(address);
            result = MathUtil.u8add(m, 1);
            this.bus.WriteU8(address, result);
        } else if (instruction == CPUInstruction.INX) {
            this.rx = result = MathUtil.u8add(this.rx, 1);
        } else {
            this.ry = result = MathUtil.u8add(this.ry, 1);
        }
        this.NZUpdate(result);
    }


    private void adc(AddressMode mode, boolean sbc) {
        var addr = this.modeProvider.getAbsAddr(mode);
        var b = this.getBus().read(addr);
        if (sbc) {
            b = (byte) (-b - 1);
        }
        var value = b & 0xff;
        var sum = this.ra + value + this.status.get(ICPUStatus.CF);
        this.status.update(ICPUStatus.CF, sum > 0xff);
        var result = sum & 0xff;
        this.status.update(ICPUStatus.OF, (((b & 0xff ^ result) & (result ^ this.ra)) & 0x80) != 0);
        this.raUpdate(result);
    }


    private void sbc(AddressMode mode) {
        this.adc(mode, true);
    }

    private void loadXY(Instruction6502 instruction6502) {
        var address = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
        var data = this.bus.ReadU8(address);
        if (instruction6502.getInstruction() == CPUInstruction.LDX) {
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
        this.bus.tick(1);

        var b = this.bus.read(this.pc);
        var jump = this.pc + 1 + b;
        var base = this.pc + 1;

        //判断跳转是否跨页
        this.modeProvider.pageCross(base, jump);

        this.pc = jump;
    }

    private void bit(Instruction6502 instruction6502) {
        var address = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
        var value = this.bus.ReadU8(address);
        this.status.update(ICPUStatus.ZF, (this.ra & value) == 0);
        this.status.update(ICPUStatus.NF, (value >> 7) == 1);
        this.status.update(ICPUStatus.OF, (value >> 6) == 1);
    }

    private void dec(Instruction6502 instruction6502) {
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

    public void interrupt(CPUInterrupt interrupt) {
        if (interrupt == null || (interrupt != CPUInterrupt.NMI && this.status.contain(ICPUStatus.ID))) {
            return;
        }

        this.pushInt(this.pc);

        var flag = this.status._clone();

        //https://www.nesdev.org/wiki/Status_flags#The_B_flag
        flag.set(ICPUStatus.BK2);
        flag.update(ICPUStatus.BK, interrupt == CPUInterrupt.BRK);

        this.push(flag.getBits());

        this.status.set(ICPUStatus.ID);
        this.bus.tick(interrupt.getCycle());
        this.pc = this.bus.readInt(interrupt.getVector());
    }

    public int next() {
        this.modeProvider.setCycles(0);
        var openCode = this.bus.read(this.pc);
        var state = (++this.pc);

        if (openCode == 0x00) {
            this.interrupt(CPUInterrupt.BRK);
            return 0;
        }

        var instruction6502 = CPUInstruction.getInstance(openCode);
        var mode = instruction6502.getAddressMode();
        var instruction = instruction6502.getInstruction();


        if (instruction == CPUInstruction.JMP) {
            this.pc = this.modeProvider.getAbsAddr(mode);
        }

        if (instruction == CPUInstruction.RTI) {
            this.status.setBits(this.pop());

            this.status.set(ICPUStatus.BK2);
            this.status.clear(ICPUStatus.BK);

            this.pc = this.popInt();
        }
        if (instruction == CPUInstruction.JSR) {
            this.pushInt(this.pc + 1);
            this.pc = this.bus.readInt(this.pc);
        }

        if (instruction == CPUInstruction.RTS) {
            this.pc = this.popInt() + 1;
        }

        if (instruction == CPUInstruction.LDA) {
            this.lda(mode);
        }

        //加减运算
        if (instruction == CPUInstruction.ADC) {
            this.adc(instruction6502.getAddressMode(), false);
        }

        if (instruction == CPUInstruction.SBC) {
            this.sbc(instruction6502.getAddressMode());
        }

        //或、与、异或逻辑运算
        if (instruction == CPUInstruction.AND
                || instruction == CPUInstruction.ORA
                || instruction == CPUInstruction.EOR) {
            this.logic(instruction6502.getInstruction(), instruction6502.getAddressMode());
        }

        //push累加寄存器/状态寄存器
        if (instruction == CPUInstruction.PHA || instruction == CPUInstruction.PHP) {
            this.push(instruction6502);
        }
        //pull累加寄存器/状态寄存器
        if (instruction == CPUInstruction.PLA || instruction == CPUInstruction.PLP) {
            this.pull(instruction6502);
        }

        //左移1位
        if (instruction == CPUInstruction.ASL) {
            this.asl(instruction6502);
        }

        if (instruction == CPUInstruction.ROL) {
            this.rol(mode);
        }

        //右移一位
        if (instruction == CPUInstruction.ROR) {
            this.ror(mode);
        }

        //刷新累加寄存器值到内存
        if (instruction == CPUInstruction.STA) {
            var addr = this.modeProvider.getAbsAddr(mode);
            this.bus.WriteU8(addr, this.ra);
        }

        //刷新y寄存器值到内存
        if (instruction == CPUInstruction.STY) {
            this.bus.WriteU8(this.modeProvider.getAbsAddr(mode), this.ry);
        }

        //刷新x寄存器值到内存中
        if (instruction == CPUInstruction.STX) {
            this.bus.WriteU8(this.modeProvider.getAbsAddr(mode), this.rx);
        }

        //清除进位标识
        if (instruction == CPUInstruction.CLC) {
            this.status.clear(ICPUStatus.CF);
        }

        //清除Decimal model
        if (instruction == CPUInstruction.CLD) {
            this.status.clear(ICPUStatus.DM);
        }

        //清除中断标识
        if (instruction == CPUInstruction.CLI) {
            this.status.clear(ICPUStatus.ID);
        }

        if (instruction == CPUInstruction.CLV) {
            this.status.clear(ICPUStatus.OF);
        }

        if (instruction == CPUInstruction.CMP
                || instruction == CPUInstruction.CPX
                || instruction == CPUInstruction.CPY) {
            this.cmp(instruction6502);
        }

        if (instruction == CPUInstruction.INC
                || instruction == CPUInstruction.INX
                || instruction == CPUInstruction.INY) {
            this.inc(instruction6502.getInstruction(), instruction6502.getAddressMode());
        }


        if (instruction == CPUInstruction.LDX || instruction == CPUInstruction.LDY) {
            this.loadXY(instruction6502);
        }

        if (instruction == CPUInstruction.BIT) {
            this.bit(instruction6502);
        }

        //By negative flag to jump
        if (instruction == CPUInstruction.BPL || instruction == CPUInstruction.BMI) {
            this.branch((instruction == CPUInstruction.BMI) == this.status.contain(ICPUStatus.NF));
        }

        //By zero flag to jump
        if (instruction == CPUInstruction.BEQ || instruction == CPUInstruction.BNE) {
            this.branch((instruction == CPUInstruction.BEQ) == this.status.contain(ICPUStatus.ZF));
        }

        //By overflow flag to jump
        if (instruction == CPUInstruction.BVC || instruction == CPUInstruction.BVS) {
            this.branch((instruction == CPUInstruction.BVS) == this.status.contain(ICPUStatus.OF));
        }

        if (instruction == CPUInstruction.BCS || instruction == CPUInstruction.BCC) {
            this.branch((instruction == CPUInstruction.BCS) == this.status.contain(ICPUStatus.CF));
        }

        if (instruction == CPUInstruction.DEC
                || instruction == CPUInstruction.DEX
                || instruction == CPUInstruction.DEY) {
            this.dec(instruction6502);
        }


        if (instruction == CPUInstruction.SEC) {
            this.status.set(ICPUStatus.CF);
        }

        if (instruction == CPUInstruction.SED) {
            this.status.set(ICPUStatus.DM);
        }

        if (instruction == CPUInstruction.SEI) {
            this.status.set(ICPUStatus.ID);
        }

        if (instruction == CPUInstruction.TAX) {
            this.rx = this.ra;
            this.NZUpdate(this.rx);
        }
        if (instruction == CPUInstruction.TAY) {
            this.ry = this.ra;
            this.NZUpdate(this.ry);
        }
        if (instruction == CPUInstruction.TSX) {
            this.rx = this.sp;
            this.NZUpdate(this.rx);
        }
        if (instruction == CPUInstruction.TXA) {
            this.raUpdate(this.rx);
        }

        if (instruction == CPUInstruction.TXS) {
            this.sp = this.rx;
        }

        if (instruction == CPUInstruction.TYA) {
            this.raUpdate(this.ry);
        }

        if (instruction == CPUInstruction.SLO) {
            this.asl(instruction6502);
            this.logic(CPUInstruction.ORA, instruction6502.getAddressMode());
        }

        if (instruction == CPUInstruction.ISC) {
            this.inc(CPUInstruction.INC, instruction6502.getAddressMode());
            this.sbc(instruction6502.getAddressMode());
        }

        if (instruction == CPUInstruction.RLA) {
            this.rol(mode);
            this.adc(mode, false);
        }

        if (instruction == CPUInstruction.ALR) {
            this.logic(CPUInstruction.AND, mode);
            this.lsr(mode);
        }

        if (instruction == CPUInstruction.ANC) {
            this.adc(mode, false);
            this.status.update(ICPUStatus.CF, this.status.contain(ICPUStatus.NF));
        }

        if (instruction == CPUInstruction.XAA) {
            this.raUpdate(this.rx);
            var addr = this.modeProvider.getAbsAddr(mode);
            var b = this.bus.ReadU8(addr);
            this.raUpdate(b & this.ra);
        }

        if (instruction == CPUInstruction.ARR) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var b = this.bus.ReadU8(addr);
            this.raUpdate(b & this.ra);
            this.ror(AddressMode.Accumulator);
            var result = this.ra;
            var b5 = (result >> 5 & 1);
            var b6 = (result >> 6 & 1);
            this.status.update(ICPUStatus.CF, b6 == 1);
            this.status.update(ICPUStatus.OF, (b5 ^ b6) == 1);
            this.NZUpdate(result);
        }

        if (instruction == CPUInstruction.DCP) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var value = this.bus.ReadU8(addr);
            value = u8sbc(value, 1);
            this.bus.WriteU8(addr, value);
            if (value <= this.ra) {
                this.status.set(ICPUStatus.CF);
            }
            this.NZUpdate(u8sbc(this.ra, value));
        }

        if (instruction == CPUInstruction.LAS) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var value = this.bus.ReadU8(addr);
            value = value & this.sp;
            this.rx = value;
            this.sp = value;
            this.raUpdate(value);
        }

        if (instruction == CPUInstruction.LAX) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var value = this.bus.ReadU8(addr);
            this.raUpdate(value);
            this.rx = value;
        }

        if (instruction == CPUInstruction.SRE || instruction == CPUInstruction.LSR) {
            this.lsr(instruction6502.getAddressMode());
            if (instruction == CPUInstruction.SRE) {
                this.logic(CPUInstruction.EOR, instruction6502.getAddressMode());
            }
        }

        if (instruction == CPUInstruction.SHX) {
            var addr = this.modeProvider.getAbsAddr(mode);
            var value = this.rx & u8add((addr >> 8) & 0xff, 1);
            this.bus.WriteU8(addr, value);
        }

        if (instruction == CPUInstruction.TAX || instruction == CPUInstruction.LXA) {
            if (instruction == CPUInstruction.LXA) {
                this.lda(mode);
            }
            this.rx = this.ra;
            this.NZUpdate(this.rx);
        }

        if (instruction == CPUInstruction.SAX) {
            var data = this.ra & this.rx;
            var addr = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
            this.bus.WriteU8(addr, data);
        }

        //根据是否发生重定向来判断是否需要更改程序计数器的值
        if (this.pc == state) {
            this.pc += (instruction6502.getBytes() - 1);
        }

        return instruction6502.getCycle() + this.modeProvider.getCycles();
    }
}
