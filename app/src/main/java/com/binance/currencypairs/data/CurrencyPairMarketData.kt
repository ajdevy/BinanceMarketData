package com.binance.currencypairs.data

import com.binance.api.client.domain.event.AllMarketTickersEvent
import com.binance.api.client.domain.market.TickerStatistics
import java.math.BigDecimal
import java.math.RoundingMode

data class CurrencyPairMarketData(val symbol: String,
                                  val priceChangePercent: BigDecimal,
                                  val lastPrice: BigDecimal,
                                  val highPrice: BigDecimal,
                                  val lowPrice: BigDecimal,
                                  val volume: BigDecimal) {
    companion object {
        private val VOLUME_SCALE = 3
    }

    //        {
//            "e": "24hrTicker",  // Event type
//            "E": 123456789,     // Event time
//            "s": "BNBBTC",      // Symbol
//            "p": "0.0015",      // Price change
//            "P": "250.00",      // Price change percent - needed
//            "w": "0.0018",      // Weighted average price
//            "x": "0.0009",      // Previous day's close price
//            "c": "0.0025",      // Current day's close price
//            "Q": "10",          // Close trade's quantity
//            "b": "0.0024",      // Best bid price
//            "B": "10",          // Bid bid quantity
//            "a": "0.0026",      // Best ask price
//            "A": "100",         // Best ask quantity
//            "o": "0.0010",      // Open price
//            "h": "0.0025",      // High price
//            "l": "0.0010",      // Low price
//            "v": "10000",       // Total traded base asset volume
//            "q": "18",          // Total traded quote asset volume -- needed one!!
//            "O": 0,             // Statistics open time
//            "C": 86400000,      // Statistics close time
//            "F": 0,             // First trade ID
//            "L": 18150,         // Last trade Id
//            "n": 18151          // Total number of trades
//        }
    constructor(ticker: Map<String, String>) : this(
            ticker["s"] ?: throw IllegalArgumentException("could not find symbol in ticker 's' : $ticker"),
            BigDecimal(ticker["P"]),
            BigDecimal(ticker["c"]),
            BigDecimal(ticker["h"]),
            BigDecimal(ticker["l"]),
            BigDecimal(ticker["q"])

    )

    constructor(tickerStatistics: TickerStatistics) : this(
            tickerStatistics.symbol,
            BigDecimal(tickerStatistics.priceChangePercent),
            BigDecimal(tickerStatistics.lastPrice),
            BigDecimal(tickerStatistics.highPrice),
            BigDecimal(tickerStatistics.lowPrice),
            BigDecimal(tickerStatistics.volume).multiply(BigDecimal(tickerStatistics.weightedAvgPrice)).setScale(VOLUME_SCALE, RoundingMode.HALF_UP))

    constructor(ticker: AllMarketTickersEvent) : this(
            ticker.symbol,
            BigDecimal(ticker.priceChangePercent),
            BigDecimal(ticker.currentDaysClosePrice),
            BigDecimal(ticker.highPrice),
            BigDecimal(ticker.lowPrice),
            BigDecimal(ticker.totalTradedQuoteAssetVolume).setScale(VOLUME_SCALE, RoundingMode.HALF_UP))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as CurrencyPairMarketData

        if (symbol != other.symbol) return false

        return true
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

}