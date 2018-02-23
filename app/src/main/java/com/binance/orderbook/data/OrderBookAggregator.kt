package com.binance.orderbook.data

import android.util.Log
import com.binance.api.client.domain.event.DepthEvent
import com.binance.api.client.domain.market.OrderBook
import com.binance.api.client.domain.market.OrderBookEntry
import com.binance.rest.MyBinanceApiAsyncRestClient
import com.binance.websocket.MyBinanceApiWebSocketClient
import io.reactivex.Flowable
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

class OrderBookAggregator(private val currencyPairSymbol: String,
                          private val binanceWebSocketClient: MyBinanceApiWebSocketClient,
                          private val binanceApiAsyncRestClient: MyBinanceApiAsyncRestClient) {

    private val TAG = OrderBookAggregator::javaClass.name

    private val buffer: LinkedList<DepthEvent> = LinkedList()
    private val isOrderBookBeingInitialized = AtomicBoolean(false)
    private var depthEventsSocket: MyBinanceApiWebSocketClient.FlowableWebSocketClient<DepthEvent>? = null

    private var orderBookData: OrderBookData? = null

//      -  Open a stream to wss://stream.binance.com:9443/ws/bnbbtc@depth
//      -  Buffer the events you receive from the stream
//      -  Get a depth snapshot from **https://www.binance.com/api/v1/depth?symbol=BNBBTC&limit=1000"
//      -  Drop any event where u is <= lastUpdateId in the snapshot
//      -  The first processed should have U <= lastUpdateId+1 AND u >= lastUpdateId+1
//                "U": 157,           // First update ID in event
//                "u": 160,           // last update ID in event
//      -  While listening to the stream, each new event's U should be equal to the previous event's u+1
//      -  The data in each event is the absolute quantity for a price level
//      -  If the quantity is 0, remove the price level
//      -  Receiving an event that removes a price level that is not in your local order book can happen and is normal.

    private fun initializeOrderBook() {
        if (isOrderBookBeingInitialized.compareAndSet(false, true)) {
            binanceApiAsyncRestClient.getOrderBook(
                    currencyPairSymbol.toUpperCase(),
                    20)
                    .subscribe(
                            {
                                orderBookData = OrderBookData(it)
                                isOrderBookBeingInitialized.set(false)
                            },
                            {
                                Log.d(TAG, "Could not get order book for $currencyPairSymbol")
                            })
        }
    }

    fun listenForChanges(): Flowable<OrderBookData> {
        depthEventsSocket = binanceWebSocketClient.listenForDepthEvents(currencyPairSymbol)
        val depthEventsSocket = depthEventsSocket
        if (depthEventsSocket != null) {
            return depthEventsSocket.toFlowable()
                    .doOnNext { onWebSocketDepthStreamConnected() }
                    .flatMap { depthEvent ->
                        val orderBookData = onOrderBookEvent(depthEvent)
                        if (isOrderBookValid(orderBookData)) {
                            return@flatMap Flowable.just(orderBookData)
                        }
                        return@flatMap Flowable.empty<OrderBookData>()
                    }
        }
        return Flowable.empty()
    }

    fun closeSockets() {
        this.depthEventsSocket?.closeSocket()
        binanceWebSocketClient
    }

    private fun onWebSocketDepthStreamConnected() {
        if (!isOrderBookValid(orderBookData)) {
            initializeOrderBook()
        }
    }

    private fun onOrderBookEvent(depthEvent: DepthEvent): OrderBookData? {
        //  -While listening to the stream, each new event's U
        //   should be equal to the previous event's u+1
        // "U": 157,           // First update ID in event
        // "u": 160,           // last update ID in event
        val shouldInvalidateOrderBook =
                buffer.isNotEmpty()
                        && depthEvent.firstUpdateId != buffer.last.finalUpdateId + 1
        if (shouldInvalidateOrderBook) {
            initializeOrderBook()
        }
        buffer.add(depthEvent)
        processBuffer(orderBookData)
        return orderBookData
    }

    private fun processBuffer(orderBookData: OrderBookData?) {
        if (orderBookData != null) {
            while (buffer.isNotEmpty() && isOrderBookValid(orderBookData)) {
                val depthEvent = buffer.removeFirst()

                //- Drop any event where u is <= lastUpdateId in the snapshot
                val shouldDropDepthEvent: Boolean =
                        depthEvent.finalUpdateId <= orderBookData.lastUpdateId

                //  -The first processed should have [First update ID in event] <= lastUpdateId+1
                // AND [last update ID in event] >= lastUpdateId+1
                val shouldProcessDepthEvent: Boolean =
                        depthEvent.firstUpdateId <= orderBookData.lastUpdateId + 1
                                && depthEvent.finalUpdateId >= orderBookData.lastUpdateId + 1

                if (!shouldDropDepthEvent && shouldProcessDepthEvent) {
                    orderBookData.onOrderBookChanged(depthEvent)
                }
            }
        }
    }

    private fun isOrderBookValid(orderBookData: OrderBookData?): Boolean {
        return !isOrderBookBeingInitialized.get()
                && orderBookData != null
                && orderBookData.isNotEmpty()
    }

}

class OrderBookData(val asks: MutableMap<String, OrderBookEntry>,
                    val bids: MutableMap<String, OrderBookEntry>,
                    var lastUpdateId: Long) {

    constructor(orderBook: OrderBook) : this(
            toPriceMap(orderBook.asks), toPriceMap(orderBook.bids), orderBook.lastUpdateId)

    companion object {
        private val TAG = OrderBookData::class.java.name

        private fun toPriceMap(asks: List<OrderBookEntry>): MutableMap<String, OrderBookEntry> {
            return asks.stream()
                    .collect(Collectors.toMap({ it.price }, { it }))
        }
    }

    fun isNotEmpty() = !asks.isEmpty() || !bids.isEmpty()

    fun onOrderBookChanged(depthEvent: DepthEvent) {
        lastUpdateId = depthEvent.finalUpdateId
        //-The data in each event is the absolute quantity for a price level
        //-If the quantity is 0, remove the price level
        //-Receiving an event that removes a price level that is not in your local order book can happen and is normal.
        depthEvent.asks.forEach {
            val existingAskEntry: OrderBookEntry? = asks[it.price]
            //if amount is 0, remove it
            if (isZero(it.qty)) {
                existingAskEntry?.let { this.asks.remove(it.price) }
            } else {
                this.asks[it.price] = it
            }
        }

        depthEvent.bids.forEach {
            val existingBidEntry: OrderBookEntry? = bids[it.price]
            //if amount is 0, remove it
            if (isZero(it.qty)) {
                existingBidEntry?.let { this.bids.remove(it.price) }
            } else {
                this.bids[it.price] = it
            }
        }

    }

    private fun isZero(qty: String): Boolean {
        return BigDecimal(qty).setScale(10, RoundingMode.HALF_UP) == BigDecimal.ZERO.setScale(10)
    }
}