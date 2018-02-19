package com.binance.util;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.annotations.Nullable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public class InMemory<T> {

    private Subject<T> behaviorSubject = BehaviorSubject.<T>create().toSerialized();
    private T variable;

    public InMemory() {
    }

    public InMemory(T variable) {
        set(variable);
    }

    public void set(T variable) {
        this.variable = variable;
        behaviorSubject.onNext(variable);
    }

    public Flowable<T> asObservable() {
        return behaviorSubject.toFlowable(BackpressureStrategy.LATEST);
    }

    @Nullable
    public T get() {
        return variable;
    }

    public void onError(Throwable throwable) {
        behaviorSubject.onError(throwable);
    }

    public boolean hasData() {
        return get() != null;
    }
}