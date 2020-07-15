package com.happyfresh.happyarch;

import androidx.lifecycle.LifecycleOwner;

public class ComponentPlugin<T> {

    protected T component;

    @SuppressWarnings("unchecked")
    public static <T, R extends ComponentPlugin<T>> R apply(Class<R> pluginClass, T component,
                                                            LifecycleOwner lifecycleOwner) {
        try {
            R plugin = pluginClass.newInstance();
            plugin.setComponent(component);
            plugin.apply(lifecycleOwner);
            return plugin;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    public void setComponent(T component) {
        this.component = component;
    }

    public void apply(LifecycleOwner lifecycleOwner) {
        EventObservable.bindSubscriber(this, EventObservable.get(lifecycleOwner));
    }
}
