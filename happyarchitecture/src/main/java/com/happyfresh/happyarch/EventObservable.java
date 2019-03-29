package com.happyfresh.happyarch;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

public class EventObservable implements LifecycleObserver {

    @NonNull
    protected static Map<LifecycleOwner, EventObservable> eventObservables = new HashMap<>();

    @NonNull
    protected Map<Class<?>, PublishSubject<? extends Event>> publishSubjects = new HashMap<>();

    @NonNull
    protected LifecycleOwner lifecycleOwner;

    public EventObservable(@NonNull LifecycleOwner lifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    public static EventObservable get(@NonNull LifecycleOwner lifecycleOwner) {
        EventObservable eventObserver = eventObservables.get(lifecycleOwner);
        if (eventObserver == null) {
            eventObserver = new EventObservable(lifecycleOwner);
            eventObservables.put(lifecycleOwner, eventObserver);
        }

        return eventObserver;
    }

    public static <T extends Event> void emitAll(Class<?> clazz, T event) {
        for (Map.Entry<LifecycleOwner, EventObservable> entry : eventObservables.entrySet()) {
            entry.getValue().emit(clazz, event);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void bindSubscriber(T target, EventObservable eventObserver) {
        Map<Class, List<Method>> methodMaps = new HashMap<>();
        for (Method method : target.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Subscribe.class) || method.getParameterTypes().length != 1) {
                continue;
            }

            Subscribe subscribe = method.getAnnotation(Subscribe.class);

            for (int i = 0; i < subscribe.value().length; i++) {
                List<Method> methods = methodMaps.get(subscribe.value()[i]);
                if (methods == null) {
                    methods = new ArrayList<>();
                }

                methods.add(method);

                methodMaps.put(subscribe.value()[i], methods);
            }
        }

        for (Map.Entry<Class, List<Method>> entry : methodMaps.entrySet()) {
            eventObserver.subscribe(entry.getKey(), event -> {
                for (Method method : entry.getValue()) {
                    Class<? extends Event> clazz = (Class<? extends Event>) method.getParameterTypes()[0];
                    if (!clazz.isInstance(event)) {
                        continue;
                    }

                    try {
                        method.invoke(target, event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void emit(@NonNull Class<?> clazz, @NonNull T event) {
        PublishSubject<T> publishSubject = ((PublishSubject<T>) getPublishSubject(clazz));
        try {
            publishSubject.onNext(event);
        } catch (Exception e) {
            publishSubject.onError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void subscribe(@NonNull Class<?> clazz, @NonNull Consumer<T> onNext) {
        subscribe(clazz, onNext, Throwable::printStackTrace);
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("unchecked")
    public <T extends Event> void subscribe(@NonNull Class<?> clazz, @NonNull Consumer<T> onNext,
                                            @NonNull Consumer<Throwable> onError) {
        ((Observable<T>) getObservable(clazz)).observeOn(AndroidSchedulers.mainThread()).subscribe(onNext, onError);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> Observable<T> getObservable(@NonNull Class<?> clazz) {
        return (Observable<T>) getPublishSubject(clazz).serialize();
    }

    private PublishSubject<? extends Event> getPublishSubject(@NonNull Class<?> clazz) {
        PublishSubject<? extends Event> publishSubject = publishSubjects.get(clazz);
        if (publishSubject == null) {
            publishSubject = createPublishSubject(clazz);
        }

        return publishSubject;
    }

    private <T extends Event> PublishSubject<T> createPublishSubject(@NonNull Class<?> clazz) {
        PublishSubject<T> publishSubject = PublishSubject.create();
        publishSubjects.put(clazz, publishSubject);
        return publishSubject;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void onDestroy() {
        for (Map.Entry<Class<?>, PublishSubject<? extends Event>> entry : publishSubjects.entrySet()) {
            entry.getValue().onComplete();
        }

        eventObservables.remove(lifecycleOwner);
    }
}
