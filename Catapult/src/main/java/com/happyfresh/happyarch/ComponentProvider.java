package com.happyfresh.happyarch;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentProvider implements LifecycleObserver {

    private LifecycleOwner lifecycleOwner;

    private Map<View, ? super Component> componentMap = new HashMap<>();

    private Map<Component, List<ComponentPlugin<?>>> pluginsMap = new HashMap<>();

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
        T component = (T) componentMap.get(view);
        if (component != null) {
            return component;
        }

        try {
            Constructor<T> constructor = componentClass.getConstructor(View.class, LifecycleOwner.class);
            component = (T) constructor.newInstance(view, lifecycleOwner);

            componentMap.put(view, component);

            if (componentClass.isAnnotationPresent(Plugin.class)) {
                List<ComponentPlugin<?>> plugins = pluginsMap.get(component);
                if (plugins == null) {
                    plugins = new ArrayList<>();
                    pluginsMap.put(component, plugins);
                }

                Plugin plugin = componentClass.getAnnotation(Plugin.class);
                for (Class pluginClass : plugin.value()) {
                    plugins.add(ComponentPlugin.apply(pluginClass, component, lifecycleOwner));
                }
            }
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
        for (Map.Entry<Component, List<ComponentPlugin<?>>> entry : pluginsMap.entrySet()) {
            for (ComponentPlugin<?>  plugin : entry.getValue()) {
                plugin.setComponent(null);
            }
            entry.getValue().clear();
        }
        pluginsMap.clear();
        ComponentProviders.sComponentProviderMap.remove(lifecycleOwner);
        lifecycleOwner = null;
    }
}
