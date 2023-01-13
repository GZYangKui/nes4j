package cn.navclub.nes4j.bin.logging;

import cn.navclub.nes4j.bin.logging.impl.JULoggerDelegate;

public class LoggerFactory {
    private static final Level level;

    static {
        var str = System.getProperty("nes4j.log.level");
        if (str == null || str.trim().equals("")) {
            level = Level.WARN;
        } else {
            level = Level.valueOf(str.toUpperCase());
        }
    }

    public static LoggerDelegate logger(Class<?> clazz) {
        return new JULoggerDelegate(clazz, level);
    }
}
