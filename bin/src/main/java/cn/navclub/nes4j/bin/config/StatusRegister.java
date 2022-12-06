package cn.navclub.nes4j.bin.config;

import cn.navclub.nes4j.bin.util.BinUtil;
import lombok.Getter;
import lombok.Setter;

public class StatusRegister<T extends Enum<?>> {
    @Getter
    @Setter
    protected byte bits;

    public StatusRegister(byte bits) {
        this.bits = bits;
    }

    public StatusRegister() {
    }

    public final void set(T instance) {
        this.bits |= (1 << instance.ordinal());
    }

    @SafeVarargs
    public final void set(T... is) {
        for (T i : is) {
            this.set(i);
        }
    }

    /**
     * 清除某个标识位
     */
    public final void clear(T instance) {
        this.bits &= (0xff - (int) (Math.pow(2, instance.ordinal())));
    }

    /**
     * 批量清除标识
     */
    @SafeVarargs
    public final void clear(T... is) {
        for (T c : is) {
            clear(c);
        }
    }

    /**
     * 更新某个标识
     */
    public final void update(T instance, boolean set) {
        if (set) {
            this.set(instance);
        } else {
            this.clear(instance);
        }
    }

    /**
     * 获取某个标识的值
     */
    public final int get(T flag) {
        return this.contain(flag) ? 1 : 0;
    }

    /**
     * 重置当前状态
     */
    public final void reset() {
        this.bits = 0b100100;
    }

    /**
     * 判断某个标识是否设置
     */
    public final boolean contain(T instance) {
        return ((1 << instance.ordinal()) & bits) > 0;
    }

    @Override
    public String toString() {
        return BinUtil.toBinStr(this.bits);
    }

    public final StatusRegister<T> _clone() {
        return new StatusRegister<>(this.bits);
    }
}
