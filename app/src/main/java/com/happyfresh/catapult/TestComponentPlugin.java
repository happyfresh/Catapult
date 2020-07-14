package com.happyfresh.catapult;

import com.happyfresh.happyarch.ComponentPlugin;
import com.happyfresh.happyarch.Event;
import com.happyfresh.happyarch.Subscribe;

import androidx.lifecycle.LifecycleOwner;

public class TestComponentPlugin extends ComponentPlugin<TestComponent> {

    public TestComponentPlugin(LifecycleOwner lifecycleOwner) {
        super(lifecycleOwner);
    }

    @Subscribe(value = Event.class, single = true)
    public void subscribe(TestEvent event) {
        component.onTestComponentLoaded(event);
    }
}
