package com.happyfresh.happyarch;

import android.support.annotation.NonNull;

public class NewInstanceProvider {

    public static <T> T create(Class<T> tClass) {
        return create(tClass, new DefaultFactory<>());
    }

    public static <T> T create(Class<T> tClass, @NonNull Factory<T> factory) {
        return factory.create(tClass);
    }

    public interface Factory<T> {

        T create(Class<T> tClass);
    }

    public static class DefaultFactory<T> implements Factory<T> {

        @Override
        public T create(Class<T> tClass) {
            try {
                return tClass.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException("Cannot create an instance of " + tClass, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot create an instance of " + tClass, e);
            }
        }
    }
}
