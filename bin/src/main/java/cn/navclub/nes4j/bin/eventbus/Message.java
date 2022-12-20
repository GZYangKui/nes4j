package cn.navclub.nes4j.bin.eventbus;


public interface Message<T> {
    /**
     * The body of the message.Can be null
     *
     * @return the body or null
     */
    T body();
}
