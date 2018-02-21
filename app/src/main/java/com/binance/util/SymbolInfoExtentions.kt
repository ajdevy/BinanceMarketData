package com.binance.util

import com.binance.api.client.domain.general.FilterType
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.util.removeTrailingZeros
import java.math.BigDecimal

fun SymbolInfo.getQuotePrecisionFromMinimalPrice(): Int {
    return BigDecimal(getSymbolFilter(FilterType.PRICE_FILTER).minPrice.removeTrailingZeros()).scale()
}