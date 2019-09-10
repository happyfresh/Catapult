package com.happyfresh.happyarch;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

public class DisposableUtils {

    public static void dispose(@Nullable Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public static void disposeAll(@NonNull Object target) {
        for (Field field : getDisposableFields(target)) {
            try {
                field.setAccessible(true);
                dispose((Disposable) field.get(target));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static List<Field> getDisposableFields(Object target) {
        List<Field> disposableFields = new ArrayList<>();
        Class clazz = target.getClass();
        do {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (!field.getType().isAssignableFrom(Disposable.class)) {
                    continue;
                }

                disposableFields.add(field);
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);

        return disposableFields;
    }
}
