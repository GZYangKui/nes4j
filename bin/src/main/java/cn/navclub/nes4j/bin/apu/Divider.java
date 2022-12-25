package cn.navclub.nes4j.bin.apu;

import cn.navclub.nes4j.bin.function.CycleDriver;
import lombok.Getter;
import lombok.Setter;

/**
 * 分频器实现
 */
public class Divider implements CycleDriver {
    private int counter;
    //改变周期并不会影响当前计数
    @Setter
    @Getter
    private int period;
    private boolean output;

    public boolean output() {
        var temp = this.output;
        if (temp) {
            this.output = false;
        }
        return temp;
    }

    @Override
    public void tick() {
        this.counter--;
        //重置计数器并输出一个时钟
        if (this.counter == 0) {
            this.reset();
            this.output = true;
        }
    }

    public void reset() {
        this.output = false;
        this.counter = this.period;
    }
}
