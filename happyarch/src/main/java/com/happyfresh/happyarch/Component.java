package com.happyfresh.happyarch;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

public abstract class Component<T extends UiView> implements LifecycleObserver {

    @NonNull
    protected LifecycleOwner lifecycleOwner;

    @NonNull
    private View view;

    @Nullable
    private EventObservable eventObservable;

    @Nullable
    private T uiView;

    public Component(@NonNull View view, @NonNull LifecycleOwner lifecycleOwner) {
        this.view = view;
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
        EventObservable.bindSubscriber(this, getEventObservable());
    }

    @NonNull
    public T getUiView() {
        if (uiView == null) {
            uiView = onCreateView(view, getEventObservable());
        }

        return uiView;
    }

    @NonNull
    public EventObservable getEventObservable() {
        if (eventObservable == null) {
            eventObservable = EventObservable.get(lifecycleOwner);
        }

        return eventObservable;
    }

    @NonNull
    public abstract T onCreateView(View view, EventObservable eventObservable);

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {

    }
}
