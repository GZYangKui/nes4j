package cn.navclub.nes4j.bin.logging.impl;

import cn.navclub.nes4j.bin.logging.LoggerDelegate;
import cn.navclub.nes4j.bin.logging.handler.JUConsoleHandler;

import java.util.logging.*;

/**
 * Java logger delegate
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class JULoggerDelegate implements LoggerDelegate {

    private final Logger logger;

    @SuppressWarnings("all")
    private final Handler console;

    public JULoggerDelegate(Class<?> clazz, cn.navclub.nes4j.bin.logging.Level level) {
        var level0 = switch (level) {
            case ALL -> Level.ALL;
            case TRACE -> Level.FINEST;
            case DEBUG -> Level.FINE;
            case INFO -> Level.INFO;
            case WARN -> Level.WARNING;
            case FATAL, ERROR -> Level.SEVERE;
            case OFF -> Level.OFF;
        };
        this.console = new JUConsoleHandler(level0);
        this.logger = Logger.getLogger(clazz.getName());
        this.logger.setLevel(level0);
        this.logger.addHandler(console);
        this.logger.setUseParentHandlers(false);
    }

    @Override
    public void debug(String msg, Object... params) {
        this.logger.log(Level.FINE, msg, params);
    }

    @Override
    public void info(String msg, Object... params) {
        this.logger.log(Level.INFO, msg, params);
    }

    @Override
    public void warning(String msg, Object... params) {
        this.logger.log(Level.WARNING, msg, params);
    }

    @Override
    public void fatal(String msg, Throwable throwable) {
        this.logger.log(Level.SEVERE, msg, throwable);
    }

    @Override
    public void fatal(String msg, Throwable throwable, Object... params) {
        var lr = new LogRecord(Level.SEVERE, msg);
        lr.setThrown(throwable);
        lr.setParameters(params);
        lr.setLoggerName(this.logger.getName());
        this.logger.log(lr);
    }

    @Override
    public void trace(String message, Object... params) {
        this.logger.log(Level.FINEST, message, params);
    }

    @Override
    public boolean isDebugEnabled() {
        return this.logger.getLevel() == Level.FINE;
    }

    @Override
    public boolean isTraceEnabled() {
        return this.logger.getLevel() == Level.FINEST;
    }
}
