package com.binance.ui

import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.binance.R
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.currencypairs.ui.SingleCurrencyPairActivity
import com.binance.databinding.ItemCurrencyPairBinding
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

class CurrencyPairAdapter(val quoteCurrency: String) :
        RecyclerView.Adapter<CurrencyPairAdapter.CurrencyPairViewHolder>() {

    private var items: List<CurrencyPairMarketData> = ArrayList()
    private var previousItems: List<CurrencyPairMarketData> = ArrayList()
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
                item, getPreviousPriceForPair(item.symbol), quoteCurrency, usdToQuoteMarketData)
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

    private fun getPreviousPriceForPair(currencyPairSymbol: String): Optional<CurrencyPairMarketData> {
        return previousItems.stream()
                .filter { it.symbol.toLowerCase() == currencyPairSymbol.toLowerCase() }
                .findFirst()
    }

    class CurrencyPairViewHolder(var binding: ItemCurrencyPairBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(currencyPairMarketData: CurrencyPairMarketData,
                 previousMarketDataForPair: Optional<CurrencyPairMarketData>,
                 quoteCurrency: String,
                 usdToQuoteMarketData: CurrencyPairMarketData?) {

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

            binding.lastPriceTextView.text = currencyPairMarketData.lastPrice.toString()


            val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
            val symbols = formatter.getDecimalFormatSymbols()

            symbols.setGroupingSeparator(',')
            formatter.setDecimalFormatSymbols(symbols)
            val volumeString = formatter.format(currencyPairMarketData.volume.setScale(0, RoundingMode.HALF_UP))
            binding.volumeTextView.text = binding.root.context.getString(
                    R.string.volumeAbbreviation, volumeString)

            binding.root.setOnClickListener {
                val intent = Intent(it.context, SingleCurrencyPairActivity::class.java).apply {
                    putExtra(SingleCurrencyPairActivity.EXTRA_SYMBOL, currencyPairMarketData.symbol)
                }
                it.context.startActivity(intent)
            }

            binding.executePendingBindings()
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