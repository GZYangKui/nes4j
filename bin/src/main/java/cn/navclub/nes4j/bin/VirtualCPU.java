package cn.navclub.nes4j.bin;

import cn.navclub.nes4j.bin.config.AddressModel;
import cn.navclub.nes4j.bin.util.ByteUtil;

/**
 * 模拟CPU信息.
 * <b>模拟CPU可寻址范围为0-65535个内存单元,也就是说一个地址需要2个字节来存储.
 * NES CPU采用小端序寻址,这就意味着地址的8个最低有效位存储在8个最高有效位之前</b>
 */
public class VirtualCPU {
    private static final int SAFE_POINT = 0xFFFC;

    //累加寄存器
    private int rax;
    //程序计数器
    private int rcx;
    //X寄存器 用作特定内存寻址模式中的偏移量。可用于辅助存储需求（保存温度值、用作计数器等）
    private int rx;
    //Y寄存器
    private int ry;
    //栈指针寄存器,始终指向栈顶
    private int rbp;
    //状态寄存器
    private int cf;

    //内存区间64kb
    private final byte[] memory;

    public VirtualCPU() {
        this.memory = new byte[0xFFFF];
    }

    public void loadRun(byte[] arr) {
        var index = 0x8000;
        System.arraycopy(arr, index, this.memory, index, index + arr.length - index);
        this.writerMemLE(SAFE_POINT, new byte[]{0x00, ByteUtil.overflow(0x80)});
        this.reset();
        this.run();
    }


    /**
     * 重置寄存器和程序计数器
     */
    public void reset() {
        this.rx = 0;
        this.ry = 0;
        this.cf = 0;
        this.rax = 0;
        this.rbp = 0;
        this.rcx = this.readMemLE(SAFE_POINT);
    }

    /**
     * 从内存中读取单个字节数据
     */
    private byte readMem(int addr) {
        return this.memory[addr];
    }

    /**
     * 以小端序读取地址
     */
    private short readMemLE(int addr) {
        var l = this.readMem(addr);
        var h = this.readMem((short) (addr + 1));
        return (short) (h >> 8 | l);
    }

    /**
     * 以小端序写入数据
     */
    private void writerMemLE(int pos, byte[] arr) {
        var lb = arr[0];
        var hb = arr[1];
        this.writerMem(pos, lb);
        this.writerMem(pos + 1, hb);
    }

    private void writerMem(int pos, byte b) {
        this.memory[pos] = b;
    }

    /**
     * 根据指定寻址模式获取操作数字
     */
    private short getOperandAddr(AddressModel model) {
        return 0;
    }


    private void run() {
        while (true) {
            var openCode = this.readMem(this.rcx);
            this.rcx++;

        }
    }
}
