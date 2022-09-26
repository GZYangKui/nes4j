package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.config.CPUStatus;
import cn.navclub.nes4j.bin.enums.AddressModel;
import cn.navclub.nes4j.bin.enums.CPUInstruction;
import cn.navclub.nes4j.bin.enums.CPUInterrupt;
import cn.navclub.nes4j.bin.model.Instruction6502;
import cn.navclub.nes4j.bin.util.ByteUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 6502 微处理器是一个相对简单的 8 位 CPU，只有几个内部寄存器能够通过其 16 位地址总线寻址最多 64Kb 的内存。处理器是小端的，并希望地址首先存储在内存中的最低有效字节中。
 * <p>
 * 内存的第一个 256 字节页面 ($0000-$00FF) 被称为“零页”，是许多特殊寻址模式的焦点，这些模式会导致更短（更快）的指令或允许间接访问内存。第二页内存（$0100-$01FF）是为系统堆栈保留的，不能重定位。
 * <p>
 * 内存映射中唯一的其他保留位置是内存 $FFFA 到 $FFFF 的最后 6 个字节，必须使用不可屏蔽中断处理程序 ($FFFA/B)、上电复位位置 ($ FFFC/D) 和 BRK/中断请求处理程序 ($FFFE/F)。
 * <p>
 * 6502 对硬件设备没有任何特殊支持，因此必须将它们映射到内存区域才能与硬件锁存器交换数据。
 */
@Slf4j
public class CPU {
    //0x0100-0x01FF
    private static final int STACK = 0x0100;
    private static final int SAFE_POINT = 0xFFFC;
    private static final int STACK_RESET = 0xFD;
    //累加寄存器
    private int ra;
    //X寄存器 用作特定内存寻址模式中的偏移量。可用于辅助存储需求（保存温度值、用作计数器等）
    private int rx;
    //Y寄存器
    private int ry;
    //程序计数器
    private int pc;
    //栈指针寄存器,始终指向栈顶
    private int sp;
    /**
     * 状态寄存器.<p/>
     * 以下表格描述各位分标代表那些状态：
     * <table>
     *     <tr>
     *         <th>0</th>
     *         <th>1</th>
     *         <th>2</th>
     *         <th>3</th>
     *         <th>4</th>
     *         <th>5</th>
     *         <th>6</th>
     *     </tr>
     *     <tr>
     *         <td>Carry Flag</td>
     *         <td>Zero Flag</td>
     *         <td>Interrupt Disable</td>
     *         <td>Decimal Model</td>
     *         <td>Break Command</td>
     *         <td>Overflow Flag</td>
     *         <td>Negative Flag</td>
     *     </tr>
     * </table>
     */
    private final CPUStatus status;

    private final Bus bus;

    public CPU(final Bus bus) {
        this.bus = bus;
        this.sp = STACK_RESET;
        this.status = new CPUStatus();
    }


    /**
     * 重置寄存器和程序计数器
     */
    public void reset() {
        this.rx = 0;
        this.ry = 0;
        this.ra = 0;
        this.status.reset();
        this.pc = SAFE_POINT;
        this.sp = STACK_RESET;
    }


    public void stackPush(byte data) {
        this.bus.writeByte(STACK + this.sp, data);
        this.sp++;
    }

    public void stackLPush(int data) {
        var lsb = data & 0xFF;
        var msb = (data >> 8) & 0xFF;
        this.stackPush(ByteUtil.overflow(lsb));
        this.stackPush(ByteUtil.overflow(msb));
    }

    public byte popByte() {
        this.sp--;
        return this.bus.readByte(STACK + this.sp);
    }

    private int popUSByte() {
        return Byte.toUnsignedInt(popByte());
    }

    public int popInt() {
        var msb = Byte.toUnsignedInt(this.popByte());
        var lsb = Byte.toUnsignedInt(this.popByte());
        return lsb | msb << 8;
    }

    /**
     * LDA指令实现
     */
    private void lda(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var value = this.bus.readByte(address);
        this.raUpdate(value);
    }

    private void raUpdate(int ra) {
        //Set ra
        this.ra = ra;
        //Update Zero and negative flag
        this.NZUpdate(ra);
    }

    /**
     * 更新负标识和零标识
     */
    private void NZUpdate(int result) {
        this.status.update(CPUStatus.BIFlag.ZERO_FLAG, result == 0);
        this.status.update(CPUStatus.BIFlag.NEGATIVE_FLAG, (result & 0b0100_0000) != 0);
    }

