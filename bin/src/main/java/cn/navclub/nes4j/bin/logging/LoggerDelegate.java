package cn.navclub.nes4j.bin.logging;

/**
 * Nes4j log delegate
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public interface LoggerDelegate {
    /**
     * Output a trace level message
     *
     * @param message message content
     * @param params  message params
     */
    void trace(String message, Object... params);

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
     * Output a fatal level message with params
     *
     * @param msg       Message content
     * @param throwable exception detail
     * @param params    Params list
     */
    void fatal(String msg, Throwable throwable, Object... params);

    /**
     * Whether debug is enable
     *
     * @return If debug is enable return {@code true} otherwise {@code false}
     */
    boolean isDebugEnabled();

    /**
     * Whether trace is enable
     *
     * @return If trace is enable return {@code true} otherwise {@code false}
     */
    boolean isTraceEnabled();
}
