package cn.navclub.nes4j.app.event;

import javafx.animation.AnimationTimer;

import java.util.function.Consumer;

public class FPSTracer extends AnimationTimer {
    private long now;

    private int frame;
    private final Consumer<Integer> consumer;

    public FPSTracer(Consumer<Integer> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void handle(long t) {
        if (now == 0 || (t - now) >= 1000_000_000L) {
            now = t;
            this.consumer.accept(this.frame);
            this.frame = 0;
        }
    }

    public void increment() {
        this.frame++;
    }
}
