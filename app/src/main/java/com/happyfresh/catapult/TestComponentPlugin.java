package com.happyfresh.catapult;

import com.happyfresh.happyarch.ComponentPlugin;
import com.happyfresh.happyarch.Event;
import com.happyfresh.happyarch.Subscribe;

public class TestComponentPlugin extends ComponentPlugin<TestComponent> {

    @Subscribe(value = Event.class, single = true)
    public void subscribe(TestEvent event) {
        component.onTestComponentLoaded(event);
    }
}
