package cn.navclub.nes4j.bin.eventbus.impl;

import cn.navclub.nes4j.bin.eventbus.Message;


public class MessageImpl<T> implements Message<T> {
    private final T body;

    public MessageImpl(T body) {
        this.body = body;
    }

    @Override
    public T body() {
        return this.body;
    }
}
