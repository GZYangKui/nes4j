package cn.navclub.nes4j.bin.eventbus;

/**
 * @param <T> Message java type
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public interface Message<T> {
    /**
     * The body of the message.Can be null
     *
     * @return the body or null
     */
    T body();
}
