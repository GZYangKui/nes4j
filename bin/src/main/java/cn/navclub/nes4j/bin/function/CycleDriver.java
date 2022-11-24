package cn.navclub.nes4j.bin.function;

public interface CycleDriver {
    /**
     *
     * 该函数仅在需要时钟传递场景使用
     *
     * @param cycle 传递时钟值
     */
    default void tick(int cycle) {

    }

    /**
     *
     * 该函数使用无需时钟传递场景使用
     *
     */
    default void tick() {

    }
}
