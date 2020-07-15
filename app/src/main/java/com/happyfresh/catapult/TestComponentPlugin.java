package com.happyfresh.catapult;

import com.happyfresh.happyarch.ComponentPlugin;
import com.happyfresh.happyarch.Subscribe;

public class TestComponentPlugin<T> extends ComponentPlugin<TestComponent<T>> {

    @Subscribe(value = TestComponent.class, single = true)
    public void subscribe(PluginEvent<T> event) {
        component.onTestComponentLoaded(event.getData());
    }
}
