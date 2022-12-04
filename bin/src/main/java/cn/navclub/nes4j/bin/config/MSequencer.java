package cn.navclub.nes4j.bin.config;

import lombok.Getter;

@Getter
public enum MSequencer {
    FOUR_STEP_SEQ(new int[]{7457, 7456, 7458, 7458}),
    FIVE_STEP_SEQ(new int[]{7457, 7456, 7458, 14910});

    //将APU时钟换算为CPU时钟
    private final int[] steps;

    MSequencer(int[] steps) {
        this.steps = steps;
    }
}
