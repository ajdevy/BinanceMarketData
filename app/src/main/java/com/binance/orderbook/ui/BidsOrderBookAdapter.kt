package com.binance.orderbook.ui

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.binance.R
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.api.client.domain.market.OrderBookEntry
import com.binance.databinding.ItemBidOrderBookBinding
import com.binance.util.getQuotePrecisionFromMinimalPrice
import com.binance.util.removeTrailingZeros
import java.math.BigDecimal
import java.math.RoundingMode

class BidsOrderBookAdapter :
        RecyclerView.Adapter<BidsOrderBookAdapter.OrderBookDataViewHolder>() {

    private var items: List<OrderBookEntry> = ArrayList()

    private var baseAssetPrecision: Int? = null
    private var quotePrecision: Int? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderBookDataViewHolder {
        val itemBinding = ItemBidOrderBookBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)

        return OrderBookDataViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: BidsOrderBookAdapter.OrderBookDataViewHolder, position: Int) {
        if (items.size > position) {
            holder.bind(items[position], baseAssetPrecision, quotePrecision)
        } else {
            holder.bindEmptyItem()
        }
    }

    override fun getItemCount(): Int {
        return OrderBookFragment.ORDER_BOOK_ITEM_COUNT.toInt()
    }

    override fun getItemId(position: Int): Long {
        if (items.size > position) {
            return items[position].price.hashCode().toLong()
        } else {
            return -1
        }
    }

    fun updateItems(newItems: List<OrderBookEntry>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    class OrderBookDataViewHolder(val binding: ItemBidOrderBookBinding)
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
            binding.bidPrice.text = priceText

            val quantityText = orderBookData.qty.removeTrailingZeros()
            binding.bidAmount.text = quantityText
            binding.executePendingBindings()
        }

        fun bindEmptyItem() {
            val emptyOrderBookString = binding.root.context.getString(R.string.empty_order_book_or_trade_item_placeholder)
            binding.bidAmount.text = emptyOrderBookString
            binding.bidPrice.text = emptyOrderBookString

        }
    }

    fun setScale(currencyPairInfo: SymbolInfo) {
        this.baseAssetPrecision = currencyPairInfo.baseAssetPrecision
        this.quotePrecision = currencyPairInfo.getQuotePrecisionFromMinimalPrice()
    }
}