    /**
     * 根据指定寻址模式获取操作数地址,
     * <a href="https://www.nesdev.org/obelisk-6502-guide/addressing.html#IMM">相关开发文档.</a></p>
     */
    private int getOperandAddr(AddressModel model) {
        return switch (model) {
            case Immediate -> this.pc;
            case ZeroPage -> this.bus.readUSByte(this.pc);
            case Absolute -> this.bus.readInt(this.pc);
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
    private void logic(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var a = this.ra;
        var b = this.bus.readByte(address);
        var instruction = instruction6502.getInstruction();
        var c = switch (instruction) {
            case EOR -> a ^ b;
            case ORA -> a | b;
            case AND -> a & b;
            default -> a;
        };
        this.raUpdate(c);
    }

    private void ror(Instruction6502 instruction6502) {
        var mode = instruction6502.getAddressModel();
        var cBit = 0;
        var result = 0;
        var oldCBit = this.status.hasFlag(CPUStatus.BIFlag.CARRY_FLAG) ? 0b1111_1111 : 0b0111_1111;
        if (mode == AddressModel.Accumulator) {
            cBit = this.ra & 0b0000_0001;
            result = this.ra >>= 1;
            result &= oldCBit;
            this.raUpdate(result);
        } else {
            var address = this.getOperandAddr(mode);
            var value = this.bus.readByte(address);
            cBit = value & 0b0000_0001;
            result = value >> 1;
            result &= oldCBit;
            this.bus.writeByte(address, ByteUtil.overflow(result));
            this.NZUpdate(result);
        }
        this.status.update(CPUStatus.BIFlag.CARRY_FLAG, cBit > 0);
    }

    private void asl(Instruction6502 instruction6502) {
        final byte b;
        var a = instruction6502.getAddressModel() == AddressModel.Accumulator;
        var address = -1;
        if (a) {
            b = (byte) this.ra;
        } else {
            address = this.getOperandAddr(instruction6502.getAddressModel());
            b = this.bus.readByte(address);
        }
        var c = b & 0b1000_0000;
        //更新进位标识
        this.status.update(CPUStatus.BIFlag.CARRY_FLAG, c != 0);
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
            this.bus.writeInt(this.sp, this.ra);
        } else {
            this.bus.writeByte(this.sp, this.status.getValue());
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
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        final int a;
        final CPUInstruction instruction = instruction6502.getInstruction();
        if (instruction == CPUInstruction.CMP) {
            a = this.ra;
        } else if (instruction == CPUInstruction.CPX) {
            a = this.rx;
        } else if (instruction == CPUInstruction.CPY) {
            a = this.ry;
        } else {
            log.warn("Not support compare instruction:[0x{}] alis:[{}]",
                    Integer.toHexString(instruction6502.getOpenCode()),
                    instruction6502.getInstruction()
            );
            return;
        }
        var b = this.bus.readByte(address);
        //设置Carry Flag
        this.status.update(CPUStatus.BIFlag.CARRY_FLAG, a >= b);
        //设置Zero Flag
        this.status.update(CPUStatus.BIFlag.ZERO_FLAG, a == b);
    }

    private void inc(Instruction6502 instruction6502) {
        final CPUInstruction instruction = instruction6502.getInstruction();
        final int result;
        if (instruction == CPUInstruction.INC) {
            var address = this.getOperandAddr(instruction6502.getAddressModel());
            var a = this.bus.readByte(address);
            result = a + 1;
            this.bus.writeInt(address, result);
        } else if (instruction == CPUInstruction.INX) {
            var x = this.rx;
            result = x + 1;
            this.rx = result;
        } else if (instruction == CPUInstruction.INY) {
            var y = this.ry;
            result = y + 1;
            this.ry = result;
        } else {
            log.warn("Unknown inc instruction:[0x{}] alia:[{}].", instruction6502.getOpenCode(), instruction);
            return;
        }
        this.status.update(CPUStatus.BIFlag.ZERO_FLAG, result == 0);
        this.status.update(CPUStatus.BIFlag.NEGATIVE_FLAG, (result & 0b0100_0000) != 0);
    }


    private void adc(AddressModel model) {
        var address = this.getOperandAddr(model);
        var m = this.bus.readByte(address);
        var c = this.status.hasFlag(CPUStatus.BIFlag.CARRY_FLAG) ? 1 : 0;
        var sum = this.ra + m + c;
        //If result overflow set carry-bit else remove carry bit.
        this.status.update(CPUStatus.BIFlag.CARRY_FLAG, sum > 0xff);
        //Set if sign-bit incorrect
        this.status.update(CPUStatus.BIFlag.OVERFLOW_FLAG, ((m ^ sum) & (sum ^ this.ra)) != 0);
        this.raUpdate(sum);
    }

    private void loadXY(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var data = this.bus.readByte(address);
        if (instruction6502.getInstruction() == CPUInstruction.LDX) {
            this.rx = data;
        } else {
            this.ry = data;
        }
        this.status.update(CPUStatus.BIFlag.CARRY_FLAG, data == 0);
        this.status.update(CPUStatus.BIFlag.NEGATIVE_FLAG, (data & 0b0100_0000) > 0);
    }

    private void branch(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        //不成立
        if ((instruction == CPUInstruction.BCC && this.status.hasFlag(CPUStatus.BIFlag.CARRY_FLAG))
                || (instruction == CPUInstruction.BCS && !this.status.hasFlag(CPUStatus.BIFlag.CARRY_FLAG))
                || (instruction == CPUInstruction.BEQ && !this.status.hasFlag(CPUStatus.BIFlag.ZERO_FLAG))
                || (instruction == CPUInstruction.BMI && this.status.hasFlag(CPUStatus.BIFlag.INTERRUPT_DISABLE))
                || (instruction == CPUInstruction.BNE && this.status.hasFlag(CPUStatus.BIFlag.ZERO_FLAG))
                || (instruction == CPUInstruction.BPL && this.status.hasFlag(CPUStatus.BIFlag.NEGATIVE_FLAG))
                || (instruction == CPUInstruction.BVC && this.status.hasFlag(CPUStatus.BIFlag.OVERFLOW_FLAG)
                || instruction == CPUInstruction.BVS && !this.status.hasFlag(CPUStatus.BIFlag.OVERFLOW_FLAG))) {
            return;
        }
        var jump = this.bus.readByte(this.pc);
        this.pc = this.pc + 1 + jump;
    }

    private void bit(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var value = this.bus.readByte(address);

        this.status.update(CPUStatus.BIFlag.NEGATIVE_FLAG, value < 0);
        this.status.update(CPUStatus.BIFlag.OVERFLOW_FLAG, (value & 0b0010_0000) != 0);
        this.status.update(CPUStatus.BIFlag.ZERO_FLAG, (value & this.ra) == 0);
    }

    private void dec(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        var value = switch (instruction) {
            case DEC -> {
                var address = getOperandAddr(instruction6502.getAddressModel());
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

        this.status.update(CPUStatus.BIFlag.ZERO_FLAG, value == 0);
        this.status.update(CPUStatus.BIFlag.NEGATIVE_FLAG, value < 0);
    }

    private void jump(Instruction6502 instruction6502) {
        final int address;
        if (instruction6502.getAddressModel() == AddressModel.Absolute) {
            address = this.getOperandAddr(AddressModel.Absolute);
        } else {
            address = this.bus.readInt(this.pc);
        }
        this.pc = address;
    }

    public void interrupt(CPUInterrupt interrupt) {
        if (interrupt != CPUInterrupt.NMI
                && this.status.hasFlag(CPUStatus.BIFlag.INTERRUPT_DISABLE)) {
//            log.debug("Current interrupt disable status.");
            return;
        }
        if (interrupt == CPUInterrupt.RESET) {
            this.pc = this.bus.readByte(SAFE_POINT);
        } else {
            this.stackLPush(this.pc);
            this.stackPush(ByteUtil.overflow(this.status.getValue()));
            //禁用中断
            this.status.setFlag(CPUStatus.BIFlag.INTERRUPT_DISABLE);
            this.pc = this.bus.readInt(interrupt == CPUInterrupt.NMI ? 0xFFFA : 0xFFFE);
            if (interrupt == CPUInterrupt.BRK) {
                this.status.setFlag(CPUStatus.BIFlag.BREAK_COMMAND);
            } else {
                this.status.clearFlag(CPUStatus.BIFlag.BREAK_COMMAND);
            }
        }
    }


    public void execute() {
        while (true) {
            var openCode = this.bus.readByte(this.pc);
            this.pc++;
            var instruction6502 = CPUInstruction.getInstance(openCode);
            if (instruction6502 == null) {
                continue;
            }
            var instruction = instruction6502.getInstruction();
//            if (instruction != CPUInstruction.BRK)
//                log.debug("Prepare execute instruction [0x{}] alias [{}].", Integer.toHexString(instruction6502.getOpenCode()), instruction);
            if (instruction == CPUInstruction.LDA) {
                this.lda(instruction6502);
            }
            if (instruction == CPUInstruction.ADC) {
                this.adc(instruction6502.getAddressModel());
            }
            if (instruction == CPUInstruction.BRK) {
                this.interrupt(CPUInterrupt.BRK);
            }

            //或、与、异或逻辑运算
            if (instruction == CPUInstruction.AND
                    || instruction == CPUInstruction.ORA
                    || instruction == CPUInstruction.EOR) {
                this.logic(instruction6502);
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

            //右移一位
            if (instruction == CPUInstruction.ROR) {
                this.ror(instruction6502);
            }

            //刷新累加寄存器值到内存
            if (instruction == CPUInstruction.STA) {
                this.bus.writeInt(this.getOperandAddr(instruction6502.getAddressModel()), this.ra);
            }

            //刷新y寄存器值到内存
            if (instruction == CPUInstruction.STY) {
                this.bus.writeInt(this.getOperandAddr(instruction6502.getAddressModel()), this.ry);
            }

            //清除进位标识
            if (instruction == CPUInstruction.CLC) {
                this.status.clearFlag(CPUStatus.BIFlag.CARRY_FLAG);
            }

            //清除Decimal model
            if (instruction == CPUInstruction.CLD) {
                this.status.clearFlag(CPUStatus.BIFlag.DECIMAL_MODE);
            }

            //清除中断标识
            if (instruction == CPUInstruction.CLI) {
                this.status.clearFlag(CPUStatus.BIFlag.INTERRUPT_DISABLE);
            }

            if (instruction == CPUInstruction.CLV) {
                this.status.clearFlag(CPUStatus.BIFlag.OVERFLOW_FLAG);
            }

            if (instruction == CPUInstruction.CMP
                    || instruction == CPUInstruction.CPX
                    || instruction == CPUInstruction.CPY) {
                this.cmp(instruction6502);
            }

            if (instruction == CPUInstruction.INC
                    || instruction == CPUInstruction.INX
                    || instruction == CPUInstruction.INY) {
                this.inc(instruction6502);
            }

            if (instruction == CPUInstruction.JSR) {
                this.stackLPush(this.pc + 1);
                this.pc = this.bus.readUSByte(this.pc);
            }

            if (instruction == CPUInstruction.RTS) {
                this.pc = this.popInt() + 1;
            }
            if (instruction == CPUInstruction.LDX || instruction == CPUInstruction.LDY) {
                this.loadXY(instruction6502);
            }

            if (instruction == CPUInstruction.BIT) {
                this.bit(instruction6502);
            }

            if (instruction == CPUInstruction.BCC
                    || instruction == CPUInstruction.BCS
                    || instruction == CPUInstruction.BEQ
                    || instruction == CPUInstruction.BMI
                    || instruction == CPUInstruction.BPL
                    || instruction == CPUInstruction.BNE
                    || instruction == CPUInstruction.BVC
                    || instruction == CPUInstruction.BVS) {
                this.branch(instruction6502);
            }

            if (instruction == CPUInstruction.DEC
                    || instruction == CPUInstruction.DEX
                    || instruction == CPUInstruction.DEY) {
                this.dec(instruction6502);
            }

            if (instruction == CPUInstruction.JMP) {
                this.jump(instruction6502);
            }

            if (instruction == CPUInstruction.RTI) {
                this.status.setValue(this.popByte());
                //取消中断标识
                this.status.clearFlag(CPUStatus.BIFlag.INTERRUPT_DISABLE);
                this.pc = this.popUSByte();
            }

            if (instruction == CPUInstruction.SEC) {
                this.status.setFlag(CPUStatus.BIFlag.CARRY_FLAG);
            }

            if (instruction == CPUInstruction.SED) {
                this.status.setFlag(CPUStatus.BIFlag.DECIMAL_MODE);
            }

            if (instruction == CPUInstruction.SEI) {
                this.status.setFlag(CPUStatus.BIFlag.INTERRUPT_DISABLE);
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

                this.NZUpdate(r);
            }
        }
    }
}
