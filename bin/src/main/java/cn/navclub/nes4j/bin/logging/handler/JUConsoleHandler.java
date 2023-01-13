package cn.navclub.nes4j.bin.logging.handler;

import cn.navclub.nes4j.bin.logging.formatter.NFormatter;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class JUConsoleHandler extends StreamHandler {

    public JUConsoleHandler(Level level) {
        super(System.out, new NFormatter());
        this.setLevel(level);
    }

    @Override
    public synchronized void publish(LogRecord record) {
        super.publish(record);
        this.flush();
    }

    @Override
    public synchronized void close() throws SecurityException {
        flush();
    }
}
