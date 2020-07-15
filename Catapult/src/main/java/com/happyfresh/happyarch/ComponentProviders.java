package com.happyfresh.happyarch;

import androidx.lifecycle.LifecycleOwner;

import java.util.HashMap;
import java.util.Map;

public class ComponentProviders {

    static Map<LifecycleOwner, ComponentProvider> sComponentProviderMap = new HashMap<>();

    public static ComponentProvider of(LifecycleOwner lifecycleOwner) {
        ComponentProvider componentProvider = sComponentProviderMap.get(lifecycleOwner);
        if (componentProvider == null) {
            componentProvider = new ComponentProvider(lifecycleOwner);
            sComponentProviderMap.put(lifecycleOwner, componentProvider);
        }

        return componentProvider;
    }
}
