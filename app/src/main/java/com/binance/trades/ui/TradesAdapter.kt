package com.binance.trades.ui

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.binance.R
import com.binance.databinding.ItemTradeBinding
import com.binance.trades.data.TradeData
import com.binance.util.removeTrailingZeros
import org.joda.time.DateTimeZone

class TradesAdapter :
        RecyclerView.Adapter<TradesAdapter.TradesDataViewHolder>() {

    private var items: MutableList<TradeData> = ArrayList()
    private var dateTimeZone: DateTimeZone = DateTimeZone.getDefault()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradesDataViewHolder {
        val itemBinding = ItemTradeBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)

        return TradesDataViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: TradesDataViewHolder, position: Int) {
        if (items.size > position) {
            holder.bind(items[position], dateTimeZone)
        } else {
            holder.bindEmptyItem()
        }
    }

    override fun getItemCount(): Int {
        return TradesFragment.TRADE_COUNT_TO_SHOW
    }

    override fun getItemId(position: Int): Long {
        if (items.size > position) {
            return items[position].hashCode().toLong()
        } else {
            return -1
        }
    }

    fun addItems(newItems: List<TradeData>) {
        newItems.forEach {
            if(!items.contains(it)) items.add(it)
        }
        sortAndRemoveUnneededItems()
        notifyDataSetChanged()
    }

    private fun sortAndRemoveUnneededItems() {
        items.sortByDescending { it.tradeTime }

        if (items.size > TradesFragment.TRADE_COUNT_TO_SHOW) {
            items = items.subList(0, TradesFragment.TRADE_COUNT_TO_SHOW)
        }
    }

    fun addItem(newItem: TradeData) {
        if (!items.contains(newItem)) {
            this.items.add(newItem)
        }
        sortAndRemoveUnneededItems()
        notifyDataSetChanged()
    }

    class TradesDataViewHolder(var binding: ItemTradeBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(tradeData: TradeData, dateTimeZone: DateTimeZone) {
            binding.tradePrice.text = tradeData.price
            binding.tradeAmount.text = tradeData.quantity.removeTrailingZeros()
            binding.tradeTime.text = tradeData.tradeTime.withZone(dateTimeZone).toString("hh:mm:ss")
            if (tradeData.isBuyerMaker) {
                binding.tradePrice.setTextColor(binding.root.context.getColor(R.color.sellRed))
            } else {
                binding.tradePrice.setTextColor(binding.root.context.getColor(R.color.buyGreen))
            }
            binding.executePendingBindings()
        }

        fun bindEmptyItem() {
            val emptyTradeString = binding.root.context.getString(R.string.empty_order_book_or_trade_item_placeholder)
            binding.tradePrice.text = emptyTradeString
            binding.tradeTime.text = emptyTradeString
            binding.tradeAmount.text = emptyTradeString
        }
    }

    fun setTimezone(timezone: String) {
        dateTimeZone = DateTimeZone.forID(timezone)
    }
}