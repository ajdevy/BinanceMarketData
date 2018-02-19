package com.binance.currencypairs;

import android.util.Log;

import com.binance.api.client.BinanceApiAsyncRestClient;

public class CurrencyPairInteractor {

    public static final String TAG = CurrencyPairInteractor.class.getName();

    public void getCurrencyPairs(BinanceApiAsyncRestClient binanceApiAsyncRestClient) {
        binanceApiAsyncRestClient.getExchangeInfo(
                exchangeInfo -> Log.d(TAG, "exchange info:  " + exchangeInfo)
        );

    }

}
