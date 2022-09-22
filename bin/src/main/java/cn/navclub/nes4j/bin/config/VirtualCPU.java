package cn.navclub.nes4j.bin.config;

import cn.navclub.nes4j.bin.enums.AddressModel;
import cn.navclub.nes4j.bin.enums.CPUInstruction;
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
public class VirtualCPU {
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
    private int status;

    //内存区间64kb
    private final byte[] memory;

    public VirtualCPU() {
        this.sp = STACK_RESET;
        this.memory = new byte[0xFFFF];
    }

    public void loadRun(byte[] arr) {
        var index = 0x0600;
        System.arraycopy(arr, 0, this.memory, index, arr.length);
        this.reset();
        this.pc = index;
        this.run();
    }


    /**
     * 重置寄存器和程序计数器
     */
    public void reset() {
        this.rx = 0;
        this.ry = 0;
        this.ra = 0;
        this.status = 0;
        this.sp = STACK_RESET;
        this.pc = this.readLMem(SAFE_POINT);
    }

    /**
     * 从内存中读取单个字节数据
     */
    private byte readMem(int addr) {
        return this.memory[addr];
    }

    private int readUMem(int addr) {
        return Byte.toUnsignedInt(this.readMem(addr));
    }

    /**
     * 以小端序读取地址
     */
    private int readLMem(int addr) {
        var l = Byte.toUnsignedInt(this.readMem(addr));
        var h = Byte.toUnsignedInt(this.readMem((addr + 1)));
        return (h << 8 | l);
    }

    /**
     * 以小端序写入数据
     */
    private void writerMemLE(int pos, int data) {
        var lsb = data & 0xFF;
        var msb = (data >> 8) & 0xFF;

        this.writerMem(pos, ByteUtil.overflow(lsb));
        this.writerMem(pos + 1, ByteUtil.overflow(msb));
    }

    public void stackPush(byte data) {
        this.writerMem(STACK + this.sp, data);
        this.sp++;
    }

    public void stackLPush(int data) {
        var lsb = data & 0xFF;
        var msb = (data >> 8) & 0xFF;
        this.stackPush(ByteUtil.overflow(lsb));
        this.stackPush(ByteUtil.overflow(msb));
    }

    public byte stackPop() {
        this.sp--;
        return this.readMem(STACK + this.sp);
    }

    public int stackLPop() {
        var msb = this.stackPop();
        var lsb = this.stackPop();
        return lsb | msb << 8;
    }

    /**
     * LDA指令实现
     */
    private void lda(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var value = this.readMem(address);
        this.updatera(value);
    }

    private void sta(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        this.writerMem(address, ByteUtil.overflow(this.ra));
    }

    private void updatera(int ra) {
        if (ra == 0) {
            this.status |= 0b0000_00010;
        }
        if ((ra & 0b0100_0000) != 0) {
            this.status |= 0b0100_0000;
        }
        //Set ra
        this.ra = ra;
        //Update Zero and negative flag
        this.updateNegZeroFlag(ra);
    }

    /**
     * 更新负标识和零标识
     */
    private void updateNegZeroFlag(int result) {
        //Upate ZeroFlag
        if (result == 0) {
            this.status |= 0b0000_0010;
        } else {
            this.status &= 0b1111_1101;
        }

        //Update Negative Flag
        if ((result & 0b0100_0000) != 0) {
            this.status |= 0b0100_0000;
        } else {
            this.status &= 0b0011_1111;
        }

    }

    private void writerMem(int pos, byte b) {
        this.memory[pos] = b;
    }

    /**
     * 根据指定寻址模式获取操作数地址,
     * <a href="https://www.nesdev.org/obelisk-6502-guide/addressing.html#IMM">相关开发文档.</a></p>
     */
    private int getOperandAddr(AddressModel model) {
        return switch (model) {
            case Immediate -> this.pc;
            case ZeroPage -> this.readUMem(this.pc);
            case Absolute -> this.readLMem(this.pc);
            case ZeroPage_X -> this.readUMem(this.pc) + this.rx;
            case ZeroPage_Y -> this.readUMem(this.pc) + this.ry;
            case Absolute_X -> this.readLMem(this.pc) + this.rx;
            case Absolute_Y -> this.readLMem(this.pc) + this.ry;
            case Indirect_X -> {
                var base = this.readMem(this.pc);
                var ptr = base + this.ra;
                var l = this.readMem(ptr);
                var h = this.readMem(ptr + 1);
                yield h << 8 | l;
            }
            case Indirect_Y -> {
                var base = this.readMem(this.pc);

                var l = this.readMem(base);
                var h = this.readMem((base)) + 1;
                var temp = (h) << 8 | l;
                yield temp + this.ry;
            }
            default -> -1;
        };
    }

