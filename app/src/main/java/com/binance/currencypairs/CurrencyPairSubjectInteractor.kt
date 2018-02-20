package com.binance.currencypairs

import android.util.Log
import com.binance.App
import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.currencypairs.data.CurrencyPairList
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.websocket.MyBinanceApiWebSocketClient
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import org.joda.time.DateTime
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.stream.Collectors

class CurrencyPairSubjectInteractor(private val binanceWebSocketClient: MyBinanceApiWebSocketClient,
                                    private val binanceApiAsyncRestClient: BinanceApiAsyncRestClient,
                                    private val currencyPairs: CurrencyPairList) {

    private val TAG = App::class.java.name

    private var existingSubscription: Disposable? = null

    private var lastUpdateTime: Optional<DateTime> = Optional.empty()

    val currencyPairSubject = BehaviorSubject.create<List<CurrencyPairMarketData>>().toSerialized()

    fun create(): Subject<List<CurrencyPairMarketData>> {

        //initial population of currencyPairMarketData
        binanceApiAsyncRestClient.getAll24HrPriceStatistics { allPricePairs ->
            currencyPairs.initItems(allPricePairs)
            val currencyPairsToPropogate = ArrayList<CurrencyPairMarketData>()
            currencyPairsToPropogate.addAll(currencyPairs)
            currencyPairSubject.onNext(currencyPairsToPropogate)
        }

        subscribeToAllMarketTickers()

        setupReconnectOnNoEvents()

        return currencyPairSubject
    }

    private fun subscribeToAllMarketTickers() {
        val currentExistingSubscription = existingSubscription
        if (currentExistingSubscription != null && !currentExistingSubscription.isDisposed) {
            currentExistingSubscription.dispose()
        }
        //subscribe for updating currencyPairMarketData
        val newSubscription = binanceWebSocketClient.listenForAllMarketTickersEventMap()
                .subscribeOn(Schedulers.io())
                .subscribe(
                        { tickersNotification ->
                            val newCurrencyPairsMarketData = tickersNotification.stream()
                                    .filter { it != null }
                                    .map { ticker ->
                                        CurrencyPairMarketData(ticker)
                                    }
                                    .collect(Collectors.toList())
                            currencyPairs.updateItems(newCurrencyPairsMarketData)

                            val currencyPairsToPropogate = ArrayList<CurrencyPairMarketData>()
                            currencyPairsToPropogate.addAll(currencyPairs)
                            currencyPairSubject.onNext(currencyPairsToPropogate)
                        },
                        { throwable ->
                            Log.e(TAG, "All market tickers subject broke", throwable)

                            subscribeToAllMarketTickers()
                        })
        existingSubscription = newSubscription
    }

    private fun setupReconnectOnNoEvents() {
        currencyPairSubject
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        { lastUpdateTime = Optional.of(DateTime.now()) },
                        { Log.d(TAG, "Could not set last update time", it) })

        Observable.interval(1, TimeUnit.MINUTES)
                .filter { oneMinutesPassedSinceLastOrderBookUpdate() }
                .subscribeOn(Schedulers.newThread())
                .subscribe(
                        { subscribeToAllMarketTickers()},
                        { Log.d(TAG, "Could not reconnect on no events", it) })
    }

    private fun oneMinutesPassedSinceLastOrderBookUpdate(): Boolean = lastUpdateTime
            .map { it.isBefore(DateTime.now().minusMinutes(1)) }
            .orElse(true)

}