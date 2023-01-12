package cn.navclub.nes4j.bin.log;

public interface Logger {
    /**
     * Output a debug level message
     *
     * @param msg    message content
     * @param params message params
     */
    void debug(String msg, Object... params);

    /**
     * Output a info level message
     *
     * @param msg    message content
     * @param params message params
     */
    void info(String msg, Object... params);

    /**
     * Output a warning level message
     *
     * @param msg    message content
     * @param params message params
     */
    void warning(String msg, Object... params);

    /**
     * Output a fatal level message
     *
     * @param msg       message content
     * @param throwable exception detail
     */
    void fatal(String msg, Throwable throwable);

    /**
     * Whether debug is enable
     *
     * @return If debug is enable return {@code true} otherwise {@code false}
     */
    boolean isDebugEnabled();
}
