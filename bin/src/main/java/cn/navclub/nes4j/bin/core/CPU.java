package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.enums.AddressMode;
import cn.navclub.nes4j.bin.enums.CPUInstruction;
import cn.navclub.nes4j.bin.enums.CPUInterrupt;
import cn.navclub.nes4j.bin.enums.CPUStatus;
import cn.navclub.nes4j.bin.util.ByteUtil;
import cn.navclub.nes4j.bin.util.MathUtil;
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
    private final SRegister status;

    private final Bus bus;


    public CPU(final Bus bus, int stackReset, int pcReset) {
        this.bus = bus;
        this.pcReset = pcReset;
        this.stackReset = stackReset;
        this.status = new SRegister();
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
        this.bus.write(STACK + this.sp, data);
        this.sp = MathUtil.unsignedSub(this.sp, 1);
    }

    private void pushInt(int data) {
        var lsb = data & 0xff;
        var msb = (data >> 8) & 0xff;
        this.pushByte((byte) lsb);
        this.pushByte((byte) msb);
    }

    private byte popByte() {
        this.sp = MathUtil.unsignedAdd(this.sp, 1);
        return this.bus.read(STACK + this.sp);
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
        var b = (byte) result;
        this.status.update(CPUStatus.ZF, (b == 0));
        this.status.update(CPUStatus.NF, (b & 0x80) != 0);
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
            case ZeroPage_X -> MathUtil.unsignedAdd(this.bus.readUSByte(this.pc), this.rx);
            case ZeroPage_Y -> MathUtil.unsignedAdd(this.bus.readUSByte(this.pc), this.ry);
            case Absolute_X -> this.bus.readInt(this.pc) + this.rx;
            case Absolute_Y -> this.bus.readInt(this.pc) + this.ry;
            case Indirect_X -> {
                var base = this.bus.readUSByte(this.pc);
                var ptr = MathUtil.unsignedAdd(base, this.rx);
                yield this.bus.readInt(ptr);
            }
            case Indirect_Y -> {
                var base = this.bus.readUSByte(this.pc);
                var ptr = this.bus.readInt(base);
                yield ptr + this.ry;
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

        this.status.update(CPUStatus.CF, (operand & 1) != 0);
        operand >>= 1;
        if (mode == AddressMode.Accumulator) {
            this.raUpdate(operand);
        } else {
            this.bus.writeUSByte(addr, operand);
            this.NZUpdate(operand);
        }
    }

    private void rol(Instruction6502 instruction6502) {
        var bit = 0;
        var addr = 0;
        var value = 0;
        var mode = instruction6502.getAddressMode();
        var updateRA = (mode == AddressMode.Accumulator);
        if (updateRA) {
            value = this.ra;
        } else {
            addr = this.getOperandAddr(mode);
            value = this.bus.readUSByte(addr);
        }
        bit = value >> 7;
        value <<= 1;
        value |= this.status.get(CPUStatus.CF);
        this.status.update(CPUStatus.CF, bit == 1);
        if (updateRA) {
            this.raUpdate(value);
        } else {
            this.bus.writeUSByte(addr, value);
            this.NZUpdate(value);
        }
    }

    private void ror(Instruction6502 instruction6502) {
        var mode = instruction6502.getAddressMode();
        var cBit = 0;
        var result = 0;
        var oldCBit = this.status.contain(CPUStatus.CF) ? 0xff : 0x7f;
        if (mode == AddressMode.Accumulator) {
            cBit = this.ra & 1;
            result = this.ra >>= 1;
            result &= oldCBit;
            this.raUpdate(result);
        } else {
            var address = this.getOperandAddr(mode);
            var value = this.bus.read(address);
            cBit = value & 1;
            result = value >> 1;
            result &= oldCBit;
            this.bus.write(address, ByteUtil.overflow(result));
            this.NZUpdate(result);
        }
        this.status.update(CPUStatus.CF, cBit > 0);
    }

    private void asl(Instruction6502 instruction6502) {
        int b;
        var a = (instruction6502.getAddressMode() == AddressMode.Accumulator);
        var address = -1;
        if (a) {
            b = this.ra;
        } else {
            address = this.getOperandAddr(instruction6502.getAddressMode());
            b = this.bus.readUSByte(address);
        }
        //更新进位标识
        this.status.update(CPUStatus.CF, (b >> 7) == 1);
        //左移1位
        b = b << 1;
        if (a) {
            this.raUpdate(b);
        } else {
            this.NZUpdate(b);
            this.bus.writeUSByte(address, b);
        }
    }

    private void push(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        if (instruction == CPUInstruction.PHA) {
            this.pushByte(ByteUtil.overflow(this.ra));
        } else {
            this.pushByte(this.status.getBits());
        }
    }

    private void pull(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        var value = this.popByte();
        if (instruction == CPUInstruction.PLA) {
            this.raUpdate(value);
        } else {
            this.status.setBits(value);
        }
    }

    private void cmp(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressMode());
        final int a;
        final CPUInstruction instruction = instruction6502.getInstruction();
        if (instruction == CPUInstruction.CMP) {
            a = this.ra;
        } else if (instruction == CPUInstruction.CPX) {
            a = this.rx;
        } else {
            a = this.ry;
        }
        var b = this.bus.readUSByte(address);
        var c = (a - b);
        //设置Carry Flag
        this.status.update(CPUStatus.CF, a >= b);
        //更新cpu状态
        this.NZUpdate(c);
    }

    private void inc(CPUInstruction instruction, AddressMode mode) {
        final int result;
        if (instruction == CPUInstruction.INC) {
            var address = this.getOperandAddr(mode);
            var m = this.bus.readUSByte(address);
            result = MathUtil.unsignedAdd(this.ra, m);
            this.bus.writeUSByte(address, result);
        } else if (instruction == CPUInstruction.INX) {
            this.rx = result = MathUtil.unsignedAdd(this.rx, 1);
        } else if (instruction == CPUInstruction.INY) {
            this.ry = result = MathUtil.unsignedAdd(this.ry, 1);
        } else {
            return;
        }
        this.NZUpdate(result);
    }


    private void adc(AddressMode mode) {
        var address = this.getOperandAddr(mode);
        var b = this.bus.readUSByte(address);
        var c = this.status.contain(CPUStatus.CF);
        var m = MathUtil.addition(this.ra, b, c);
        this.status.update(CPUStatus.CF, m.carry());
        this.status.update(CPUStatus.OF, m.overflow());
        this.raUpdate(m.result());
    }


    private void sbc(AddressMode mode) {
        var addr = this.getOperandAddr(mode);
        var a = this.ra;
        var c = this.status.contain(CPUStatus.CF);
        var b = this.bus.readUSByte(addr);
        final MathUtil.Mathematics m;
        if (ByteUtil.negative(b) && ByteUtil.negative(a)) {
            b = ByteUtil.origin(b);
            m = MathUtil.subtract(a, b, c);
        } else if (ByteUtil.negative(b) && !ByteUtil.negative(a)) {
            m = MathUtil.addition(a, ByteUtil.origin(b), c);
        } else {
            m = MathUtil.subtract(a, b, this.status.contain(CPUStatus.CF));
        }
        this.status.update(CPUStatus.CF, m.carry());
        this.status.update(CPUStatus.OF, m.overflow());
        this.raUpdate(m.result());
    }

    private void loadXY(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressMode());
        var data = this.bus.readUSByte(address);
        if (instruction6502.getInstruction() == CPUInstruction.LDX) {
            this.rx = data;
        } else {
            this.ry = data;
        }
        this.status.update(CPUStatus.CF, data == 0);
        this.status.update(CPUStatus.NF, (data & 0x40) > 0);
    }

    private void branch(boolean condition) {
        //条件不成立不跳转分支
        if (!condition) {
            return;
        }
        this.bus.tick(1);
        var b = this.bus.read(this.pc);
        var jump = this.pc + 1 + b;
        if ((this.pc + 1 & 0xff00) != (jump & 0xff00)) {
            this.bus.tick(1);
        }

        this.pc = jump;
    }

    private void bit(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressMode());
        var value = this.bus.readUSByte(address);
        this.status.update(CPUStatus.ZF, (this.ra & value) == 0);
        this.status.update(CPUStatus.NF, (value >> 7) == 1);
        this.status.update(CPUStatus.OF, (value >> 6) == 1);
    }

    private void dec(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        var value = switch (instruction) {
            case DEC -> {
                var address = getOperandAddr(instruction6502.getAddressMode());
                var b = MathUtil.unsignedSub(this.bus.readUSByte(address), 1);
                this.bus.writeUSByte(address, b);
                yield b;
            }
            case DEX -> {
                this.rx = MathUtil.unsignedSub(this.rx, 1);
                yield this.rx;
            }
            default -> {
                this.ry = MathUtil.unsignedSub(this.ry, 1);
                yield this.ry;
            }
        };
        this.NZUpdate(value);
    }

    public void interrupt(CPUInterrupt interrupt) {
        //中断状态不可用
        if (interrupt != CPUInterrupt.NMI && this.status.contain(CPUStatus.ID)) {
            return;
        }
        this.pushInt(this.pc);
        this.pushByte(this.status.getBits());
        //禁用中断
        this.status.set(CPUStatus.ID);
        this.pc = this.bus.readInt(interrupt == CPUInterrupt.NMI ? 0Xfffa : 0xfffe);
        this.status.update(CPUStatus.BK, interrupt == CPUInterrupt.BRK);
        if (interrupt == CPUInterrupt.NMI) {
            this.bus.tick(2);
        }
    }

    public void execute() {
        if (this.bus.pollPPUNMI()) {
            this.interrupt(CPUInterrupt.NMI);
        }
        var openCode = this.bus.read(this.pc);
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
            var addr = this.getOperandAddr(instruction6502.getAddressMode());
            if (instruction6502.getAddressMode() == AddressMode.Indirect) {
                var lsb = addr & 0xff;
                var msb = addr >> 8 & 0xff;
                lsb = this.bus.readUSByte(lsb);
                msb = this.bus.readUSByte(msb);
                addr = (lsb | msb << 8);
            }
            this.pc = addr;
        }

        if (instruction == CPUInstruction.RTI) {
            this.status.setBits(this.popByte());
            this.status.clear(CPUStatus.BK, CPUStatus.BK0);
            this.pc = this.popInt();
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
        if (instruction == CPUInstruction.ADC) {
            this.adc(instruction6502.getAddressMode());
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
            this.rol(instruction6502);
        }

        //右移一位
        if (instruction == CPUInstruction.ROR) {
            this.ror(instruction6502);
        }

        //刷新累加寄存器值到内存
        if (instruction == CPUInstruction.STA) {
            var addr = this.getOperandAddr(instruction6502.getAddressMode());
            if (addr == 0x2001){
                System.out.println("a");
            }
            this.bus.writeUSByte(addr, this.ra);
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
            this.status.clear(CPUStatus.CF);
        }

        //清除Decimal model
        if (instruction == CPUInstruction.CLD) {
            this.status.clear(CPUStatus.DM);
        }

        //清除中断标识
        if (instruction == CPUInstruction.CLI) {
            this.status.clear(CPUStatus.ID);
        }

        if (instruction == CPUInstruction.CLV) {
            this.status.clear(CPUStatus.OF);
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
            this.branch((instruction == CPUInstruction.BMI) == this.status.contain(CPUStatus.NF));
        }

        //By zero flag to jump
        if (instruction == CPUInstruction.BEQ || instruction == CPUInstruction.BNE) {
            this.branch((instruction == CPUInstruction.BEQ) == this.status.contain(CPUStatus.ZF));
        }

        //By overflow flag to jump
        if (instruction == CPUInstruction.BVC || instruction == CPUInstruction.BVS) {
            this.branch((instruction == CPUInstruction.BVS) == this.status.contain(CPUStatus.OF));
        }

        if (instruction == CPUInstruction.BCS || instruction == CPUInstruction.BCC) {
            this.branch((instruction == CPUInstruction.BCS) == this.status.contain(CPUStatus.CF));
        }

        if (instruction == CPUInstruction.DEC
                || instruction == CPUInstruction.DEX
                || instruction == CPUInstruction.DEY) {
            this.dec(instruction6502);
        }


        if (instruction == CPUInstruction.SEC) {
            this.status.set(CPUStatus.CF);
        }

        if (instruction == CPUInstruction.SED) {
            this.status.set(CPUStatus.DM);
        }

        if (instruction == CPUInstruction.SEI) {
            this.status.set(CPUStatus.ID);
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
            this.sbc(instruction6502.getAddressMode());
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
            str = String.format("(%d)0x%s,0x%s", value, Integer.toHexString(lsb), Integer.toHexString(msb));
        } else {
            str = String.format("(%d)0x%s", value, Integer.toHexString(value));
        }
        return str;
    }
}
