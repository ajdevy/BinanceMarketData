package com.binance.orderbook.ui

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.binance.R
import com.binance.api.client.BinanceApiAsyncRestClient
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.currencypairs.ui.QuoteCurrencyPairsFragment
import com.binance.databinding.FragmentOrderBookBinding
import com.binance.orderbook.data.OrderBookAggregator
import com.binance.util.InMemory
import com.binance.websocket.MyBinanceApiWebSocketClient
import com.github.salomonbrys.kodein.android.KodeinSupportFragment
import com.github.salomonbrys.kodein.instance
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class OrderBookFragment : KodeinSupportFragment() {

    private val binanceWebSocketClient: MyBinanceApiWebSocketClient by instance()
    private val binanceRestClient: BinanceApiAsyncRestClient by instance()
    private val exchangeInfo by instance<InMemory<ExchangeInfo>>()

    private lateinit var fragmentViewModel: OrderBookFragmentViewModel
    private lateinit var orderBookAggregator: OrderBookAggregator

    companion object {
        private val TAG: String = QuoteCurrencyPairsFragment.javaClass.name
        private val EXTRA_SYMBOL: String = "EXTRA_SYMBOL"

        fun newInstance(symbol: String): OrderBookFragment {

            val fragment = OrderBookFragment()
            val arguments = Bundle()
            arguments.putString(EXTRA_SYMBOL, symbol)
            fragment.arguments = arguments
            return fragment
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val currentCurrencyPairSymbol = getSymbolArgument().toLowerCase()

        orderBookAggregator = OrderBookAggregator(
                currentCurrencyPairSymbol, binanceWebSocketClient, binanceRestClient)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val binding = DataBindingUtil.inflate<FragmentOrderBookBinding>(
                inflater, R.layout.fragment_order_book, container, false)

        setupListViews(binding)

        return binding.root
    }

    private fun setupListViews(binding: FragmentOrderBookBinding) {
        val asksAdapter = AsksOrderBookAdapter()
        binding.asksRecyclerView.layoutManager = LinearLayoutManager(activity)
        binding.asksRecyclerView.adapter = asksAdapter

        val bidsAdapter = BidsOrderBookAdapter()
        binding.bidsRecyclerView.layoutManager = LinearLayoutManager(activity)
        binding.bidsRecyclerView.adapter = bidsAdapter

        val currentCurrencyPairSymbol = getSymbolArgument().toLowerCase()

        fragmentViewModel = ViewModelProviders.of(this)
                .get(OrderBookFragmentViewModel::class.java)

        exchangeInfo.asObservable()
                .flatMap { Flowable.fromIterable(it.symbols) }
                .filter { it.symbol.toLowerCase() == currentCurrencyPairSymbol }
                .firstElement()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(binding.root)
                .subscribe(
                        {
                            fragmentViewModel.currencyPairInfo.value = it
                        },
                        {
                            Log.e(TAG, "Could not get info for symbol $currentCurrencyPairSymbol," +
                                    " $exchangeInfo", it)
                        })

        fragmentViewModel.asks.observe(
                this,
                Observer { it?.let { asksAdapter.updateItems(it) } })

        fragmentViewModel.bids.observe(
                this,
                Observer { it?.let { bidsAdapter.updateItems(it) } })

        fragmentViewModel.currencyPairInfo.observe(
                this,
                Observer {
                    it?.let {
                        asksAdapter.setScale(it)
                        bidsAdapter.setScale(it)
                    }
                })

        orderBookAggregator.listenForChanges()
                .throttleFirst(1, TimeUnit.SECONDS)
                .onErrorResumeNext(orderBookAggregator.listenForChanges())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(binding.root)
                .subscribe(
                        {
                            fragmentViewModel.asks.value = it.asks.entries.stream()
                                    .map { it.value }
                                    .sorted { first, second -> first.price.compareTo(second.price) }
                                    .collect(Collectors.toList())
                            fragmentViewModel.bids.value = it.bids.entries.stream()
                                    .map { it.value }
                                    .sorted { first, second -> second.price.compareTo(first.price) }
                                    .collect(Collectors.toList())
                        },
                        {
                            Log.e(TAG, "orderBookAggregator subject broke", it)
                        })
    }

    private fun getSymbolArgument(): String {
        if (arguments != null) {
            return arguments.getString(EXTRA_SYMBOL, "")
        }
        return ""
    }
}