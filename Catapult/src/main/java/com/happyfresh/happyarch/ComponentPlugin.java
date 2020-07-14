package com.happyfresh.happyarch;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import androidx.lifecycle.LifecycleOwner;

public class ComponentPlugin<T> {

    protected T component;

    protected LifecycleOwner lifecycleOwner;

    public ComponentPlugin(LifecycleOwner lifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner;
    }

    public static <T> void apply(Class<? extends ComponentPlugin<?>> pluginClass, T component,
                                                               LifecycleOwner lifecycleOwner) {
        try {
            Constructor<? extends ComponentPlugin<?>> constructor = pluginClass.getConstructor(LifecycleOwner.class);
            ComponentPlugin<T> plugin = (ComponentPlugin<T>) constructor.newInstance(lifecycleOwner);
            plugin.setComponent(component);
            plugin.apply();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public void setComponent(T component) {
        this.component = component;
    }

    public void apply() {
        EventObservable.bindSubscriber(this, EventObservable.get(lifecycleOwner));
    }
}
