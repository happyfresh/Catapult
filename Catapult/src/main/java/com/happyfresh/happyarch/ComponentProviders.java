package com.happyfresh.happyarch;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.HashMap;
import java.util.Map;

public class ComponentProviders {

    private static Map<LifecycleOwner, ComponentProvider> sComponentProviderMap = new HashMap<>();

    public static ComponentProvider of(LifecycleOwner lifecycleOwner) {
        ComponentProvider componentProvider = sComponentProviderMap.get(lifecycleOwner);
        if (componentProvider == null) {
            componentProvider = new ComponentProvider(lifecycleOwner);
            sComponentProviderMap.put(lifecycleOwner, componentProvider);

            lifecycleOwner.getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                private void onDestroy() {
                    lifecycleOwner.getLifecycle().removeObserver(this);
                    sComponentProviderMap.remove(lifecycleOwner);
                }
            });
        }

        return componentProvider;
    }
}
