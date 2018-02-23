package com.binance.trades.ui

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.trades.data.TradeData

class TradesFragmentViewModel : ViewModel() {

    val trades = MutableLiveData<List<TradeData>>()

    var newTrade = MutableLiveData<TradeData>()

    val timezone = MutableLiveData<String>()

    val symbolInfo = MutableLiveData<SymbolInfo>()
}