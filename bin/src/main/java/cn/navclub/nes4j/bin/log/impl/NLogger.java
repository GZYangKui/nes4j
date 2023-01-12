package cn.navclub.nes4j.bin.log.impl;

import cn.navclub.nes4j.bin.log.formatter.NFormatter;

import java.util.logging.*;

public class NLogger implements cn.navclub.nes4j.bin.log.Logger {

    private final Logger logger;
    @SuppressWarnings("all")
    private final Handler handler;

    public NLogger(Class<?> clazz, cn.navclub.nes4j.bin.log.Level level) {
        this.handler = new ConsoleHandler();
        this.handler.setFormatter(new NFormatter());
        this.logger = Logger.getLogger(clazz.getName());
        this.logger.addHandler(handler);
        this.logger.setUseParentHandlers(false);
        switch (level) {
            case ALL -> this.logger.setLevel(Level.ALL);
            case TRACE -> this.logger.setLevel(Level.FINEST);
            case DEBUG -> this.logger.setLevel(Level.FINER);
            case INFO -> this.logger.setLevel(Level.INFO);
            case WARN -> this.logger.setLevel(Level.WARNING);
            case FATAL -> this.logger.setLevel(Level.SEVERE);
            case OFF -> this.logger.setLevel(Level.OFF);
        }
    }

    @Override
    public void debug(String msg, Object... params) {
        this.logger.log(Level.FINER, msg, params);
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
    public boolean isDebugEnabled() {
        var level = this.logger.getLevel();
        return level == Level.ALL || level.intValue() <= Level.CONFIG.intValue();
    }
}
