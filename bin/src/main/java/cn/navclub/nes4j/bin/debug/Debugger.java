package cn.navclub.nes4j.bin.debug;

import cn.navclub.nes4j.bin.NES;
import cn.navclub.nes4j.bin.core.Bus;
import cn.navclub.nes4j.bin.core.CPU;

public interface Debugger {
    /**
     *
     * {@link CPU#next()}执行前调用该方法
     *
     * @return 如果返回 {@code true} 则阻塞当前处理器,否则继续执行
     */
    boolean hack(Bus bus);

    /**
     *
     * 当rpg-rom发生改变时调用该函数
     *
     * @param buffer 改变后的rpg-rom数据
     * @param offset 偏移量
     */
    void buffer(byte[] buffer, int offset);

    /**
     *
     * 注入NES对象实例
     *
     */
    void inject(NES instance);
}
