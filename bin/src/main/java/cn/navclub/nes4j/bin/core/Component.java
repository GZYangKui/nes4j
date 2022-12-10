package cn.navclub.nes4j.bin.core;

import cn.navclub.nes4j.bin.function.CycleDriver;

/**
 *
 * NES系统核心组件功能抽象
 *
 */
public interface Component extends CycleDriver {
    /**
     * 向指定位置写入一个字节
     */
    void write(int address, byte b);

    /**
     * 从指定位置读取一个字节
     */
    byte read(int address);

    /**
     * 组件停止时触发资源清除工作
     */
    default void stop() {

    }

    /**
     * 组件触发重置
     */
    default void reset() {

    }

    /**
     *
     * 组件快照,用于存档使用
     *
     */
    default byte[] snapshot() {
        return null;
    }

    /**
     *
     * 加载组件快照,用于恢复存档
     *
     */
    default void load(byte[] snapshot) {

    }
}