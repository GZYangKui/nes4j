package cn.navclub.nes4j.bin.enums;

import cn.navclub.nes4j.bin.model.Instruction6502;
import cn.navclub.nes4j.bin.util.ByteUtil;
import lombok.extern.slf4j.Slf4j;

import static cn.navclub.nes4j.bin.enums.AddressModel.*;


/**
 * 6502 指令集
 */
@Slf4j
public enum CPUInstruction {
    /**
     * More detail please visit:<a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#ADC">ADC Document</a>
     */
    ADC(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0x69), Immediate),
            Instruction6502.create(ByteUtil.overflow(0x65), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0x75), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0x6D), Absolute),
            Instruction6502.create(ByteUtil.overflow(0x7D), Absolute_X),
            Instruction6502.create(ByteUtil.overflow(0x79), Absolute_Y),
            Instruction6502.create(ByteUtil.overflow(0x61), Indirect_X),
            Instruction6502.create(ByteUtil.overflow(0x71), Indirect_Y),
    }),
    /**
     * 加载一字节数据进入累加器并视情况而定是否需要设置零或者负数flags.
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#LDA">相关文档</a>
     */
    LDA(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0xA9), Immediate),
            Instruction6502.create(ByteUtil.overflow(0xA5), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0xB5), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0xAD), Absolute),
            Instruction6502.create(ByteUtil.overflow(0xBD), Absolute_X),
            Instruction6502.create(ByteUtil.overflow(0xB9), Absolute_Y),
            Instruction6502.create(ByteUtil.overflow(0xA1), Indirect_X),
            Instruction6502.create(ByteUtil.overflow(0xB1), Indirect_Y),
    }),
    /**
     * 将累加器中的值写入内存中
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#STA">相关文档</a>
     */
    STA(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0x85), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0x95), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0x8D), Absolute),
            Instruction6502.create(ByteUtil.overflow(0x9D), Absolute_X),
            Instruction6502.create(ByteUtil.overflow(0x99), Absolute_Y),
            Instruction6502.create(ByteUtil.overflow(0x81), Indirect_X),
            Instruction6502.create(ByteUtil.overflow(0x91), Indirect_Y)
    }
    ),
    /**
     * 将寄存器Y中的值刷新到内存中
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#STY">相关文档</a>
     */
    STY(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0x84), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0x94), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0X8C), Absolute)
    }),
    /**
     * 与逻辑指令实现
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#AND">相关文档</a>
     */
    AND(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0x29), Immediate),
            Instruction6502.create(ByteUtil.overflow(0x25), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0x35), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0x2D), Absolute),
            Instruction6502.create(ByteUtil.overflow(0x3D), Absolute_X),
            Instruction6502.create(ByteUtil.overflow(0x39), Absolute_Y),
            Instruction6502.create(ByteUtil.overflow(0x21), Indirect_X),
            Instruction6502.create(ByteUtil.overflow(0x31), Indirect_Y),
    }),
    /**
     * 或指令实现
     */
    ORA(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0x09), Immediate),
            Instruction6502.create(ByteUtil.overflow(0x05), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0x15), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0x0D), Absolute),
            Instruction6502.create(ByteUtil.overflow(0x1D), Absolute_X),
            Instruction6502.create(ByteUtil.overflow(0x19), Absolute_Y),
            Instruction6502.create(ByteUtil.overflow(0x01), Indirect_X),
            Instruction6502.create(ByteUtil.overflow(0x11), Indirect_Y)
    }
    ),
    /**
     * 异或逻辑实现
     */
    EOR(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0x49), Immediate),
            Instruction6502.create(ByteUtil.overflow(0x45), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0x55), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0x4D), Absolute),
            Instruction6502.create(ByteUtil.overflow(0x5D), Absolute_X),
            Instruction6502.create(ByteUtil.overflow(0x59), Absolute_Y),
            Instruction6502.create(ByteUtil.overflow(0x41), Indirect_X),
            Instruction6502.create(ByteUtil.overflow(0x51), Indirect_Y)
    }
    ),
    /**
     * 将累加寄存器中的值push到堆栈上
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#PHA">相关文档</a>
     */
    PHA(ByteUtil.overflow(0x48)),
    /**
     * 将状态寄存器push到堆栈上
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#PHP">相关文档</a>
     */
    PHP(ByteUtil.overflow(0X08)),
    /**
     * 从系统栈中获取值并更新累加寄存器
     *
     * <a href="href="https://www.nesdev.org/obelisk-6502-guide/reference.html#PLA">相关文档</a>
     */
    PLA(ByteUtil.overflow(0x68)),
    /**
     * 从系统栈中读取值并更新状态寄存器
     * <a href="href="https://www.nesdev.org/obelisk-6502-guide/reference.html#PLP">相关文档</a>
     */
    PLP(ByteUtil.overflow(0x28)),

    /**
     * 将操作数所有字节左移一位
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#ASL">相关文档</a>
     */
    ASL(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0x0A), Accumulator),
            Instruction6502.create(ByteUtil.overflow(0x06), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0x16), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0x0E), Absolute),
            Instruction6502.create(ByteUtil.overflow(0x1E), Absolute_X)
    }),
    /**
     * 累加寄存器与指定内存值比较
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#CMP">相关文档</a>
     */
    CMP(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0xC9), Immediate),
            Instruction6502.create(ByteUtil.overflow(0xC5), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0xD5), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0xCD), Absolute),
            Instruction6502.create(ByteUtil.overflow(0xDD), Absolute_X),
            Instruction6502.create(ByteUtil.overflow(0xD9), Absolute_Y),
            Instruction6502.create(ByteUtil.overflow(0xC1), Indirect_X),
            Instruction6502.create(ByteUtil.overflow(0xD1), Indirect_Y)
    }),
    /**
     * 比较X寄存器与指定内存的值
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#CMX">相关文档</a>
     */
    CPX(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0xE0), Immediate),
            Instruction6502.create(ByteUtil.overflow(0xE4), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0xEC), Absolute),
    }),
    /**
     * 比较y寄存器与内存中的值
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#CMY">相关文档</a>
     */
    CPY(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0xE0), Immediate),
            Instruction6502.create(ByteUtil.overflow(0xE4), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0xEC), Absolute),
    }
    ),
    /**
     * 自增指令
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#INC">相关文档</a>
     */
    INC(new Instruction6502[]{
            Instruction6502.create(ByteUtil.overflow(0xF6), ZeroPage_X),
            Instruction6502.create(ByteUtil.overflow(0xFE), Absolute_X),
            Instruction6502.create(ByteUtil.overflow(0xE6), ZeroPage),
            Instruction6502.create(ByteUtil.overflow(0xEE), Absolute),
    }
    ),
    /**
     * 清除进位标识
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#CLC">相关文档</a>
     */
    CLC(ByteUtil.overflow(0x18)),
    /**
     * 清除数字标识
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#CLD">相关文档</a>
     */
    CLD(ByteUtil.overflow(0xD8)),
    /**
     * 清除中断标识
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#CLI">相关文档</a>
     */
    CLI(ByteUtil.overflow(0x58)),
    /**
     * 清除溢出标识
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#CLV">相关文档</a>
     */
    CLV(ByteUtil.overflow(0xB8)),

    /**
     * 自增X寄存器
     *
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#INX">相关文档</a>
     */
    INX(ByteUtil.overflow(0XE8)),
    /**
     * 自增Y寄存器
     * <a href="https://www.nesdev.org/obelisk-6502-guide/reference.html#INY">相关文档</a>
     */
    INY(ByteUtil.overflow(0xC8)),

    /**
     * 中断指令
     */
    BRK(ByteUtil.overflow(0x00));

    private final Instruction6502 instruction6502;
    private final Instruction6502[] instruction6502s;

    CPUInstruction(Instruction6502 instruction6502, Instruction6502[] instruction6502s) {
        this.instruction6502 = instruction6502;
        this.instruction6502s = instruction6502s;
        if (this.instruction6502s != null) {
            for (Instruction6502 instruction65021 : this.instruction6502s) {
                instruction65021.setInstruction(this);
            }
        } else {
            this.instruction6502.setInstruction(this);
        }
    }

    CPUInstruction(byte openCode) {
        this.instruction6502 = new Instruction6502(openCode, this);
        this.instruction6502s = null;
    }

    CPUInstruction(Instruction6502[] instruction6502s) {
        this(null, instruction6502s);
    }

    CPUInstruction(Instruction6502 instruction6502) {
        this.instruction6502s = null;
        this.instruction6502 = instruction6502;
    }

    public static Instruction6502 getInstance(byte openCode) {
        for (CPUInstruction value : values()) {
            var parent = value.instruction6502;
            if (parent != null) {
                if (openCode == parent.getOpenCode()) {
                    return parent;
                }
            } else {
                for (Instruction6502 instruction6502 : value.instruction6502s) {
                    if (instruction6502.getOpenCode() == openCode) {
                        return instruction6502;
                    }
                }
            }
        }
//        log.warn("Unknown instruction:[0x{}]", Integer.toHexString(openCode));
//        throw new NullPointerException("Unknown instruction:[0x" + Integer.toHexString(openCode) + "]");
        return null;
    }
}
