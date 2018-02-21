package com.binance.orderbook.ui

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.OrderBookEntry
import com.binance.databinding.ItemAskOrderBookBinding
import com.binance.util.getQuotePrecisionFromMinimalPrice
import com.binance.util.removeTrailingZeros
import java.math.BigDecimal
import java.math.RoundingMode

class AsksOrderBookAdapter :
        RecyclerView.Adapter<AsksOrderBookAdapter.OrderBookDataViewHolder>() {

    private var items: List<OrderBookEntry> = ArrayList()

    private var baseAssetPrecision: Int? = null
    private var quotePrecision: Int? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderBookDataViewHolder {
        val itemBinding = ItemAskOrderBookBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)

        return OrderBookDataViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: OrderBookDataViewHolder, position: Int) {
        holder.bind(items[position], baseAssetPrecision, quotePrecision)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].price.hashCode().toLong()
    }

    fun updateItems(newItems: List<OrderBookEntry>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    class OrderBookDataViewHolder(val binding: ItemAskOrderBookBinding)
        : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(orderBookData: OrderBookEntry,
                 baseAssetPrecision: Int?,
                 quotePrecision: Int?) {
            val priceText =
                    if (quotePrecision != null) {
                        BigDecimal(orderBookData.price)
                                .setScale(quotePrecision, RoundingMode.HALF_UP)
                                .toString()
                    } else {
                        orderBookData.price
                    }
            binding.askPrice.text = priceText

            val quantityText = orderBookData.qty.removeTrailingZeros()
            binding.askAmount.text = quantityText
            binding.executePendingBindings()
        }
    }

    fun setScale(currencyPairInfo: SymbolInfo) {
        this.baseAssetPrecision = currencyPairInfo.baseAssetPrecision
        this.quotePrecision = currencyPairInfo.getQuotePrecisionFromMinimalPrice()
    }
}