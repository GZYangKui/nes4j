package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.config.*;
import cn.navclub.nes4j.bin.core.register.CPUStatus;
import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;
import lombok.Getter;

import static cn.navclub.nes4j.bin.util.BinUtil.int8;
import static cn.navclub.nes4j.bin.util.BinUtil.uint8;
import static cn.navclub.nes4j.bin.util.MathUtil.u8add;
import static cn.navclub.nes4j.bin.util.MathUtil.u8sbc;

/**
 * 6502 cpu instance
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class CPU {
    private final static LoggerDelegate logger = LoggerFactory.logger(CPU.class);

    //Stack offset
    public static final int STACK = 0x0100;
    //Program counter reset offset
    private static final int PC_RESET = 0xfffc;
    //Stack reset offset
    private static final int STACK_RESET = 0xfd;

    //Accumulator register
    @Getter
    private int ra;
    //X register
    @Getter
    private int rx;
    //Y register
    @Getter
    private int ry;
    //Program counter
    @Getter
    private int pc;
    @Getter
    //Stack pointer
    private int sp;
    private final Bus bus;
    private final AddrMProvider modeProvider;
    //CPU status
    private final CPUStatus status;

    public CPU(NES context) {
        this.bus = context.getBus();
        this.status = new CPUStatus();
        this.modeProvider = new AddrMProvider(this, this.bus);
    }


    /**
     * Reset cpu all status
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
        var lsb = uint8(data);
        var msb = uint8(data >> 8);

        this.push(int8(msb));
        this.push(int8(lsb));
    }

    public byte pop() {
        this.sp = u8add(this.sp, 1);
        return this.bus.read(STACK + this.sp);
    }

    public int popInt() {
        var lsb = uint8(this.pop());
        var msb = uint8(this.pop());
        return lsb | msb << 8;
    }


    private void lda(AddressMode mode) {
        var address = this.modeProvider.getAbsAddr(mode);
        var value = this.bus.ReadU8(address);
        this.raUpdate(value);
    }

    private void raUpdate(int value) {
        value = uint8(value);
        //Set ra
        this.ra = value;
        //Update Zero and negative flag
        this.NZUpdate(value);
    }

    /**
     * Check CPU Negative and Zero flag
     */
    private void NZUpdate(int result) {
        var b = uint8(result);
        this.status.update(ICPUStatus.ZERO, (b == 0));
        this.status.update(ICPUStatus.NEGATIVE, (b >> 7) == 1);
    }

    /**
     * Or and not operator
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
        //Check Carry flag
        this.status.update(ICPUStatus.CARRY, (b >> 7) == 1);
        //Left shifter one bit
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
            this.push(int8(this.ra));
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
        var val = switch (instruction6502.getInstruction()) {
            case CMP -> this.ra;
            case CPX -> this.rx;
            default -> this.ry;
        };
        var address = this.modeProvider.getAbsAddr(instruction6502.getAddressMode());
        var m = this.bus.ReadU8(address);
        //Set carry Flag
        this.status.update(ICPUStatus.CARRY, val >= m);
        //Update cpu status
        this.NZUpdate(u8sbc(val, m));
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
            /* The absolute value of a negative number may be larger than the size
             * of the corresponding positive number, so here needs `-b -1` after
             * taking the opposite number.
             * */
            b = int8(-b - 1);
        }
        var value = uint8(b);
        var sum = this.ra + value + this.status.get(ICPUStatus.CARRY);
        this.status.update(ICPUStatus.CARRY, sum > 0xff);
        var result = uint8(sum);
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
        //If condition was false not anything.
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
            case DEX -> this.rx = u8sbc(this.rx, 1);
            default -> this.ry = u8sbc(this.ry, 1);
        };
        this.NZUpdate(value);
    }

    public int interrupt(CPUInterrupt interrupt) {
        //When ICPUStatus#INTERRUPT_DISABLE flag was set, all interrupts except the NMI are inhibited.
        if (this.status.contain(ICPUStatus.INTERRUPT_DISABLE) && interrupt != CPUInterrupt.NMI) {
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
        var openCode = this.bus.read(this.pc);
        var state = (++this.pc);

        if (openCode == 0x00) {
            return this.interrupt(CPUInterrupt.BRK);
        }

        var instruction6502 = Instruction.getInstance(openCode);
        var mode = instruction6502.getAddressMode();
        var instruction = instruction6502.getInstruction();

        if (logger.isTraceEnabled()) {
            var operand = "";
            if (mode != AddressMode.Implied && mode != AddressMode.Accumulator && mode != AddressMode.Relative) {
                operand = "0x" + Integer.toHexString(this.modeProvider.getAbsAddr(mode));
            }
            logger.trace(
                    "[0x{}] A:{} X:{} Y:{} S:{} {}({}) {}",
                    Integer.toHexString(this.pc - 1),
                    Integer.toHexString(this.ra),
                    Integer.toHexString(this.rx),
                    Integer.toHexString(this.ry),
                    this.status,
                    instruction,
                    instruction6502.getCycle(),
                    operand
            );
        }

        this.modeProvider.setCycles(0);

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

        if (instruction == Instruction.ADC) {
            this.adc(instruction6502.getAddressMode(), false);
        }

        if (instruction == Instruction.SBC) {
            this.sbc(instruction6502.getAddressMode());
        }

        if (instruction == Instruction.AND
                || instruction == Instruction.ORA
                || instruction == Instruction.EOR) {
            this.logic(instruction6502.getInstruction(), instruction6502.getAddressMode());
        }

        if (instruction == Instruction.PHA || instruction == Instruction.PHP) {
            this.push(instruction6502);
        }

        if (instruction == Instruction.PLA || instruction == Instruction.PLP) {
            this.pull(instruction6502);
        }

        if (instruction == Instruction.ASL) {
            this.asl(instruction6502);
        }

        if (instruction == Instruction.ROL) {
            this.rol(mode);
        }

        if (instruction == Instruction.ROR) {
            this.ror(mode);
        }

        if (instruction == Instruction.STA) {
            var addr = this.modeProvider.getAbsAddr(mode);
            this.bus.WriteU8(addr, this.ra);
        }

        if (instruction == Instruction.STY) {
            this.bus.WriteU8(this.modeProvider.getAbsAddr(mode), this.ry);
        }

        if (instruction == Instruction.STX) {
            this.bus.WriteU8(this.modeProvider.getAbsAddr(mode), this.rx);
        }

        if (instruction == Instruction.CLC) {
            this.status.clear(ICPUStatus.CARRY);
        }

        if (instruction == Instruction.CLD) {
            this.status.clear(ICPUStatus.DECIMAL_MODE);
        }

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
            var value = this.rx & u8add(uint8(addr >> 8), 1);
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

        if (instruction == Instruction.RRA) {
            this.ror(instruction6502.getAddressMode());
            this.adc(instruction6502.getAddressMode(), false);
        }

        //
        // Judge whether it is necessary to change the value of the program counter according to whether the
        // redirection occurs
        //
        if (this.pc == state) {
            this.pc += (instruction6502.getSize() - 1);
        }

        return instruction6502.getCycle() + this.modeProvider.getCycles();
    }

    public byte getStatus() {
        return this.status.getBits();
    }
}
