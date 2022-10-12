package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.core.registers.CSRegister;
import cn.navclub.nes4j.bin.enums.AddressMode;
import cn.navclub.nes4j.bin.enums.CPUInstruction;
import cn.navclub.nes4j.bin.enums.CPUInterrupt;
import cn.navclub.nes4j.bin.model.Instruction6502;
import cn.navclub.nes4j.bin.util.ByteUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
public class CPU {
    //栈开始位置
    private static final int STACK = 0x0100;

    //累加寄存器
    private int ra;
    //X寄存器
    private int rx;
    //Y寄存器
    private int ry;
    @Setter
    @Getter
    //程序计数器
    private int pc;

    @Setter
    private int pcReset;
    @Setter
    private int stackReset;

    //栈指针寄存器,始终指向栈顶
    private int sp;
    //cpu状态
    private final CSRegister status;

    private final Bus bus;

    public CPU(final Bus bus, int stackReset, int pcReset) {
        this.bus = bus;
        this.pcReset = pcReset;
        this.stackReset = stackReset;
        this.status = new CSRegister();
    }


    /**
     * 重置寄存器和程序计数器
     */
    public void reset() {
        this.rx = 0;
        this.ry = 0;
        this.ra = 0;
        this.status.reset();
        this.sp = this.stackReset;
        this.pc = this.bus.readInt(this.pcReset);
    }


    private void pushByte(byte data) {
        this.bus.writeByte(STACK + this.sp, data);
        this.sp--;
    }

    private void pushInt(int data) {
        var lsb = data & 0xFF;
        var msb = (data >> 8) & 0xFF;
        this.pushByte(ByteUtil.overflow(lsb));
        this.pushByte(ByteUtil.overflow(msb));
    }

    private byte popByte() {
        this.sp++;
        return this.bus.readByte(STACK + this.sp);
    }

    private int popUSByte() {
        return Byte.toUnsignedInt(popByte());
    }

    private int popInt() {
        var msb = Byte.toUnsignedInt(this.popByte());
        var lsb = Byte.toUnsignedInt(this.popByte());
        return lsb | msb << 8;
    }

