package cn.navclub.nes4j.bin.eventbus.impl;

import cn.navclub.nes4j.bin.eventbus.Message;
import cn.navclub.nes4j.bin.eventbus.MessageConsumer;

import java.util.function.Function;

public class MessageConsumerImpl<T> implements MessageConsumer<T> {
    private final Function<Message<T>, Object> handler;

    public MessageConsumerImpl(Function<Message<T>, Object> handler) {
        this.handler = handler;
    }

    public Object accept(T body) {
        return this.handler.apply(new MessageImpl<>(body));
    }
}
