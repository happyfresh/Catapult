package com.happyfresh.catapult;

import android.view.View;

import com.happyfresh.happyarch.Component;
import com.happyfresh.happyarch.EventObservable;
import com.happyfresh.happyarch.Plugin;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

@Plugin(TestComponentPlugin.class)
public class ExampleComponent extends Component<ExampleUiView> implements TestComponent<String> {

    public ExampleComponent(@NonNull View view, @NonNull LifecycleOwner lifecycleOwner) {
        super(view, lifecycleOwner);
    }

    @NonNull
    @Override
    public ExampleUiView onCreateView(View view, EventObservable eventObservable) {
        return new ExampleUiView(view, eventObservable);
    }

    @Override
    public void onTestComponentLoaded(String data) {
        getUiView().showToast(data);
    }
}
