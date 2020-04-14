package com.happyfresh.happyarch;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;

public class UiView {

    @NonNull
    protected View view;

    @NonNull
    protected EventObservable eventObservable;

    public UiView(@NonNull View view, @NonNull EventObservable eventObservable) {
        this.view = view;
        this.eventObservable = eventObservable;
    }

    public Context getContext() {
        return view.getContext();
    }
}
