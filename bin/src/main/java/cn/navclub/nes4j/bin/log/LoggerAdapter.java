package cn.navclub.nes4j.bin.log;

import cn.navclub.nes4j.bin.log.impl.NLogger;

public class LoggerAdapter {
    private static final Level level;

    static {
        var str = System.getProperty("nes4j.log.level");
        if (str == null || str.trim().equals("")) {
            level = Level.INFO;
        } else {
            level = Level.valueOf(str.toUpperCase());
        }
    }

    public static Logger logger(Class<?> clazz) {
        return new NLogger(clazz, level);
    }
}
