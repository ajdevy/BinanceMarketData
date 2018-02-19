package com.numbers.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.binance.currencypairs.data.CurrencyPairList
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.currencypairs.ui.SingleCurrencyPairActivity
import com.binance.databinding.ItemCurrencyPairBinding
import java.math.RoundingMode

class CurrencyPairAdapter :
        RecyclerView.Adapter<CurrencyPairAdapter.CurrencyPairViewHolder>() {

    private var items: CurrencyPairList = CurrencyPairList()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyPairViewHolder {
        val itemBinding = ItemCurrencyPairBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)

        return CurrencyPairViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: CurrencyPairViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].symbol.hashCode().toLong()
    }

    fun updateItems(newItems: List<CurrencyPairMarketData>) {
        if (this.items.updateItems(newItems)) {
            notifyDataSetChanged()
        }
    }

    class CurrencyPairViewHolder(var binding: ItemCurrencyPairBinding) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(currencyPairMarketData: CurrencyPairMarketData) {
            binding.symbolNameTextView.text = currencyPairMarketData.symbol
            binding.lastPriceTextView.text = currencyPairMarketData.lastPrice.toString()
            binding.volumeTextView.text = currencyPairMarketData.volume.setScale(3, RoundingMode.HALF_UP).toString()
            binding.root.setOnClickListener {
                val intent = Intent(it.context, SingleCurrencyPairActivity::class.java).apply {
                    putExtra(SingleCurrencyPairActivity.EXTRA_SYMBOL, currencyPairMarketData.symbol)
                }
                it.context.startActivity(intent)
            }
            binding.executePendingBindings()
        }
    }
}