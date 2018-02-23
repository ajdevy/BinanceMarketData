package com.binance.trades.data

import com.binance.api.client.domain.event.AggTradeEvent
import com.binance.api.client.domain.market.AggTrade
import org.joda.time.DateTime
import java.util.stream.Collectors

data class TradeData(val price: String,
                     val quantity: String,
                     val tradeTime: DateTime, val isBuyerMaker: Boolean) {

    constructor(aggTradeEvent: AggTradeEvent) : this(
            aggTradeEvent.price,
            aggTradeEvent.quantity,
            DateTime(aggTradeEvent.tradeTime),
            aggTradeEvent.isBuyerMaker
    )

    companion object {
        fun from(aggTrades: List<AggTrade>): List<TradeData> {
            return aggTrades.stream()
                    .map {
                        TradeData(it.price, it.quantity, DateTime(it.tradeTime), it.isBuyerMaker)
                    }
                    .collect(Collectors.toList())
        }
    }
}