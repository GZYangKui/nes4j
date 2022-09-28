package cn.navclub.nes4j.bin.core.registers;

import lombok.Data;

/**
 * CPU状态管理
 */
@Data
public class CSRegister {
    private byte value;

    /**
     * 清除某个标识位
     */
    public void clearFlag(BIFlag flag) {
        this.value &= (0xff - (int) (Math.pow(2, flag.ordinal())));
    }

    /**
     * 设置某个标识位
     */
    public void setFlag(BIFlag flag) {
        this.value |= (1 << flag.ordinal());
    }

    public void update(BIFlag flag, boolean set) {
        if (set) {
            this.setFlag(flag);
        } else {
            this.clearFlag(flag);
        }
    }

    /**
     * 判断某个标识位是否设置
     */
    public boolean hasFlag(BIFlag flag) {
        return ((1 << flag.ordinal()) & value) > 0;
    }

    public int getFlagBit(BIFlag flag) {
        return this.hasFlag(flag) ? 1 : 0;
    }

    public void reset() {
        this.value = 0;
    }

    public enum BIFlag {
        CARRY_FLAG,
        ZERO_FLAG,
        INTERRUPT_DISABLE,
        DECIMAL_MODE,
        BREAK_COMMAND,
        OVERFLOW_FLAG,
        NEGATIVE_FLAG
    }
}
