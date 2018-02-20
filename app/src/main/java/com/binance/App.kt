package com.binance

import android.app.Application
import android.util.Log
import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiWebSocketClient
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.currencypairs.CurrencyPairSubjectInteractor
import com.binance.currencypairs.data.CurrencyPairList
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.ui.MainActivity
import com.binance.util.InMemory
import com.binance.websocket.MyBinanceApiWebSocketClient
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.autoAndroidModule
import com.squareup.picasso.Picasso
import io.reactivex.subjects.Subject

class App : Application(), KodeinAware {

    private val TAG = App::class.java.name

    override val kodein by Kodein.lazy {
        import(autoAndroidModule(this@App))

        bind<Picasso>() with instance(Picasso.with(this@App))

        bind<BinanceApiClientFactory>() with singleton { BinanceApiClientFactory.newInstance() }
        bind<CurrencyPairList>() with singleton { CurrencyPairList() }

        bind<CurrencyPairSubjectInteractor>() with singleton {
            CurrencyPairSubjectInteractor(instance(), instance(), instance())
        }

        bind<Subject<List<CurrencyPairMarketData>>>("currencyPairSubject") with singleton {
            return@singleton instance<CurrencyPairSubjectInteractor>().create()
        }

        bind<InMemory<ExchangeInfo>>() with singleton {
            val exchangeInfo = InMemory<ExchangeInfo>()

            instance<BinanceApiAsyncRestClient>().getExchangeInfo({
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

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(
                { thread: Thread, throwable: Throwable ->

                    Log.e(TAG, "Got an uncaught exception", throwable)
                })
    }
}