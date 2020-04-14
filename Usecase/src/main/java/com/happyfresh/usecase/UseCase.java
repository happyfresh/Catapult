package com.happyfresh.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static io.reactivex.internal.functions.Functions.emptyConsumer;

public abstract class UseCase<T> {

    public static <R> UseCase<R> just(R item) {
        return new UseCase<R>() {
            @NonNull
            @Override
            public Observable<R> observe() {
                Scheduler scheduler = Schedulers.newThread();
                return Observable.just(item).subscribeOn(scheduler).observeOn(scheduler);
            }
        };
    }

    @NonNull
    public abstract Observable<T> observe();

    @NonNull
    public Observable<T> observeCreate(@NonNull ObserveCallback<T> callback) {
        Scheduler scheduler = Schedulers.newThread();
        return observeCreate(callback, scheduler);
    }

    @NonNull
    public Observable<T> observeCreate(@NonNull ObserveCallback<T> callback, Scheduler scheduler) {
        Observable<T> observable = Observable.create(emitter -> {
            try {
                callback.subscribe(emitter);
            } catch (Exception e) {
                saveOnError(emitter, e);
            }
        });
        return observable.observeOn(scheduler).subscribeOn(scheduler);
    }

    @NonNull
    public Disposable subscribe() {
        return observe().subscribe(emptyConsumer(), emptyConsumer());
    }

    @NonNull
    public Disposable subscribe(@NonNull Consumer<T> onNext) {
        return observe().subscribe(onNext, emptyConsumer());
    }

    @NonNull
    public Disposable subscribe(@NonNull Consumer<T> onNext, @NonNull Consumer<? super Throwable> onError) {
        return observe().subscribe(onNext, onError);
    }

    @NonNull
    public void subscribe(@NonNull Observer<T> observer) {
        observe().subscribe(observer);
    }

    public boolean saveOnNext(ObservableEmitter<? super T> emitter, T value) {
        if (emitter.isDisposed()) {
            return false;
        }

        emitter.onNext(value);
        return true;
    }

    public void saveOnError(ObservableEmitter<? super T> emitter, Throwable error) {
        if (emitter.isDisposed()) {
            return;
        }

        emitter.onError(error);
    }

    public <R> UseCase<R> map(Function<T, R> mapper) {
        return new UseCase<R>() {
            @NonNull
            @Override
            public Observable<R> observe() {
                return UseCase.this.observe().map(mapper);
            }
        };
    }

    public <R> UseCase<R> concatMap(Function<T, UseCase<R>> mapper) {
        return new UseCase<R>() {
            @NonNull
            @Override
            public Observable<R> observe() {
                return UseCase.this.observe().concatMap(t -> mapper.apply(t).observe());
            }
        };
    }

    public <R> UseCase<R> concatMapOptional(FunctionOptional<T, UseCase<R>> mapper) {
        return new UseCase<R>() {

            @Nullable
            private T item = null;

            @NonNull
            @Override
            public Observable<R> observe() {
                return observeCreate(emitter ->
                                             UseCase.this.doFinally(() ->
                                                                            mapper.apply(item).subscribe(new Observer<R>() {
                                                                                @Override
                                                                                public void onSubscribe(Disposable d) {
                                                                                    // Do nothing
                                                                                }

                                                                                @Override
                                                                                public void onNext(R r) {
                                                                                    saveOnNext(emitter, r);
                                                                                }

                                                                                @Override
                                                                                public void onError(Throwable e) {
                                                                                    saveOnError(emitter, e);
                                                                                }

                                                                                @Override
                                                                                public void onComplete() {
                                                                                    if (!emitter.isDisposed()) {
                                                                                        emitter.onComplete();
                                                                                    }
                                                                                }
                                                                            })).subscribe(onNext -> item = onNext));
            }
        };
    }

    public UseCase<T> doFinally(Action action) {
        return new UseCase<T>() {
            @NonNull
            @Override
            public Observable<T> observe() {
                return UseCase.this.observe().doFinally(action);
            }
        };
    }

    public interface ObserveCallback<T> {

        void subscribe(@NonNull ObservableEmitter<? super T> emitter);
    }

    /**
     * A functional interface that takes a value and returns another value, possibly with a
     * different type and allows throwing a checked exception.
     *
     * @param <T> the input value type, if on error the input will be null
     * @param <R> the output value type
     */
    public interface FunctionOptional<T, R> {

        /**
         * Apply some calculation to the input value and return some other value.
         *
         * @param t the input value
         * @return the output value
         * @throws Exception on error
         */
        R apply(@Nullable T t) throws Exception;
    }
}
