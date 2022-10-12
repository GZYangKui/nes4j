package cn.navclub.nes4j.bin.core.registers;

import cn.navclub.nes4j.bin.enums.CPUStatus;
import cn.navclub.nes4j.bin.util.ByteUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * CPU状态管理
 */
@Data
@Slf4j
public class CSRegister {
    private byte bits;

    /**
     * 清除某个标识位
     */
    public void clear(CPUStatus flag) {
        this.bits &= (0xff - (int) (Math.pow(2, flag.ordinal())));
    }

    /**
     *
     * 批量清除标识
     *
     */
    public void clear(CPUStatus... cs) {
        for (CPUStatus c : cs) {
            clear(c);
        }
    }

    /**
     * 设置某个标识位
     */
    public void set(CPUStatus status) {
        this.bits |= (1 << status.ordinal());
    }

    public void update(CPUStatus status, boolean set) {
        if (set) {
            this.set(status);
        } else {
            this.clear(status);
        }
    }

    /**
     * 判断某个标识位是否设置
     */
    public boolean contain(CPUStatus flag) {
        return ((1 << flag.ordinal()) & bits) > 0;
    }

    public int get(CPUStatus flag) {
        return this.contain(flag) ? 1 : 0;
    }

    public void reset() {
        this.bits = 0;
    }

    @Override
    public String toString() {
        return ByteUtil.toBinStr(this.bits);
    }
}
