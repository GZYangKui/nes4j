package cn.navclub.nes4j.bin.config;

import lombok.Data;
import lombok.Getter;

/**
 * CPU状态管理
 */
@Data
public class CPUStatus {
    private byte value;

    /**
     * 清除某个标识位
     */
    public void clearFlag(BIFlag flag) {
        this.value &= switch (flag) {
            case CARRY_FLAG -> 0b0111_1110;
            case ZERO_FLAG -> 0b0111_1101;
            case INTERRUPT_DISABLE -> 0b0111_1011;
            case DECIMAL_MODE -> 0b0111_0111;
            case BREAK_COMMAND -> 0b0110_1111;
            case OVERFLOW_FLAG -> 0b0101_1111;
            case NEGATIVE_FLAG -> 0b0011_1111;
        };
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
