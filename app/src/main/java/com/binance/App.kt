package com.binance

import android.app.Application
import android.util.Log
import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.BinanceApiWebSocketClient
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.currencypairs.CurrencyPairSubjectInteractor
import com.binance.currencypairs.data.CurrencyPairList
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.rest.MyBinanceApiAsyncRestClient
import com.binance.util.InMemory
import com.binance.websocket.MyBinanceApiWebSocketClient
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.github.salomonbrys.kodein.*
import com.github.salomonbrys.kodein.android.autoAndroidModule
import com.squareup.picasso.Picasso
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject

class App : Application(), KodeinAware {

    private val TAG = App::class.java.name

    override val kodein by Kodein.lazy {
        import(autoAndroidModule(this@App))

        bind<RxSharedPreferences>() with singleton {
            return@singleton RxSharedPreferences.create(
                    getSharedPreferences("favorites_prefs", android.content.Context.MODE_PRIVATE))
        }

        bind<Preference<Set<String>>>("favorites") with singleton {
            val rxSharedPreferences: RxSharedPreferences = instance()

            return@singleton rxSharedPreferences.getStringSet("favorites", HashSet())
        }

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

            instance<MyBinanceApiAsyncRestClient>().getExchangeInfo()
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            {
                                Log.d(TAG, "exchange info $it")
                                exchangeInfo.set(it)
                            },
                            {
                                Log.e(TAG, "Could not get exchange info", it)
                            })
            return@singleton exchangeInfo
        }

        bind<MyBinanceApiWebSocketClient>() with singleton { MyBinanceApiWebSocketClient() }


        bind<BinanceApiWebSocketClient>() with singleton { instance<MyBinanceApiWebSocketClient>() }
        bind<MyBinanceApiAsyncRestClient>() with singleton { MyBinanceApiAsyncRestClient(null, null) }

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