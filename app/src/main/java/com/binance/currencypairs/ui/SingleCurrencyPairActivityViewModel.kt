package com.binance.currencypairs.ui

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.binance.currencypairs.data.CurrencyPairMarketData

class SingleCurrencyPairActivityViewModel : ViewModel() {

    val currencyPairMarketData = MutableLiveData<CurrencyPairMarketData>()

    val quoteCurrencyToUsdMarketData = MutableLiveData<CurrencyPairMarketData>()

}