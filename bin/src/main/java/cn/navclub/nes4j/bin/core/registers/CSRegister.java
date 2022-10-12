package cn.navclub.nes4j.bin.core.registers;

import cn.navclub.nes4j.bin.enums.CPUStatus;
import cn.navclub.nes4j.bin.util.ByteUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * CPU状态管理
 *
 */
@Getter
@Setter
@Slf4j
public class CSRegister {
    private byte value;

    /**
     * 清除某个标识位
     */
    public void clear(CPUStatus flag) {
        this.value &= (0xff - (int) (Math.pow(2, flag.ordinal())));
    }

    /**
     * 设置某个标识位
     */
    public void set(CPUStatus status) {
        this.value |= (1 << status.ordinal());
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
        return ((1 << flag.ordinal()) & value) > 0;
    }

    public int get(CPUStatus flag) {
        return this.contain(flag) ? 1 : 0;
    }

    public void reset() {
        this.value = 0;
    }

    @Override
    public String toString() {
        return ByteUtil.toBinStr(this.value);
    }
}
