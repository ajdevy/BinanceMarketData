package com.binance.ui

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.binance.R
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.currencypairs.ui.SingleCurrencyPairActivity
import com.binance.databinding.ItemCurrencyPairBinding
import com.binance.util.getQuotePrecisionFromMinimalPrice
import com.f2prateek.rx.preferences2.Preference
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList

class CurrencyPairAdapter(private val quoteCurrency: String,
                          private val favoritesPreference: Preference<Set<String>>) :
        RecyclerView.Adapter<CurrencyPairAdapter.CurrencyPairViewHolder>() {

    private var items: List<CurrencyPairMarketData> = ArrayList()
    private var previousItems: List<CurrencyPairMarketData> = ArrayList()
    private var symbolInfoList: List<SymbolInfo> = ArrayList()
    private var usdToQuoteMarketData: CurrencyPairMarketData? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyPairViewHolder {
        val itemBinding = ItemCurrencyPairBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)

        return CurrencyPairViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: CurrencyPairViewHolder, position: Int) {
        val item = items[position]
        holder.bind(
                item,
                getPreviousPriceForPair(item.symbol),
                quoteCurrency,
                usdToQuoteMarketData,
                symbolInfoList,
                favoritesPreference)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].symbol.hashCode().toLong()
    }

    fun setItems(newItems: List<CurrencyPairMarketData>) {
        this.previousItems = this.items
        this.items = newItems
        notifyDataSetChanged()
    }

    fun setCurrencyInfos(currencyInfos: List<SymbolInfo>) {
        this.symbolInfoList = currencyInfos
    }

    private fun getPreviousPriceForPair(currencyPairSymbol: String): Optional<CurrencyPairMarketData> {
        return previousItems.stream()
                .filter { it.symbol.toLowerCase() == currencyPairSymbol.toLowerCase() }
                .findFirst()
    }

    class CurrencyPairViewHolder(var binding: ItemCurrencyPairBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(currencyPairMarketData: CurrencyPairMarketData,
                 previousMarketDataForPair: Optional<CurrencyPairMarketData>,
                 quoteCurrency: String,
                 usdToQuoteMarketData: CurrencyPairMarketData?,
                 symbolInfoList: List<SymbolInfo>,
                 favoritesPreference: Preference<Set<String>>) {

            binding.favoritesIconImageView.visibility =
                    if (favoritesPreference.get().contains(currencyPairMarketData.symbol)) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

            binding.symbolNameTextView.text = getAssetCurrencyName(currencyPairMarketData.symbol, quoteCurrency)
            binding.quoteCurrencyNameTextView.text = binding.root.context.getString(
                    R.string.quoteCurrencyNamePlaceholder, quoteCurrency.toUpperCase())

            binding.changePercentTextView.text = binding.root.context.getString(
                    R.string.priceChangePercent, currencyPairMarketData.priceChangePercent.toString())

            //change box color depending on price percentage change
            when (currencyPairMarketData.priceChangePercent.compareTo(BigDecimal.ZERO)) {
                -1 -> {
                    binding.changePercentTextView.setBackgroundColor(binding.root.context.getColor(R.color.sellRedDark))
                }
                0, 1 -> {
                    binding.changePercentTextView.setBackgroundColor(binding.root.context.getColor(R.color.buyGreen))
                }
            }

            //change price text color depending on price percentage change
            previousMarketDataForPair.ifPresent { previousData ->
                when (currencyPairMarketData.lastPrice.compareTo(previousData.lastPrice)) {
                    1 -> {
                        binding.lastPriceTextView.setTextColor(binding.root.context.getColor(R.color.buyGreen))
                    }
                    0 -> {
                        binding.lastPriceTextView.setTextColor(binding.root.context.getColor(android.R.color.white))
                    }
                    -1 -> {
                        binding.lastPriceTextView.setTextColor(binding.root.context.getColor(R.color.sellRed))
                    }
                }
            }

            //TODO: map usd text view
            val priceInUsd = getPriceInUsdText(currencyPairMarketData, usdToQuoteMarketData)
            binding.usdPriceTextView.text = priceInUsd

            val symbolInfo = getSymbolInfo(currencyPairMarketData.symbol, symbolInfoList)

            val lastPriceString =
                    if (symbolInfo.isPresent) {
                        currencyPairMarketData.lastPrice
                                .setScale(symbolInfo.get().getQuotePrecisionFromMinimalPrice())
                                .toString()
                    } else {
                        currencyPairMarketData.lastPrice.toString()
                    }
            binding.lastPriceTextView.text = lastPriceString


            val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
            val symbols = formatter.decimalFormatSymbols
            symbols.setGroupingSeparator(',')
            formatter.setDecimalFormatSymbols(symbols)

            val volumeString = formatter.format(currencyPairMarketData.volume.setScale(0, RoundingMode.HALF_UP))
            binding.volumeTextView.text = binding.root.context.getString(
                    R.string.volumeAbbreviation, volumeString)

            binding.root.setOnClickListener {
                val intent = Intent(it.context, SingleCurrencyPairActivity::class.java).apply {
                    putExtra(SingleCurrencyPairActivity.EXTRA_SYMBOL, currencyPairMarketData.symbol)
                    putExtra(SingleCurrencyPairActivity.EXTRA_QUOTE_ASSET, quoteCurrency)
                }
                it.context.startActivity(intent)
            }

            binding.executePendingBindings()
        }

        private fun getSymbolInfo(symbol: String,
                                  symbolInfoList: List<SymbolInfo>): Optional<SymbolInfo> {
            val lowerCaseSymbol = symbol.toLowerCase()
            return symbolInfoList.stream()
                    .filter { it.symbol.toLowerCase() == lowerCaseSymbol }
                    .findFirst()
        }

        private fun getPriceInUsdText(currencyPairMarketData: CurrencyPairMarketData,
                                      usdToQuoteMarketData: CurrencyPairMarketData?): String {
            if (usdToQuoteMarketData != null) {
                val priceInUsd = (currencyPairMarketData.lastPrice * usdToQuoteMarketData.lastPrice).setScale(2, RoundingMode.HALF_UP)
                return binding.root.context.getString(R.string.usdPrice, priceInUsd.toString())
            }
            return ""
        }

        private fun getAssetCurrencyName(symbol: String, quoteCurrency: String): String {
            return symbol.toLowerCase()
                    .removeSuffix(quoteCurrency.toLowerCase())
                    .toUpperCase()
        }
    }

    fun setUsdToQuoteCurrencyMarketData(usdToQuoteMarketData: CurrencyPairMarketData) {
        this.usdToQuoteMarketData = usdToQuoteMarketData
    }
}