package com.binance.rest

import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.api.client.domain.market.AggTrade
import com.binance.api.client.domain.market.OrderBook
import com.binance.api.client.domain.market.TickerStatistics
import com.binance.api.client.impl.BinanceApiService
import com.binance.api.client.impl.BinanceApiServiceGenerator
import io.reactivex.Single
import io.reactivex.SingleEmitter
import retrofit2.Callback

class MyBinanceApiAsyncRestClient(apiKey: String?, secret: String?) {

    constructor() : this(null, null)

    private val binanceApiService: BinanceApiService = BinanceApiServiceGenerator.createService(BinanceApiService::class.java, apiKey, secret)

    fun getExchangeInfo(): Single<ExchangeInfo> {
        return Single.fromCallable<ExchangeInfo>({
            return@fromCallable this.binanceApiService.exchangeInfo
                    .execute().body()
        })
    }

    fun getOrderBook(symbol: String, limit: Int?): Single<OrderBook> {
        return Single.fromCallable<OrderBook>({
            return@fromCallable this.binanceApiService.getOrderBook(symbol, limit)
                    .execute().body()
        })
    }

    fun getAggTrades(symbol: String,
                     fromId: String?,
                     limit: Int?,
                     startTime: Long?,
                     endTime: Long?): Single<List<AggTrade>> {
        return Single.fromCallable<List<AggTrade>>({
            return@fromCallable this.binanceApiService.getAggTrades(symbol, fromId, limit, startTime, endTime)
                    .execute().body()
        })
    }

    fun getAll24HrPriceStatistics(): Single<List<TickerStatistics>> {
        return Single.fromCallable<List<TickerStatistics>>({
            return@fromCallable  this.binanceApiService.all24HrPriceStatistics
                    .execute().body()
        })
    }

    private fun <T> createCallbackAdapter(it: SingleEmitter<T>): Callback<T> {
        return MyBinanceApiCallbackAdapter(object : MyBinanceApiCallback<T> {
            override fun onResponse(response: T) {
                it.onSuccess(response)
            }

            override fun onError(throwable: Throwable) {
                if (!it.isDisposed) {
                    it.onError(throwable)
                }
            }
        })
    }

}
