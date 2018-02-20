package com.binance.currencypairs.ui

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.binance.currencypairs.data.CurrencyPairMarketData

class CurrencyPairViewHolder : ViewModel() {

    val currencyPairsMarketData = MutableLiveData<List<CurrencyPairMarketData>>()

    val quoteCurrencyToUsdMarketData = MutableLiveData<CurrencyPairMarketData>()

}