package cn.navclub.nes4j.bin.function;

/**
 * 实现该接口的类均可被CPU时钟驱动
 */
@FunctionalInterface
public interface CycleDriver {
    /**
     * CPU时钟发生改变回调当前方法
     */
    void tick(int cycle);
}
