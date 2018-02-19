package com.binance.trades.ui

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.binance.databinding.ItemCurrencyPairBinding
import com.binance.trades.data.TradeData

class TradesAdapter :
        RecyclerView.Adapter<TradesAdapter.TradesDataViewHolder>() {

    private var items: MutableList<TradeData> = ArrayList()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TradesDataViewHolder {
        val itemBinding = ItemCurrencyPairBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)

        return TradesDataViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: TradesDataViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].symbol.hashCode().toLong()
    }

    fun updateItems(newItems: List<TradeData>) {
        //FIXME: add items
        this.items.addAll(newItems)
//        if (this.items.updateItems(newItems)) {
//            notifyDataSetChanged()
//        }
    }

    class TradesDataViewHolder(var binding: ItemCurrencyPairBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(tradeData: TradeData) {
            //FIXME: bind items
//            binding.symbolNameTextView.text = currencyPairMarketData.symbol
//            binding.lastPriceTextView.text = currencyPairMarketData.lastPrice.toString()
//            binding.volumeTextView.text = currencyPairMarketData.volume.toString()
            binding.executePendingBindings()
        }
    }
}