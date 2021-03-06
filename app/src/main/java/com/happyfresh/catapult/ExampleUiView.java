package com.happyfresh.catapult;

import android.view.View;
import android.widget.Toast;

import com.happyfresh.happyarch.EventObservable;
import com.happyfresh.happyarch.UiView;

import androidx.annotation.NonNull;

public class ExampleUiView extends UiView {

    public ExampleUiView(@NonNull View view,
                         @NonNull EventObservable eventObservable) {
        super(view, eventObservable);
    }

    public void showToast(String data) {
        Toast.makeText(getContext(), "Subscribe TestComponentPlugin Event " + data, Toast.LENGTH_LONG).show();
    }
}
