package com.binance.orderbook.ui

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.OrderBookEntry

class OrderBookFragmentViewModel : ViewModel() {

    val currencyPairInfo = MutableLiveData<SymbolInfo>()

    val asks = MutableLiveData<List<OrderBookEntry>>()

    val bids = MutableLiveData<List<OrderBookEntry>>()

}