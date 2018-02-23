package com.binance.rest

import com.binance.api.client.exception.BinanceApiException
import com.binance.api.client.impl.BinanceApiServiceGenerator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyBinanceApiCallbackAdapter<T>(private val callback: MyBinanceApiCallback<T>) : Callback<T> {

    override fun onResponse(call: Call<T>, response: Response<T>) {
        if (response.isSuccessful) {
            this.callback.onResponse(response.body())
        } else if (response.code() != 504) {
            try {
                val apiError = BinanceApiServiceGenerator.getBinanceApiError(response)
                callback.onError(BinanceApiException(apiError))
            } catch (t: Throwable) {
                callback.onError(t)
            }
        }
    }

    override fun onFailure(call: Call<T>, throwable: Throwable) {
        callback.onError(throwable)
    }
}
