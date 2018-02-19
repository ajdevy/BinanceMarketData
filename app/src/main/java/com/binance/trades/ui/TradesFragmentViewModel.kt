package com.binance.trades.ui

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.binance.trades.data.TradeData

class TradesFragmentViewModel : ViewModel() {
    val trades = MutableLiveData<List<TradeData>>()
}