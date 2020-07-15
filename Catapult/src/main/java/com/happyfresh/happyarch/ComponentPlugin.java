package com.happyfresh.happyarch;

import androidx.lifecycle.LifecycleOwner;

public class ComponentPlugin<T> {

    protected T component;

    public static <T> ComponentPlugin apply(Class<? extends ComponentPlugin<?>> pluginClass, T component,
                                            LifecycleOwner lifecycleOwner) {
        try {
            ComponentPlugin<T> plugin = (ComponentPlugin<T>) pluginClass.newInstance();
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
