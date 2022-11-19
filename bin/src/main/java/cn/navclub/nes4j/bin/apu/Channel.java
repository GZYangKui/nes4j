package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.NESystemComponent;
import cn.navclub.nes4j.bin.core.APU;
import lombok.Getter;

public abstract class Channel implements NESystemComponent {
    @Getter
    protected final Divider divider;
    //储存四个寄存器的值
    protected byte[] value;
    protected final APU apu;
    @Getter
    protected boolean lock;

    public Channel(final APU apu) {
        this.apu = apu;
        this.value = new byte[4];
        this.divider = new Divider();
    }

    @Override
    public byte read(int address) {
        throw new RuntimeException("Write-only register.");
    }

    @Override
    public void tick(int cycle) {

    }

    /**
     *
     *  当前通道输出
     *
     */
    public int output() {
        return 0;
    }
}