    private void brk() {
        //设置中断状态为1
        this.status |= 0b0000_1000;
    }

    /**
     * 逻辑运算 或、与、异或
     */
    private void logic(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var a = this.ra;
        var b = this.readMem(address);
        var instruction = instruction6502.getInstruction();
        var c = switch (instruction) {
            case EOR -> a ^ b;
            case ORA -> a | b;
            case AND -> a & b;
            default -> a;
        };
        this.updatera(c);
    }

    private void asl(Instruction6502 instruction6502) {
        final byte b;
        var a = instruction6502.getAddressModel() == AddressModel.Accumulator;
        var address = -1;
        if (a) {
            b = (byte) this.ra;
        } else {
            address = this.getOperandAddr(instruction6502.getAddressModel());
            b = this.readMem(address);
        }
        var c = b & 0b1000_0000;
        //更新进位标识
        this.status |= c;
        //左移1位
        c = b << 1;
        if (a) {
            this.updatera(c);
        } else {
            this.writerMem(address, (byte) c);
        }
    }

    private void push(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        if (instruction == CPUInstruction.PHA) {
            this.memory[this.sp] = (byte) this.sp;
        } else {
            this.memory[this.sp] = (byte) this.status;
        }
        this.sp++;
    }

    private void pull(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        if (instruction == CPUInstruction.PLA) {
            this.updatera(this.memory[this.sp]);
        } else {
            this.status = this.memory[this.sp];
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
        var b = this.readMem(address);
        //设置Carry Flag
        if (a >= b) {
            this.status |= 0b0000_0001;
        }
        //设置Zero Flag
        if (a == b) {
            this.status |= 0b0000_0010;
        }
    }

    private void inc(Instruction6502 instruction6502) {
        final CPUInstruction instruction = instruction6502.getInstruction();
        final int result;
        if (instruction == CPUInstruction.INC) {
            var address = this.getOperandAddr(instruction6502.getAddressModel());
            var a = this.readMem(address);
            result = a + 1;
            this.writerMem(address, ByteUtil.overflow(result));
        } else if (instruction == CPUInstruction.INX) {
            var x = this.rx;
            result = x + 1;
            this.rx = result;
        } else if (instruction == CPUInstruction.INY) {
            var y = this.ry;
            result = y + 1;
            this.ry = result;
        } else {
            log.warn("Unknown inc instruction:0x[{}] alia:[{}].", instruction6502.getOpenCode(), instruction);
            return;
        }
        //设置Zero Flag
        if (result == 0) {
            this.status |= 0b0000_0010;
        }
        //设置Negative Flag
        if ((result & 0b0100_0000) != 0) {
            this.status |= 0b0100_0000;
        }
    }


    private void adc(AddressModel model) {
        var address = this.getOperandAddr(model);
        var m = this.readMem(address);
        var c = (this.status & 0b0000_0001) > 0 ? 1 : 0;
        var sum = this.ra + m + c;
        //If result overflow set carry-bit else remove carry bit.
        if (sum > 0xff) {
            this.status |= 0b0000_0001;
        } else {
            this.status &= 0b1111_1110;
        }
        //Set if sign-bit incorrect
        if ((m ^ sum & (sum ^ this.ra)) != 0) {
            this.status |= 0b0001_0000;
        } else {
            this.status &= 0b1110_1111;
        }
        this.ra = sum;
    }

    private void loadXY(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var data = this.readMem(address);
        if (instruction6502.getInstruction() == CPUInstruction.LDX) {
            this.rx = data;
        } else {
            this.ry = data;
        }
        if (data == 0) {
            this.status |= 0b0000_0001;
        }
        if ((data & 0b0100_0000) > 0) {
            this.status |= 0b0100_0000;
        }
    }

    private void branch(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        //不成立
        if ((instruction == CPUInstruction.BCC && (this.status & 0b0000_0001) != 0)
                || (instruction == CPUInstruction.BCS && (this.status & 0b0000_0001) == 0)
                || (instruction == CPUInstruction.BEQ && (this.status & 0b0000_0010) == 0)
                || (instruction == CPUInstruction.BMI && (this.status & 0b0100_0000) != 0)
                || (instruction == CPUInstruction.BNE && (this.status & 0b0000_0010) != 0)
                || (instruction == CPUInstruction.BPL && (this.status & 0b0100_0000) != 0)
                || (instruction == CPUInstruction.BVC && (status & 0b0001_0000) != 0)
                || instruction == CPUInstruction.BVS && (status & 0b0001_000) == 0) {
            return;
        }
        var jump = this.readMem(this.pc);
        this.pc = this.pc + 1 + jump;
    }

    private void bit(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var value = this.readMem(address);

        if (value < 0) {
            this.status |= 0b0100_0000;
        }

        if ((value & 0b0010_0000) != 0) {
            this.status |= 0b0010_0000;
        }
        var and = value & this.ra;
        if (and == 0) {
            this.status |= 0b0000_0010;
        } else {
            this.status &= 0b0111_1101;
        }
    }

    private void dec(Instruction6502 instruction6502) {
        var instruction = instruction6502.getInstruction();
        var value = switch (instruction) {
            case DEC -> {
                var address = getOperandAddr(instruction6502.getAddressModel());
                var b = this.readMem(address);
                b--;
                this.writerMem(address, b);
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
        if (value == 0) {
            this.status |= 0b0000_00010;
        }
        if (value < 0) {
            this.status |= 0b0100_0000;
        }
    }

    private void jump(Instruction6502 instruction6502) {
        final int address;
        if (instruction6502.getAddressModel() == AddressModel.Absolute) {
            address = this.getOperandAddr(AddressModel.Absolute);
        } else {
            var lsb = this.readMem(this.pc);
            var msb = this.readMem(this.pc + 1);
            address = ByteUtil.toInt(new byte[]{lsb, msb});
        }
        this.pc = address;
    }


    private void run() {
        while (true) {
            var openCode = this.readMem(this.pc);
            this.pc += 1;
//            log.debug("Current pc:{}.", this.pc);
            var instruction6502 = CPUInstruction.getInstance(openCode);
            if (instruction6502 == null) {
                continue;
            }
            var instruction = instruction6502.getInstruction();
            if (instruction != CPUInstruction.BRK)
                log.debug("Prepare execute instruction [0x{}] alias [{}].", Integer.toHexString(instruction6502.getOpenCode()), instruction);
            if (instruction == CPUInstruction.LDA) {
                this.lda(instruction6502);
            }
            if (instruction == CPUInstruction.ADC) {
                this.adc(instruction6502.getAddressModel());
            }
            if (instruction == CPUInstruction.BRK) {
                this.brk();
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

            //刷新累加寄存器值到内存
            if (instruction == CPUInstruction.STA) {
                this.sta(instruction6502);
            }

            //刷新y寄存器值到内存
            if (instruction == CPUInstruction.STY) {
                var address = this.getOperandAddr(instruction6502.getAddressModel());
                this.writerMem(address, ByteUtil.overflow(this.ry));
            }

            //清除进位标识
            if (instruction == CPUInstruction.CLC) {
                this.status &= 0b0111_1110;
            }

            //清除Decimal model
            if (instruction == CPUInstruction.CLD) {
                this.status &= 0b0111_0111;
            }

            //清除中断标识
            if (instruction == CPUInstruction.CLI) {
                this.status &= 0b0111_1011;
            }

            if (instruction == CPUInstruction.CLV) {
                this.status &= 0b0110_1111;
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
                this.stackLPush(this.pc + 2 - 1);
                this.pc = this.readLMem(this.pc);
            }

            if (instruction == CPUInstruction.RTS) {
                this.pc = this.stackLPop() + 1;
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
                this.status = this.stackPop();
                //取消中断标识
                this.status &= 0b01110_1111;
                this.pc = this.stackPop();
            }

            if (instruction == CPUInstruction.SEC) {
                this.status |= 0b0000_0001;
            }

            if (instruction == CPUInstruction.SED) {
                this.status |= 0b0000_1000;
            }

            if (instruction == CPUInstruction.SEI) {
                this.status |= 0b0000_0100;
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
                    r = this.ra = this.rx;
                else if (instruction == CPUInstruction.TXS)
                    r = this.sp = this.rx;
                else
                    r = this.rx = this.ry;

                this.updateNegZeroFlag(r);
            }
        }
    }
}
