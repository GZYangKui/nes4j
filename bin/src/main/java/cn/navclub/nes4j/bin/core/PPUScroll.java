package cn.navclub.nes4j.bin.core;

import lombok.Getter;

@Getter
public class PPUScroll {
    private int x;
    private int y;
    private boolean latch;

    public void write(int b) {
        if (!latch) {
            this.x = b;
        } else {
            this.y = b;
        }
        this.latch = !this.latch;
    }

    public void reset() {
        this.latch = false;
    }
}
