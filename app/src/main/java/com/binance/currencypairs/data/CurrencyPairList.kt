package com.binance.currencypairs.data

import com.binance.api.client.domain.market.TickerStatistics
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

class CurrencyPairList : ArrayList<CurrencyPairMarketData>() {

    private val isUpdating = AtomicBoolean(false)

    fun initItems(newItems: List<TickerStatistics>) {
        isUpdating.set(true)
        forceUpdateItems(newItems.stream().map { CurrencyPairMarketData(it) }.collect(Collectors.toList()))
        isUpdating.set(false)
    }

    fun updateItems(newItems: List<CurrencyPairMarketData>): Boolean {
        if (isUpdating.compareAndSet(false, true)) {
            forceUpdateItems(newItems)
            isUpdating.compareAndSet(true, false)
            return true
        } else {
            return false
        }
    }

    private fun forceUpdateItems(newItems: List<CurrencyPairMarketData>) {
        newItems.forEach {
            val itemIndex = indexOf(it)
            if (itemIndex != -1) {
                //update the item
                set(itemIndex, it)
            } else {
                //add new item
                add(it)
            }
        }
        sortByDescending { it.volume }
    }
}