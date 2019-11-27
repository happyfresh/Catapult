package com.happyfresh.happyarch;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class EventObservable implements LifecycleObserver {

    private static final IgnoreEvent IGNORE_EVENT = new IgnoreEvent();

    @NonNull
    protected static Map<LifecycleOwner, EventObservable> eventObservables = new HashMap<>();

    @Nullable
    protected static EventObservable lastEventObservable;

    @NonNull
    protected Map<Class<?>, Subject<? extends Event>> subjects = new HashMap<>();

    @NonNull
    private LifecycleOwner lifecycleOwner;

    private Map<Object, CompositeDisposable> compositeDisposableMap = new HashMap<>();

    private boolean rebindSubscriberOnResume = false;

    public EventObservable(@NonNull LifecycleOwner lifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner;
        this.lifecycleOwner.getLifecycle().addObserver(this);
    }

    public static EventObservable get(@NonNull LifecycleOwner lifecycleOwner) {
        EventObservable eventObserver = eventObservables.get(lifecycleOwner);
        if (eventObserver == null) {
            eventObserver = new EventObservable(lifecycleOwner);
            eventObservables.put(lifecycleOwner, eventObserver);
            lastEventObservable = eventObserver;
        }

        return eventObserver;
    }

    public static <T extends Event> void emitAll(Class<?> clazz, T event) {
        for (Map.Entry<LifecycleOwner, EventObservable> entry : eventObservables.entrySet()) {
            entry.getValue().emit(clazz, event);
        }
    }

    public static <T extends Event> void emitLast(Class<?> clazz, T event) {
        if (lastEventObservable != null) {
            lastEventObservable.emit(clazz, event);
        }
    }

    public static <T> void bindSubscriber(T target, EventObservable eventObservable) {
        bindSubscriber(target, eventObservable, false);
    }

    public static <T> void bindSubscriber(T target, EventObservable eventObservable, boolean subscribeNormalOnly) {
        Map<Class, List<Method>> methodMaps = new HashMap<>();
        Map<Class, List<Method>> singleMethodMaps = new HashMap<>();
        Map<Class, List<Method>> keepAliveMethodMaps = new HashMap<>();
        for (Method method : target.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Subscribe.class) || method.getParameterTypes().length == 0 || method
                    .getParameterTypes().length > 2) {
                continue;
            }

            Subscribe subscribe = method.getAnnotation(Subscribe.class);

            if (subscribe.single()) {
                putMethodIntoMaps(singleMethodMaps, subscribe, method);
            }
            else if (subscribe.keepAlive()) {
                putMethodIntoMaps(keepAliveMethodMaps, subscribe, method);
            }
            else {
                putMethodIntoMaps(methodMaps, subscribe, method);
            }
        }

        for (Map.Entry<Class, List<Method>> entry : methodMaps.entrySet()) {
            eventObservable.addDisposable(target, eventObservable.subscribe(entry.getKey(), event -> {
                invokeMethod(entry, event, target);
            }));
        }

        if (!keepAliveMethodMaps.isEmpty() && !subscribeNormalOnly) {
            for (Map.Entry<Class, List<Method>> entry : keepAliveMethodMaps.entrySet()) {
                eventObservable.subscribe(entry.getKey(), event -> {
                    invokeMethod(entry, event, target);
                });
            }
        }

        if (!singleMethodMaps.isEmpty() && !subscribeNormalOnly) {
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

            if (!eventClass.equals(event.getClass())) {
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
        Subject<T> subject = ((Subject<T>) getSubject(clazz));
        try {
            subject.onNext(event);
        } catch (Exception e) {
            subject.onError(e);
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
        return ((Observable<T>) getObservable(clazz))
                .filter(event -> !event.equals(IGNORE_EVENT))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onNext, onError);
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> Observable<T> getObservable(@NonNull Class<?> clazz) {
        return (Observable<T>) getSubject(clazz).serialize();
    }

    @NonNull
    public LifecycleOwner getLifecycleOwner() {
        return lifecycleOwner;
    }

    private Subject<? extends Event> getSubject(@NonNull Class<?> clazz) {
        Subject<? extends Event> subject = subjects.get(clazz);
        if (subject == null) {
            subject = createSubject(clazz);
        }

        return subject;
    }

    private <T extends Event> Subject<T> createSubject(@NonNull Class<?> clazz) {
        Subject<T> subject = BehaviorSubject.create();
        subjects.put(clazz, subject);
        return subject;
    }

    private Disposable addDisposable(Object target, Disposable disposable) {
        CompositeDisposable compositeDisposable = compositeDisposableMap.get(target);
        if (compositeDisposable == null) {
            compositeDisposable = new CompositeDisposable();
            compositeDisposableMap.put(target, compositeDisposable);
        }

        compositeDisposable.add(disposable);

        return disposable;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void onResume() {
        if (!rebindSubscriberOnResume) {
            return;
        }

        for (Map.Entry<Object, CompositeDisposable> entry : compositeDisposableMap.entrySet()) {
            bindSubscriber(entry.getKey(), this, true);
        }

        rebindSubscriberOnResume = false;
    }

    @SuppressWarnings("unchecked")
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void onPause() {
        for (Map.Entry<Object, CompositeDisposable> entry : compositeDisposableMap.entrySet()) {
            entry.getValue().dispose();
            entry.setValue(new CompositeDisposable());
        }
        for (Map.Entry<Class<?>, Subject<? extends Event>> entry : subjects.entrySet()) {
            ((Subject<Event>) entry.getValue()).onNext((IGNORE_EVENT));
        }
        rebindSubscriberOnResume = true;
    }

    @SuppressWarnings("unchecked")
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onStop() {
        for (Map.Entry<Object, CompositeDisposable> entry : compositeDisposableMap.entrySet()) {
            entry.getValue().dispose();
            entry.setValue(new CompositeDisposable());
        }
        for (Map.Entry<Class<?>, Subject<? extends Event>> entry : subjects.entrySet()) {
            ((Subject<Event>) entry.getValue()).onNext((IGNORE_EVENT));
        }
        rebindSubscriberOnResume = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void onDestroy() {
        for (Map.Entry<Class<?>, Subject<? extends Event>> entry : subjects.entrySet()) {
            entry.getValue().onComplete();
        }

        eventObservables.remove(lifecycleOwner);
    }
}
