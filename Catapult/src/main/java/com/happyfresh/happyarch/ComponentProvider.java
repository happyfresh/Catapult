package com.happyfresh.happyarch;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ComponentProvider implements LifecycleObserver {

    private LifecycleOwner lifecycleOwner;

    private Map<Class, ? super Component> componentMap = new HashMap<>();

    public ComponentProvider(LifecycleOwner lifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner;
        lifecycleOwner.getLifecycle().addObserver(this);
    }

    public <T extends Component> ComponentProvider add(Class<T> componentClass, View view) {
        get(componentClass, view);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> componentClass, View view) {
        T component = (T) componentMap.get(componentClass);
        if (component != null) {
            return component;
        }

        try {
            Constructor constructor = componentClass.getConstructor(View.class, LifecycleOwner.class);
            component = (T) constructor.newInstance(view, lifecycleOwner);
            componentMap.put(componentClass, component);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return component;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void onDestroy() {
        componentMap.clear();
    }
}
