package com.binance.websocket;

import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.event.DepthEvent;

import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class MyBinanceApiWebSocketClient extends com.binance.api.client.impl.BinanceApiWebSocketClientImpl {

    private OkHttpClient client;

    public MyBinanceApiWebSocketClient() {
        Dispatcher d = new Dispatcher();
        d.setMaxRequestsPerHost(100);
        this.client = (new OkHttpClient.Builder()).dispatcher(d).build();
    }

    public FlowableWebSocketClient<AggTradeEvent> listenForAggTradeEvent(String symbol) {
        String channel = String.format("%s@aggTrade", new Object[]{symbol.toLowerCase()});
        final FlowableBinanceApiWebSocketListener<AggTradeEvent> listener =
                new FlowableBinanceApiWebSocketListener<>(AggTradeEvent.class);
        return this.createNewFlowableWebSocketClient(channel, listener);
    }

    public FlowableWebSocketClient<List<Map<String, String>>> listenForAllMarketTickersEventMap() {
        final FlowableBinanceApiWebSocketListener<List<Map<String, String>>> listener =
                new FlowableBinanceApiWebSocketListener<>(true);
        return this.createNewFlowableWebSocketClient(
                "!ticker@arr",
                listener);
    }

    public FlowableWebSocketClient<DepthEvent> listenForDepthEvents(String symbol) {
        final String channel = String.format("%s@depth", new Object[]{symbol.toLowerCase()});
        final FlowableBinanceApiWebSocketListener<DepthEvent> listener =
                new FlowableBinanceApiWebSocketListener<>(DepthEvent.class);
        return this.createNewFlowableWebSocketClient(channel, listener);
    }

    private <T> FlowableWebSocketClient<T> createNewFlowableWebSocketClient(String channel, FlowableBinanceApiWebSocketListener<T> listener) {
        final String streamingUrl = String.format("%s/%s", new Object[]{"wss://stream.binance.com:9443/ws", channel});
        final Request request = (new okhttp3.Request.Builder()).url(streamingUrl).build();
        final WebSocket webSocket = this.client.newWebSocket(request, listener);
        return new FlowableWebSocketClient<>(webSocket, listener);
    }

    public static final class FlowableWebSocketClient<T> {
        private final WebSocket socket;
        private FlowableBinanceApiWebSocketListener<T> listener;

        public FlowableWebSocketClient(WebSocket socket,
                                       FlowableBinanceApiWebSocketListener<T> listener) {
            this.socket = socket;
            this.listener = listener;
        }

        public void closeSocket() {
            socket.close(1001, null);
        }

        public Flowable<T> toFlowable() {
            return listener.toFlowable();
        }
    }

}