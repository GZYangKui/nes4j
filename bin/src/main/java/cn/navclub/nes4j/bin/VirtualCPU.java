package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.config.AddressModel;
import cn.navclub.nes4j.bin.enums.CPUInstruction;
import cn.navclub.nes4j.bin.model.Instruction6502;
import cn.navclub.nes4j.bin.util.ByteUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 模拟CPU信息.
 * <b>模拟CPU可寻址范围为0-65535个内存单元,也就是说一个地址需要2个字节来存储.
 * NES CPU采用小端序寻址,这就意味着地址的8个最低有效位存储在8个最高有效位之前</b>
 */
@Slf4j
public class VirtualCPU {
    private static final int SAFE_POINT = 0xFFFC;

    //累加寄存器
    private int rax;
    //程序计数器
    private int rcx;
    //X寄存器 用作特定内存寻址模式中的偏移量。可用于辅助存储需求（保存温度值、用作计数器等）
    private int rx;
    //Y寄存器
    private int ry;
    //栈指针寄存器,始终指向栈顶
    private int rbp;
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
    private int cf;

    //内存区间64kb
    private final byte[] memory;

    public VirtualCPU() {
        this.memory = new byte[0xFFFF];
    }

    public void loadRun(byte[] arr) {
        var index = 0x8000;
        System.arraycopy(arr, 0, this.memory, index, arr.length);
        this.writerMemLE(SAFE_POINT, new byte[]{0x00, ByteUtil.overflow(0x80)});
        this.reset();
        this.run();
    }


    /**
     * 重置寄存器和程序计数器
     */
    public void reset() {
        this.rx = 0;
        this.ry = 0;
        this.cf = 0;
        this.rax = 0;
        this.rbp = 0;
        this.rcx = this.readMemLE(SAFE_POINT);
    }

    /**
     * 从内存中读取单个字节数据
     */
    private byte readMem(int addr) {
        return this.memory[addr];
    }

    /**
     * 以小端序读取地址
     */
    private int readMemLE(int addr) {
        var l = Byte.toUnsignedInt(this.readMem(addr));
        var h = Byte.toUnsignedInt(this.readMem((addr + 1)));
        return (h >> 8 | l);
    }

    /**
     * 以小端序写入数据
     */
    private void writerMemLE(int pos, byte[] arr) {
        var lb = arr[0];
        var hb = arr[1];
        this.writerMem(pos, lb);
        this.writerMem(pos + 1, hb);
    }

    /**
     * LDA指令实现
     */
    private void lda(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var value = this.readMem(address);
        this.updateRax(value);
    }

    private void updateRax(int rax) {
        if (rax == 0) {
            this.cf |= 0b0000_00010;
        }
        if ((rax & 0b0100_0000) != 0) {
            this.cf |= 0b0100_0000;
        }
        //Set rax
        this.rax = rax;
        //Update Zero and negative flag
        this.updateNegZeroFlag(rax);
    }

    /**
     * 更新负标识和零标识
     */
    private void updateNegZeroFlag(int result) {
        //Upate ZeroFlag
        if (result == 0) {
            this.cf |= 0b0000_0010;
        } else {
            this.cf &= 0b1111_1101;
        }

        //Update Negative Flag
        if ((result & 0b0100_0000) != 0) {
            this.cf |= 0b0100_0000;
        } else {
            this.cf &= 0b0011_1111;
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
            case Immediate -> this.rcx;
            case ZeroPage -> this.readMem(this.rcx);
            case Absolute -> this.readMemLE(this.rcx);
            case ZeroPage_X -> this.readMem(this.rcx) + this.rx;
            case ZeroPage_Y -> this.readMem(this.rcx) + this.ry;
            case Absolute_X -> this.readMemLE(this.rcx) + this.rx;
            case Absolute_Y -> this.readMemLE(this.rcx) + this.ry;
            case Indirect_X -> {
                var base = this.readMem(this.rcx);
                var ptr = base + this.rax;
                var l = this.readMem(ptr);
                var h = this.readMem(ptr + 1);
                yield h << 8 | l;
            }
            case Indirect_Y -> {
                var base = this.readMem(this.rcx);

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
        this.cf |= 0b0000_1000;
    }

    private void and(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var a = this.rax;
        var b = this.readMem(address);
        var c = a & b;

        this.updateRax(c);
    }

    private void ora(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var a = this.rax;
        var b = this.readMem(address);
        this.updateRax(a | b);
    }

    private void eor(Instruction6502 instruction6502) {
        var address = this.getOperandAddr(instruction6502.getAddressModel());
        var a = this.rax;
        var b = this.readMem(address);
        this.updateRax(a ^ b);
    }

    private void adc(AddressModel model) {
        var address = this.getOperandAddr(model);
        var m = this.readMem(address);
        var c = (this.cf & 0b0000_0001) > 0 ? 1 : 0;
        var sum = this.rax + m + c;
        //If result overflow set carry-bit else remove carry bit.
        if (sum > 0xff) {
            this.cf |= 0b0000_0001;
        } else {
            this.cf &= 0b1111_1110;
        }
        //Set if sign-bit incorrect
        if ((m ^ sum & (sum ^ this.rax)) != 0) {
            this.cf |= 0b0001_0000;
        } else {
            this.cf &= 0b1110_1111;
        }
        this.rax = sum;
    }


    private void run() {
        while (true) {
            var openCode = this.readMem(this.rcx);
            this.rcx += 1;
            var instruction6502 = CPUInstruction.getInstance(openCode);
            if (instruction6502 == null) {
                continue;
            }
            log.debug("Prepare execute instruction [0x{}] alias [{}].", Integer.toHexString(instruction6502.getOpenCode()), instruction6502.getInstruction());
            var instruction = instruction6502.getInstruction();
            if (instruction == CPUInstruction.LDA) {
                this.lda(instruction6502);
            }
            if (instruction == CPUInstruction.ADC) {
                this.adc(instruction6502.getAddressModel());
            }
            if (instruction == CPUInstruction.BRK) {
                this.brk();
            }
            if (instruction == CPUInstruction.AND) {
                this.and(instruction6502);
            }
            if (instruction == CPUInstruction.ORA) {
                this.ora(instruction6502);
            }
            if (instruction == CPUInstruction.EOR) {
                this.eor(instruction6502);
            }

            this.rcx++;
        }
    }
}
