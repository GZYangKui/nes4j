package cn.navclub.nes4j.app.event;

import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Nes instance fps tracer
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class FPSTracer {
    private static final LoggerDelegate log = LoggerFactory.logger(FPSTracer.class);

    private final Timer timer;
    private final AtomicInteger frame;
    private final Consumer<Integer> consumer;

    public FPSTracer(Consumer<Integer> consumer) {
        this.consumer = consumer;
        this.timer = new Timer();
        this.frame = new AtomicInteger(0);
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                FPSTracer.this.consumer.accept(FPSTracer.this.frame.getAndSet(0));
            }
        }, 0, 1000);
    }

    public void increment() {
        this.frame.incrementAndGet();
    }

    public void stop() {
        this.frame.set(0);
        this.timer.cancel();
        if (log.isDebugEnabled()) {
            log.debug("FPS Tracer was closed.");
        }
    }
}
