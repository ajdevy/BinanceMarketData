package com.binance.util

fun String.removeTrailingZeros(): CharSequence? {
    var stringWithoutTrailingZeros = this
    //remove only zeros after the dot
    if (stringWithoutTrailingZeros.contains(".")) {
        while (stringWithoutTrailingZeros.last() == '0') {
            stringWithoutTrailingZeros = stringWithoutTrailingZeros.dropLast(1)
        }
    }
    return stringWithoutTrailingZeros
}