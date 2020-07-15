package com.happyfresh.catapult;

import com.happyfresh.happyarch.Event;

public class PluginEvent<T> implements Event {

    private T data;

    public PluginEvent(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}
