package com.binance.trades.ui

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.binance.R
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.currencypairs.ui.QuoteCurrencyPairsFragment
import com.binance.databinding.FragmentTradesBinding
import com.github.salomonbrys.kodein.android.KodeinSupportFragment
import com.github.salomonbrys.kodein.instance
import io.reactivex.subjects.Subject

class TradesFragment : KodeinSupportFragment() {

    private val currencyPairSubject by instance<Subject<List<CurrencyPairMarketData>>>("currencyPairSubject")

    private lateinit var fragmentViewModel: TradesFragmentViewModel

    companion object {
        private val TAG: String = QuoteCurrencyPairsFragment.javaClass.name
        private val EXTRA_SYMBOL: String = "EXTRA_SYMBOL"

        fun newInstance(symbol: String): TradesFragment {

            val fragment = TradesFragment()
            val arguments = Bundle()
            arguments.putString(EXTRA_SYMBOL, symbol)
            fragment.arguments = arguments
            return fragment
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val symbol = getSymbolArgument()
        //FIXME: trades subject
//        currencyPairSubject
//                .throttleFirst(4, TimeUnit.SECONDS)
//                .flatMap {
//                    Observable.fromIterable(it)
//                            .filter { it.symbol == symbol }
//                            .firstElement()
//                            .toObservable()
//                }
//                .subscribeOn(Schedulers.computation())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                        { currencyPairMarketData ->
//                            fragmentViewModel.trades.value = currencyPairMarketData
//                        },
//                        { throwable ->
//                            Log.e(TAG, "currencyPairSubject broke", throwable)
//                        })

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val binding = DataBindingUtil.inflate<FragmentTradesBinding>(
                inflater, R.layout.fragment_trades, container, false)

        setupListView(binding.recyclerView)

        return binding.root
    }

    private fun setupListView(recyclerView: RecyclerView) {
        val recyclerViewAdapter = TradesAdapter()
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = recyclerViewAdapter

        fragmentViewModel = ViewModelProviders.of(this)
                .get(TradesFragmentViewModel::class.java)

        fragmentViewModel.trades.observe(
                this,
                Observer {
                    it?.let { recyclerViewAdapter.updateItems(it) }
                })
    }

    private fun getSymbolArgument(): String? {
        if (arguments != null) {
            return arguments.getString(EXTRA_SYMBOL,"")
        }
        return ""
    }
}