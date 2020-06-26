package com.happyfresh.catapult;

import android.app.Application;
import android.widget.Toast;

import com.happyfresh.happyarch.EventObservable;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        EventObservable.setOnCatchExceptionListener(e -> {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        });
    }
}
