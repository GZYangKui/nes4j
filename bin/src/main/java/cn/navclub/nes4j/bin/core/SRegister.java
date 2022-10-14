package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.util.ByteUtil;
import lombok.Getter;
import lombok.Setter;

public class SRegister {
    @Getter
    @Setter
    protected byte bits;

    public void set(Enum<?> instance) {
        this.bits |= (1 << instance.ordinal());
    }

    public void set(Enum<?>... is) {
        for (Enum<?> i : is) {
            this.set(i);
        }
    }

    /**
     * 清除某个标识位
     */
    public void clear(Enum<?> instance) {
        this.bits &= (0xff - (int) (Math.pow(2, instance.ordinal())));
    }

    /**
     * 批量清除标识
     */
    public void clear(Enum<?>... is) {
        for (Enum<?> c : is) {
            clear(c);
        }
    }

    /**
     * 更新某个标识
     */
    public void update(Enum<?> instance, boolean set) {
        if (set) {
            this.set(instance);
        } else {
            this.clear(instance);
        }
    }

    /**
     * 获取某个标识的值
     */
    public int get(Enum<?> flag) {
        return this.contain(flag) ? 1 : 0;
    }

    /**
     * 重置当前状态
     */
    public void reset() {
        this.bits = 0;
    }

    /**
     * 判断某个标识是否设置
     */
    public boolean contain(Enum<?> instance) {
        return ((1 << instance.ordinal()) & bits) > 0;
    }

    @Override
    public String toString() {
        return ByteUtil.toBinStr(this.bits);
    }
}
