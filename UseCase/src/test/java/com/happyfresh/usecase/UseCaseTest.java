package com.happyfresh.usecase;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UseCaseTest extends BaseTest {

    private UseCase usecase;

    private TestObserver testObserver;

    @After
    public void after() {
        if (testObserver != null) {
            testObserver.dispose();
        }
    }

    @Test
    public void saveOnError_hasDispose_test() {
        usecase = new UseCase() {
            @NonNull
            @Override
            public Observable observe() {
                return observeCreate(emitter -> {
                    saveOnError(emitter, new Throwable());
                });
            }
        };

        testObserver = usecase.observe().test(true);

        testObserver.assertNoErrors();
    }

    @Test
    public void map_test() {
        usecase = new UseCase() {
            @NonNull
            @Override
            public Observable observe() {
                return Observable.just(new Object());
            }
        };

        Object object = new Object();
        usecase = usecase.map(o -> object);

        testObserver = usecase.observe().test();

        testObserver.assertValue(object);
    }

    @Test
    public void concatMap_test() {
        usecase = new UseCase() {
            @NonNull
            @Override
            public Observable observe() {
                return Observable.just(new Object());
            }
        };

        Object object = new Object();
        usecase = usecase.concatMap((Function) o -> new UseCase() {
            @NonNull
            @Override
            public Observable observe() {
                return Observable.just(object);
            }
        });

        testObserver = usecase.observe().test();

        testObserver.assertValue(object);
    }

    @Test
    public void doFinally_test() throws Exception {
        usecase = new UseCase() {
            @NonNull
            @Override
            public Observable observe() {
                return Observable.just(new Object());
            }
        };

        Action action = mock(Action.class);
        usecase = usecase.doFinally(action);

        testObserver = usecase.observe().test();

        verify(action).run();
    }

    @Test
    public void concatMapOptional_success_test() {
        usecase = new UseCase() {
            @NonNull
            @Override
            public Observable observe() {
                return Observable.just(new Object());
            }
        };

        Object object = new Object();
        usecase = usecase.concatMapOptional(o -> UseCase.just(object));

        testObserver = usecase.observe().test();

        testObserver.assertValue(object);
    }

    @Test
    public void concatMapOptional_error_test() {
        usecase = new UseCase() {
            @NonNull
            @Override
            public Observable observe() {
                return Observable.error(new Throwable());
            }
        };

        Object object = new Object();
        usecase = usecase.concatMapOptional(o -> UseCase.just(object));

        testObserver = usecase.observe().test();

        testObserver.assertValue(object);
    }
}
