package com.binance.rest

import com.binance.api.client.BinanceApiCallback

interface MyBinanceApiCallback<T> : BinanceApiCallback<T> {

    fun onError(throwable: Throwable)
}
