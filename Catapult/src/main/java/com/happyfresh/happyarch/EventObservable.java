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
import io.reactivex.disposables.Disposable;
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

    public static <T> void bindSubscriber(T target, EventObservable eventObservable) {
        Map<Class, List<Method>> methodMaps = new HashMap<>();
        Map<Class, List<Method>> singleMethodMaps = new HashMap<>();
        for (Method method : target.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Subscribe.class) || method.getParameterTypes().length == 0 || method
                    .getParameterTypes().length > 2) {
                continue;
            }

            Subscribe subscribe = method.getAnnotation(Subscribe.class);

            if (subscribe.single()) {
                putMethodIntoMaps(singleMethodMaps, subscribe, method);
            }
            else {
                putMethodIntoMaps(methodMaps, subscribe, method);
            }
        }

        for (Map.Entry<Class, List<Method>> entry : methodMaps.entrySet()) {
            eventObservable.subscribe(entry.getKey(), event -> {
                invokeMethod(entry, event, target);
            });
        }

        if (!singleMethodMaps.isEmpty()) {
            Map<Class, Disposable> disposableMaps = new HashMap<>();
            for (Map.Entry<Class, List<Method>> entry : singleMethodMaps.entrySet()) {
                disposableMaps.put(entry.getKey(), eventObservable.subscribe(entry.getKey(), event -> {
                    List<Method> methodInvokes = invokeMethod(entry, event, target);
                    for (Method method : methodInvokes) {
                        entry.getValue().remove(method);
                    }
                    if (entry.getValue().isEmpty()) {
                        Disposable disposable = disposableMaps.get(entry.getKey());
                        if (disposable != null) {
                            disposable.dispose();
                        }
                        disposableMaps.remove(entry.getKey());
                    }
                }));
            }
        }
    }

    private static void putMethodIntoMaps(Map<Class, List<Method>> methodMaps, Subscribe subscribe, Method method) {
        for (int i = 0; i < subscribe.value().length; i++) {
            List<Method> methods = methodMaps.get(subscribe.value()[i]);
            if (methods == null) {
                methods = new ArrayList<>();
            }

            methods.add(method);

            methodMaps.put(subscribe.value()[i], methods);
        }
    }

    @SuppressWarnings("unchecked")
    @NonNull
    private static <T> List<Method> invokeMethod(Map.Entry<Class, List<Method>> entry, Event event, T target) {
        List<Method> methodInvokes = new ArrayList<>();
        for (Method method : entry.getValue()) {
            int parameterLength = method.getParameterTypes().length;
            Class<? extends Event> eventClass = (Class<? extends Event>) method
                    .getParameterTypes()[parameterLength - 1];

            if (!eventClass.isInstance(event)) {
                continue;
            }

            try {
                if (parameterLength == 1) {
                    Object result = method.invoke(target, event);
                    if (result instanceof Boolean) {
                        if ((Boolean) result) {
                            methodInvokes.add(method);
                        }
                    }
                    else {
                        methodInvokes.add(method);
                    }
                }
                else if (parameterLength == 2) {
                    Object result = method.invoke(target, entry.getKey(), event);
                    if (result instanceof Boolean) {
                        if ((Boolean) result) {
                            methodInvokes.add(method);
                        }
                    }
                    else {
                        methodInvokes.add(method);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return methodInvokes;
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
    public <T extends Event> Disposable subscribe(@NonNull Class<?> clazz, @NonNull Consumer<T> onNext) {
        return subscribe(clazz, onNext, Throwable::printStackTrace);
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("unchecked")
    public <T extends Event> Disposable subscribe(@NonNull Class<?> clazz, @NonNull Consumer<T> onNext,
                                                  @NonNull Consumer<Throwable> onError) {
        return ((Observable<T>) getObservable(clazz)).observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNext, onError);
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
