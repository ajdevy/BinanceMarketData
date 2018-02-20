package com.binance.websocket;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class FlowableBinanceApiWebSocketListener<T> extends WebSocketListener {

    private Class<T> eventClass;
    private final Subject<T> subject = PublishSubject.<T>create().toSerialized();
    private final ObjectMapper mapper = new ObjectMapper();
    private final boolean shouldReturnAsListOfMaps;

    public FlowableBinanceApiWebSocketListener(@NonNull Class<T> eventClass) {
        this.eventClass = eventClass;
        this.shouldReturnAsListOfMaps = false;
    }

    public FlowableBinanceApiWebSocketListener(boolean shouldReturnAsListOfMaps) {
        this.shouldReturnAsListOfMaps = shouldReturnAsListOfMaps;
    }

    public void onMessage(WebSocket webSocket, String text) {

        try {
            final T event;
            if (shouldReturnAsListOfMaps) {
                event = mapper.readValue(text, new TypeReference<T>() {
                });
            } else {
                event = mapper.readValue(text, this.eventClass);
            }
            subject.onNext(event);
        } catch (Throwable t) {
            subject.onError(t);
        }
    }

    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        subject.onError(t);
    }

    public Flowable<T> toFlowable() {
        return subject.toFlowable(BackpressureStrategy.LATEST);
    }
}
