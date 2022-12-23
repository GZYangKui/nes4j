package cn.navclub.nes4j.bin.eventbus;


import cn.navclub.nes4j.bin.eventbus.impl.MessageConsumerImpl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class EventBus {
    private final Map<String, MessageConsumer<?>> map;

    public EventBus() {
        this.map = new ConcurrentHashMap<>();
    }

    /**
     * listener address to event-bus
     *
     * @param address Wait consumer address
     * @param handler When message arrive callback handler
     * @param <T>     Accept message type
     * @return {@link MessageConsumer}
     */
    public <T> MessageConsumer<T> listener(String address, Function<Message<T>, Object> handler) {
        if (this.map.containsKey(address)) {
            throw new RuntimeException("Repeat consumer target address:" + address);
        }
        var consumer = new MessageConsumerImpl<>(handler);
        this.map.put(address, consumer);
        return consumer;
    }

    /**
     * Send message to event-bus
     *
     * @param address Target address
     * @param body    The message of body
     * @param <T>     The message of type
     * @return Return reply message or null
     */
    @SuppressWarnings("all")
    public <T, R> R publish(String address, T body) {
        var consumer = (MessageConsumerImpl<T>) (this.map.get(address));
        if (consumer == null) {
            throw new RuntimeException(address + " address already register?");
        }
        return (R) (consumer.accept(body));
    }

    /**
     * Remove target address listener
     *
     * @param address Wait remove listener address
     */
    public void removeListener(String address) {
        if (!this.map.containsKey(address)) {
            return;
        }
        this.map.remove(address);
    }
}
