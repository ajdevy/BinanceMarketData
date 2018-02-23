package com.binance.trades.ui

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.binance.R
import com.binance.api.client.domain.event.AggTradeEvent
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.currencypairs.ui.SingleCurrencyPairActivityViewModel
import com.binance.databinding.FragmentTradesBinding
import com.binance.rest.MyBinanceApiAsyncRestClient
import com.binance.trades.data.TradeData
import com.binance.util.InMemory
import com.binance.util.getQuotePrecisionFromMinimalPrice
import com.binance.websocket.MyBinanceApiWebSocketClient
import com.github.salomonbrys.kodein.android.KodeinSupportFragment
import com.github.salomonbrys.kodein.instance
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


class TradesFragment : KodeinSupportFragment() {

    private val binanceRestClient: MyBinanceApiAsyncRestClient by instance()
    private val binanceWebSocketClient by instance<MyBinanceApiWebSocketClient>()
    private val exchangeInfo by instance<InMemory<ExchangeInfo>>()

    private var tradesWebSocket: MyBinanceApiWebSocketClient.FlowableWebSocketClient<AggTradeEvent>? = null
    private lateinit var fragmentViewModel: TradesFragmentViewModel

    companion object {
        val TRADE_COUNT_TO_SHOW = SingleCurrencyPairActivityViewModel.BOTTOM_LIST_MAX_ITEM_COUNT

        private val TAG: String = TradesFragment::class.java.name
        private val EXTRA_SYMBOL: String = "EXTRA_SYMBOL"

        fun newInstance(symbol: String): TradesFragment {

            val fragment = TradesFragment()
            val arguments = Bundle()
            arguments.putString(EXTRA_SYMBOL, symbol)
            fragment.arguments = arguments
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val binding = DataBindingUtil.inflate<FragmentTradesBinding>(
                inflater, R.layout.fragment_trades, container, false)

        setupListView(binding.recyclerView)

        val symbol = getSymbolArgument()

        setupSymbolInfo(binding)
        setupTimezone(binding.recyclerView)
        getAndShowLastTrades(binding.recyclerView, symbol)
        listenForNewTrades(binding.recyclerView, symbol)

        return binding.root
    }

    @SuppressLint("MissingSuperCall")
    override fun onDestroy() {
        super.onDestroy()
        tradesWebSocket?.closeSocket()
    }

    private fun setupTimezone(recyclerView: RecyclerView) {
        exchangeInfo.asObservable()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(recyclerView)
                .subscribe(
                        {
                            fragmentViewModel.timezone.value = it.timezone
                        },
                        { Log.e(TAG, "Could not get exchange info for timezone") })
    }

    private fun getAndShowLastTrades(recyclerView: RecyclerView, symbol: String) {
        binanceRestClient.getAggTrades(
                symbol, null, TradesFragment.TRADE_COUNT_TO_SHOW, null, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(recyclerView)
                .subscribe(
                        {
                            fragmentViewModel.trades.value = TradeData.from(it)
                        },
                        { Log.e(TAG, "Could not get agg trades for $symbol") })
    }

    private fun listenForNewTrades(recyclerView: RecyclerView, symbol: String) {
        val newTradesWebSocket = binanceWebSocketClient.listenForAggTradeEvent(symbol)
        tradesWebSocket = newTradesWebSocket

        newTradesWebSocket.toFlowable()
                .map { TradeData(it) }
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(recyclerView)
                .subscribe(
                        {
                            fragmentViewModel.newTrade.value = it
                        },
                        { throwable ->
                            Log.e(TAG, "listenForNewTrades broke", throwable)
                            listenForNewTrades(recyclerView, symbol)
                        })
    }

    private fun setupListView(recyclerView: RecyclerView) {
        val recyclerViewAdapter = TradesAdapter()
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = recyclerViewAdapter
        recyclerView.itemAnimator = null
        fragmentViewModel = ViewModelProviders.of(this)
                .get(TradesFragmentViewModel::class.java)

        fragmentViewModel.trades.observe(
                this,
                Observer {
                    it?.let { recyclerViewAdapter.addItems(it) }
                })

        fragmentViewModel.newTrade.observe(
                this,
                Observer {
                    it?.let { recyclerViewAdapter.addItem(it) }
                })

        fragmentViewModel.timezone.observe(
                this,
                Observer {
                    it?.let { recyclerViewAdapter.setTimezone(it) }
                })

        fragmentViewModel.symbolInfo.observe(
                this,
                Observer {
                    it?.let {
                        recyclerViewAdapter.setPriceScale(it.getQuotePrecisionFromMinimalPrice())
                    }
                })
    }

    private fun setupSymbolInfo(binding: FragmentTradesBinding) {
        val currentCurrencyPairSymbol = getSymbolArgument().toLowerCase()

        exchangeInfo.asObservable()
                .flatMap { Flowable.fromIterable(it.symbols) }
                .filter { it.symbol.toLowerCase() == currentCurrencyPairSymbol }
                .firstElement()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(binding.root)
                .subscribe(
                        {
                            fragmentViewModel.symbolInfo.value = it
                        },
                        {
                            Log.e(TAG, "Could not get info for symbol $currentCurrencyPairSymbol," +
                                    " $exchangeInfo", it)
                        })
    }

    private fun getSymbolArgument(): String {
        if (arguments != null) {
            return arguments.getString(EXTRA_SYMBOL, "")
        }
        return ""
    }
}