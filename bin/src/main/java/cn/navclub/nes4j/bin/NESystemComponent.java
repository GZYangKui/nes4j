package cn.navclub.nes4j.bin;

/**
 *
 * NES系统核心组件功能抽象
 *
 */
public interface NESystemComponent {
    /**
     * 向指定位置写入一个字节
     */
    void write(int address, byte b);

    /**
     * 从指定位置读取一个字节
     */
    byte read(int address);

    /**
     * 由CPU时钟驱动该函数
     */
    void tick(int cycle);
}
