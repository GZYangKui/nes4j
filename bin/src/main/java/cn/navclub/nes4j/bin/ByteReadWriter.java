package cn.navclub.nes4j.bin;

public interface ByteReadWriter {
    /**
     * 向指定位置写入一个字节
     */
    void write(int address, byte b);

    /**
     * 从指定位置读取一个字节
     */
    byte read(int address);

    /**
     * 向指定位置写入无符号字节
     */
    default void writeUSByte(int address, int value) {
        this.write(address, (byte) value);
    }

    /**
     * 向目标地址写入双字节
     */
    default void writeInt(int address, int value) {
        var lsb = (byte) (value & 0xff);
        var msb = (byte) (value >> 8 & 0xff);
        this.write(address, lsb);
        this.write(address + 1, msb);
    }

    /**
     * 读无符号字节
     */
    default int readUSByte(int address) {
        return this.read(address) & 0xff;
    }

    /**
     * 以小端序形式读取数据
     */
    default int readInt(int address) {
        var lsb = this.read(address);
        var msb = this.read(address + 1);
        return (lsb & 0xff) | ((msb & 0xff) << 8);
    }
}
