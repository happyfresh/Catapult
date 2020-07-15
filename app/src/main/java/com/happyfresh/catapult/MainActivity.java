package com.happyfresh.catapult;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.happyfresh.happyarch.ComponentProviders;
import com.happyfresh.happyarch.Event;
import com.happyfresh.happyarch.EventObservable;
import com.happyfresh.happyarch.Subscribe;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EventObservable.bindSubscriber(this, EventObservable.get(this));

        findViewById(R.id.btnEmit).setOnClickListener(v -> {
            EventObservable.emitAll(Event.class, new TestEvent());
        });

        findViewById(R.id.btnNextScreen).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        ComponentProviders.of(this).get(ExampleComponent.class, findViewById(R.id.component_example));
    }

    @Subscribe(Event.class)
    public void subscribeEventForTestError(TestEvent testEvent) {
        throw new RuntimeException("Test Runtime Exception");
    }

    @Subscribe(Event.class)
    public void subscribeEvent(TestEvent testEvent) {
        Toast.makeText(this, "Subscribe Event Normal " + toString(), Toast.LENGTH_LONG).show();
    }

    @Subscribe(value = Event.class, single = true)
    public void subscribeEventSingle(TestEvent testEvent) {
        Toast.makeText(this, "Subscribe Event Single " + toString(), Toast.LENGTH_LONG).show();
    }

    @Subscribe(value = Event.class, keepAlive = true)
    public void subscribeEventKeepAlive(TestEvent testEvent) {
        Toast.makeText(this, "Subscribe Event Keep Alive " + toString(), Toast.LENGTH_LONG).show();
    }
}
