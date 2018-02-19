package com.binance.websocket;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.domain.event.DepthEvent;
import com.binance.api.client.impl.BinanceApiWebSocketListener;

import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MyBinanceApiWebSocketClient extends com.binance.api.client.impl.BinanceApiWebSocketClientImpl {

    private OkHttpClient client;

    public MyBinanceApiWebSocketClient() {
        Dispatcher d = new Dispatcher();
        d.setMaxRequestsPerHost(100);
        this.client = (new OkHttpClient.Builder()).dispatcher(d).build();
    }

    public Flowable<List<Map<String, String>>> listenForAllMarketTickersEventMap() {
        final FlowableBinanceApiWebSocketListener<List<Map<String, String>>> listener =
                new FlowableBinanceApiWebSocketListener<>(true);
        this.createNewWebSocket(
                "!ticker@arr",
                listener);
        return listener.toFlowable();
    }

    public Flowable<DepthEvent> listenForDepthEvents(String symbol) {
        final String channel = String.format("%s@depth", new Object[]{symbol});
        final FlowableBinanceApiWebSocketListener<DepthEvent> listener =
                new FlowableBinanceApiWebSocketListener<>(DepthEvent.class);
        this.createNewWebSocket(channel, listener);
        return listener.toFlowable();
    }

    private void createNewWebSocket(String channel, FlowableBinanceApiWebSocketListener<?> listener) {
        final String streamingUrl = String.format("%s/%s", new Object[]{"wss://stream.binance.com:9443/ws", channel});
        final Request request = (new okhttp3.Request.Builder()).url(streamingUrl).build();
        this.client.newWebSocket(request, listener);
    }

}