    /**
     * LDA指令实现
     */
    private void lda(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressMode());
        var value = this.bus.readUSByte(address);
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
        this.status.update(CSRegister.BIFlag.ZERO_FLAG, result == 0);
        this.status.update(CSRegister.BIFlag.NEGATIVE_FLAG, (result & 0x80) != 0);
    }

    /**
     * 根据指定寻址模式获取操作数地址,
     * <a href="https://www.nesdev.org/obelisk-6502-guide/addressing.html#IMM">相关开发文档.</a></p>
     */
    private int getOperandAddr(AddressMode mode) {
        return switch (mode) {
            case Immediate -> this.pc;
            case ZeroPage -> this.bus.readUSByte(this.pc);
            case Absolute, Indirect -> this.bus.readInt(this.pc);
            case ZeroPage_X -> this.bus.readUSByte(this.pc) + this.rx;
            case ZeroPage_Y -> this.bus.readUSByte(this.pc) + this.ry;
            case Absolute_X -> this.bus.readInt(this.pc) + this.rx;
            case Absolute_Y -> this.bus.readInt(this.pc) + this.ry;
            case Indirect_X -> {
                var base = this.bus.readInt(this.pc);
                var ptr = base + this.ra;
                yield this.bus.readInt(ptr);
            }
            case Indirect_Y -> {
                var base = this.bus.readInt(this.pc);
                var ptr = base + this.ry;
                yield this.bus.readInt(ptr);
            }
            default -> -1;
        };
    }

    /**
     * 逻辑运算 或、与、异或
     */
    private void logic(CPUInstruction instruction, AddressMode addressMode) {
        var address = this.getOperandAddr(addressMode);
        var a = this.ra;
        var b = this.bus.readUSByte(address);
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
                : this.bus.readUSByte(addr = this.getOperandAddr(mode));

        this.status.update(CSRegister.BIFlag.CARRY_FLAG, (operand & 1) != 0);
        operand >>= 1;
        if (mode == AddressMode.Accumulator) {
            this.raUpdate(operand);
        } else {
            this.bus.writeUSByte(addr, operand);
            this.NZUpdate(operand);
        }
    }

    private void rol(Instruction6502 instruction6502) {
        var mode = instruction6502.getAddressMode();
        var cBit = this.status.hasFlag(CSRegister.BIFlag.CARRY_FLAG) ? 0xff : 0xfe;

        int result, oBit;
        if (mode == AddressMode.Accumulator) {
            oBit = (this.ra & 0x80);
            result = this.ra << 1;
            result &= cBit;
            this.raUpdate(result);
        } else {
            var address = this.getOperandAddr(mode);
            var value = this.bus.readByte(address);
            oBit = value & 0x80;
            result = value << 1;
            result &= cBit;
            this.NZUpdate(result);
        }

        this.status.update(CSRegister.BIFlag.CARRY_FLAG, oBit != 0);
    }

    private void ror(Instruction6502 instruction6502) {
        var mode = instruction6502.getAddressMode();
        var cBit = 0;
        var result = 0;
        var oldCBit = this.status.hasFlag(CSRegister.BIFlag.CARRY_FLAG) ? 0xff : 0x7f;
        if (mode == AddressMode.Accumulator) {
            cBit = this.ra & 1;
            result = this.ra >>= 1;
            result &= oldCBit;
            this.raUpdate(result);
        } else {
            var address = this.getOperandAddr(mode);
            var value = this.bus.readByte(address);
            cBit = value & 1;
            result = value >> 1;
            result &= oldCBit;
            this.bus.writeByte(address, ByteUtil.overflow(result));
            this.NZUpdate(result);
        }
        this.status.update(CSRegister.BIFlag.CARRY_FLAG, cBit > 0);
    }

    private void asl(Instruction6502 instruction6502) {
        final int b;
        var a = (instruction6502.getAddressMode() == AddressMode.Accumulator);
        var address = -1;
        if (a) {
            b = this.ra;
        } else {
            address = this.getOperandAddr(instruction6502.getAddressMode());
            b = this.bus.readUSByte(address);
        }
        var c = b & (1 << 7);
        //更新进位标识
        this.status.update(CSRegister.BIFlag.CARRY_FLAG, c != 0);
        //左移1位
        c = b << 1;
        if (a) {
            this.raUpdate(c);
        } else {
            this.bus.writeByte(address, (byte) c);
        }
    }

    private void push(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        if (instruction == CPUInstruction.PHA) {
            this.pushByte(ByteUtil.overflow(this.ra));
        } else {
            this.pushByte(this.status.getValue());
        }
        this.sp++;
    }

    private void pull(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        var value = this.bus.readByte(this.sp);
        if (instruction == CPUInstruction.PLA) {
            this.raUpdate(value);
        } else {
            this.status.setValue(value);
        }
    }

    /**
     * {@link CPUInstruction#CMP}、{@link  CPUInstruction#CPX}和{@link CPUInstruction#CPY} 具体实现
     */
    private void cmp(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressMode());
        final int a;
        final CPUInstruction instruction = instruction6502.getInstruction();
        if (instruction == CPUInstruction.CMP) {
            a = this.ra;
        } else if (instruction == CPUInstruction.CPX) {
            a = this.rx;
        } else if (instruction == CPUInstruction.CPY) {
            a = this.ry;
        } else {
            return;
        }
        var b = this.bus.readByte(address);
        //设置Carry Flag
        this.status.update(CSRegister.BIFlag.CARRY_FLAG, a >= b);
        //设置Zero Flag
        this.status.update(CSRegister.BIFlag.ZERO_FLAG, a == b);
    }

    private void inc(CPUInstruction instruction, AddressMode mode) {
        final int result;
        if (instruction == CPUInstruction.INC) {
            var address = this.getOperandAddr(mode);
            var a = this.bus.readUSByte(address);
            result = a + 1;
            this.bus.writeInt(address, result);
        } else if (instruction == CPUInstruction.INX) {
            var x = this.rx;
            this.rx = result = x + 1;
        } else if (instruction == CPUInstruction.INY) {
            var y = this.ry;
            this.ry = result = y + 1;
        } else {
            return;
        }
        this.status.update(CSRegister.BIFlag.ZERO_FLAG, result == 0);
        this.status.update(CSRegister.BIFlag.NEGATIVE_FLAG, (result & 0x40) != 0);
    }

    private void maths(AddressMode mode, boolean sbc) {
        int result;
        var address = this.getOperandAddr(mode);
        var b = this.bus.readUSByte(address);
        var carry = this.status.getFlagBit(CSRegister.BIFlag.CARRY_FLAG);
        if (sbc) {
            result = this.ra - b - carry;
        } else {
            result = this.ra + b + carry;
        }
        this.status.update(CSRegister.BIFlag.CARRY_FLAG, result > 0xff);
        this.status.update(CSRegister.BIFlag.OVERFLOW_FLAG, ((b ^ result) & (result ^ this.ra) & 0x80) != 0);
        this.raUpdate(result);
    }

    private void loadXY(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressMode());
        var data = this.bus.readByte(address);
        if (instruction6502.getInstruction() == CPUInstruction.LDX) {
            this.rx = data;
        } else {
            this.ry = data;
        }
        this.status.update(CSRegister.BIFlag.CARRY_FLAG, data == 0);
        this.status.update(CSRegister.BIFlag.NEGATIVE_FLAG, (data & 0x40) > 0);
    }

    private void branch(boolean condition) {
        //条件不成立不跳转分支
        if (!condition) {
            return;
        }
        var jump = this.bus.readByte(this.pc);
        this.pc = this.pc + 1 + jump;
    }

    private void bit(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressMode());
        var value = this.bus.readByte(address);

        this.status.update(CSRegister.BIFlag.NEGATIVE_FLAG, value < 0);
        this.status.update(CSRegister.BIFlag.OVERFLOW_FLAG, (value & 0x20) != 0);
        this.status.update(CSRegister.BIFlag.ZERO_FLAG, (value & this.ra) == 0);
    }

    private void dec(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        var value = switch (instruction) {
            case DEC -> {
                var address = getOperandAddr(instruction6502.getAddressMode());
                var b = this.bus.readByte(address);
                b--;
                this.bus.writeByte(address, b);
                yield b;
            }
            case DEX -> {
                this.rx -= 1;
                yield this.rx;
            }
            default -> {
                this.ry -= 1;
                yield this.ry;
            }
        };

        this.status.update(CSRegister.BIFlag.ZERO_FLAG, value == 0);
        this.status.update(CSRegister.BIFlag.NEGATIVE_FLAG, value < 0);
    }

    public void interrupt(CPUInterrupt interrupt) {
        //中断状态不可用
        if (interrupt != CPUInterrupt.NMI && this.status.hasFlag(CSRegister.BIFlag.INTERRUPT_DISABLE)) {
            return;
        }
        if (interrupt == CPUInterrupt.RESET) {
            this.reset();
        } else {
            this.pushInt(this.pc);
            this.pushByte(ByteUtil.overflow(this.status.getValue()));
            //禁用中断
            this.status.setFlag(CSRegister.BIFlag.INTERRUPT_DISABLE);
            this.pc = this.bus.readInt(interrupt == CPUInterrupt.NMI ? 0xFFFA : 0xFFFE);
            this.status.update(CSRegister.BIFlag.BREAK_COMMAND, interrupt == CPUInterrupt.BRK);
        }
    }


    public void execute() {
        var openCode = this.bus.readByte(this.pc);
        var pcState = (++this.pc);
        var instruction6502 = CPUInstruction.getInstance(openCode);
        if (instruction6502 == null) {
            return;
        }

        var instruction = instruction6502.getInstruction();


        if (instruction != CPUInstruction.BRK) {
            log.info("({}){}(0x{}) {}", pcState - 1, instruction,
                    Integer.toHexString(Byte.toUnsignedInt(openCode)), formatInstruction(instruction6502));
        }

        if (instruction == CPUInstruction.JMP) {
            this.pc = this.bus.readInt(this.getOperandAddr(instruction6502.getAddressMode()));
        }

        if (instruction == CPUInstruction.RTI) {
            this.status.setValue(this.popByte());
            //取消中断标识
            this.status.clearFlag(CSRegister.BIFlag.INTERRUPT_DISABLE);
            this.pc = this.popUSByte();
        }
        if (instruction == CPUInstruction.JSR) {
            this.pushInt(this.pc + 1);
            this.pc = this.bus.readInt(this.pc);
        }

        if (instruction == CPUInstruction.RTS) {
            this.pc = this.popInt() + 1;
        }
        if (instruction == CPUInstruction.BRK) {
            this.interrupt(CPUInterrupt.BRK);
        }

        if (instruction == CPUInstruction.LDA) {
            this.lda(instruction6502);
        }

        //加减运算
        if (instruction == CPUInstruction.ADC || instruction == CPUInstruction.SBC) {
            this.maths(instruction6502.getAddressMode(), instruction == CPUInstruction.SBC);
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
            this.rol(instruction6502);
        }

        //右移一位
        if (instruction == CPUInstruction.ROR) {
            this.ror(instruction6502);
        }

        //刷新累加寄存器值到内存
        if (instruction == CPUInstruction.STA) {
            this.bus.writeUSByte(this.getOperandAddr(instruction6502.getAddressMode()), this.ra);
        }

        //刷新y寄存器值到内存
        if (instruction == CPUInstruction.STY) {
            this.bus.writeUSByte(this.getOperandAddr(instruction6502.getAddressMode()), this.ry);
        }
        //刷新x寄存器值到内存中
        if (instruction == CPUInstruction.STX) {
            this.bus.writeUSByte(this.getOperandAddr(instruction6502.getAddressMode()), this.rx);
        }

        //清除进位标识
        if (instruction == CPUInstruction.CLC) {
            this.status.clearFlag(CSRegister.BIFlag.CARRY_FLAG);
        }

        //清除Decimal model
        if (instruction == CPUInstruction.CLD) {
            this.status.clearFlag(CSRegister.BIFlag.DECIMAL_MODE);
        }

        //清除中断标识
        if (instruction == CPUInstruction.CLI) {
            this.status.clearFlag(CSRegister.BIFlag.INTERRUPT_DISABLE);
        }

        if (instruction == CPUInstruction.CLV) {
            this.status.clearFlag(CSRegister.BIFlag.OVERFLOW_FLAG);
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

        if (instruction == CPUInstruction.BCC) {
            this.branch(!this.status.hasFlag(CSRegister.BIFlag.CARRY_FLAG));
        }

        if (instruction == CPUInstruction.BCS) {
            this.branch(this.status.hasFlag(CSRegister.BIFlag.CARRY_FLAG));
        }

        if (instruction == CPUInstruction.BEQ) {
            this.branch(this.status.hasFlag(CSRegister.BIFlag.ZERO_FLAG));
        }

        if (instruction == CPUInstruction.BMI) {
            this.branch(this.status.hasFlag(CSRegister.BIFlag.NEGATIVE_FLAG));
        }

        if (instruction == CPUInstruction.BPL) {
            this.branch(!this.status.hasFlag(CSRegister.BIFlag.NEGATIVE_FLAG));
        }

        if (instruction == CPUInstruction.BNE) {
            this.branch(!this.status.hasFlag(CSRegister.BIFlag.ZERO_FLAG));
        }

        if (instruction == CPUInstruction.BVC) {
            this.branch(!this.status.hasFlag(CSRegister.BIFlag.OVERFLOW_FLAG));
        }
        if (instruction == CPUInstruction.BCS) {
            this.branch(this.status.hasFlag(CSRegister.BIFlag.OVERFLOW_FLAG));
        }

        if (instruction == CPUInstruction.DEC
                || instruction == CPUInstruction.DEX
                || instruction == CPUInstruction.DEY) {
            this.dec(instruction6502);
        }


        if (instruction == CPUInstruction.SEC) {
            this.status.setFlag(CSRegister.BIFlag.CARRY_FLAG);
        }

        if (instruction == CPUInstruction.SED) {
            this.status.setFlag(CSRegister.BIFlag.DECIMAL_MODE);
        }

        if (instruction == CPUInstruction.SEI) {
            this.status.setFlag(CSRegister.BIFlag.INTERRUPT_DISABLE);
        }

        if (instruction == CPUInstruction.TAX
                || instruction == CPUInstruction.TAY
                || instruction == CPUInstruction.TSX
                || instruction == CPUInstruction.TXA
                || instruction == CPUInstruction.TXS
                || instruction == CPUInstruction.TYA) {
            final int r;
            if (instruction == CPUInstruction.TAX)
                r = this.rx = this.ra;
            else if (instruction == CPUInstruction.TAY)
                r = this.ry = this.ra;
            else if (instruction == CPUInstruction.TSX)
                r = this.rx = this.sp;
            else if (instruction == CPUInstruction.TXA)
                r = this.rx;
            else if (instruction == CPUInstruction.TXS)
                r = this.sp = this.rx;
            else
                r = this.rx = this.ry;
            //fix:TXS not affect relative status
            if (instruction != CPUInstruction.TXS) {
                this.NZUpdate(r);
            }
        }
        if (instruction == CPUInstruction.SLO) {
            this.asl(instruction6502);
            this.logic(CPUInstruction.ORA, instruction6502.getAddressMode());
        }

        if (instruction == CPUInstruction.ISC) {
            this.inc(CPUInstruction.INC, instruction6502.getAddressMode());
            this.maths(instruction6502.getAddressMode(), true);
        }


        if (instruction == CPUInstruction.SRE || instruction == CPUInstruction.LSR) {
            this.lsr(instruction6502.getAddressMode());
            if (instruction == CPUInstruction.SRE) {
                this.logic(CPUInstruction.EOR, instruction6502.getAddressMode());
            }
        }

        if (instruction == CPUInstruction.SHX) {
            var addr = this.getOperandAddr(instruction6502.getAddressMode());
            var value = this.bus.readUSByte(addr + 1);
            value = this.rx & value;
            this.bus.writeInt(addr, value);
        }

        this.bus.tick(instruction6502.getCycle());

        //根据是否发生重定向来判断是否需要更改程序计数器的值
        if (this.pc == pcState) {
            this.pc += (instruction6502.getBytes() - 1);
        } else {
            log.debug("Program counter was redirect to [0x{}/{}]", Integer.toHexString(this.pc), this.pc);
        }
    }

    /**
     * 格式化指令
     */
    private String formatInstruction(Instruction6502 instruction6502) {
        var size = instruction6502.getBytes();
        var mode = instruction6502.getAddressMode();
        if (size == 1 || mode == null || mode == AddressMode.NoneAddressing) {
            return "";
        }
        var value = switch (mode) {
            case Accumulator -> this.ra;
            case Immediate -> this.bus.readUSByte(this.getOperandAddr(mode));
            default -> this.getOperandAddr(mode);
        };

        final String str;
        //多个操作数
        if (value > 0xff) {
            var lsb = value & 0xff;
            var msb = (value >>> 8) & 0xff;
            str = String.format("0x%s,0x%s", Integer.toHexString(lsb), Integer.toHexString(msb));
        } else {
            str = String.format("0x%s", Integer.toHexString(value));
        }
        return str;
    }
}
