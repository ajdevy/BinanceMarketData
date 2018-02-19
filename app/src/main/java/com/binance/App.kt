package com.binance

import android.app.Application
import android.util.Log
import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiWebSocketClient
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.currencypairs.data.CurrencyPairList
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.ui.MainActivity
import com.binance.util.InMemory
import com.binance.websocket.MyBinanceApiWebSocketClient
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.autoAndroidModule
import com.squareup.picasso.Picasso
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.stream.Collectors

class App : Application(), KodeinAware {

    private val TAG = App::class.java.name

    override val kodein by Kodein.lazy {
        import(autoAndroidModule(this@App))

        bind<Picasso>() with instance(Picasso.with(this@App))

        bind<BinanceApiClientFactory>() with singleton { BinanceApiClientFactory.newInstance() }
        bind<CurrencyPairList>() with singleton { CurrencyPairList() }

        bind<Subject<List<CurrencyPairMarketData>>>("currencyPairSubject") with
                singleton {
                    val currencyPairSubject = BehaviorSubject.create<List<CurrencyPairMarketData>>().toSerialized()
                    val binanceWebSocketClient: MyBinanceApiWebSocketClient = instance()
                    val currencyPairs: CurrencyPairList = instance()
                    val binanceApiAsyncRestClient: BinanceApiAsyncRestClient = instance()

                    //initial population of currencyPairMarketData
                    binanceApiAsyncRestClient.getAll24HrPriceStatistics { allPricePairs ->
                        currencyPairs.initItems(allPricePairs)
                        currencyPairSubject.onNext(currencyPairs)
                    }
                    //subscribe for updating currencyPairMarketData
                    binanceWebSocketClient.listenForAllMarketTickersEventMap()
                            .onErrorResumeNext(binanceWebSocketClient.listenForAllMarketTickersEventMap()
                                    .doOnNext { "onErrorResumeNext has been called" })
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                    { tickers ->
                                        val newCurrencyPairsMarketData = tickers.stream()
                                                .map { ticker ->
                                                    CurrencyPairMarketData(ticker)
                                                }
                                                .collect(Collectors.toList())
                                        currencyPairs.updateItems(newCurrencyPairsMarketData)
                                        currencyPairSubject.onNext(currencyPairs)
//                                        Log.d(TAG, "all market tickers event $currencyPairs")
                                    },
                                    { throwable ->
                                        Log.e(TAG, "All market tickers subject broke", throwable)
                                    })
                    return@singleton currencyPairSubject
                }

        bind<InMemory<ExchangeInfo>>() with singleton {
            val binanceApiAsyncRestClient: BinanceApiAsyncRestClient = instance()
            val exchangeInfo = InMemory<ExchangeInfo>()
            binanceApiAsyncRestClient.getExchangeInfo({
                Log.d(MainActivity.TAG, "exchange info $it")
                exchangeInfo.set(it)
            })
            return@singleton exchangeInfo
        }

        bind<MyBinanceApiWebSocketClient>() with singleton { MyBinanceApiWebSocketClient() }
        bind<BinanceApiWebSocketClient>() with singleton { instance<MyBinanceApiWebSocketClient>() }
        bind<BinanceApiAsyncRestClient>() with singleton { instance<BinanceApiClientFactory>().newAsyncRestClient() }

        bind<String>() with instance("DemoApplication")
    }

}