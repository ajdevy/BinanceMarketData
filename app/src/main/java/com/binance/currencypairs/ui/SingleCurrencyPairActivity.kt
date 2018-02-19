package com.binance.currencypairs.ui

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.util.Log
import com.binance.R
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.orderbook.ui.OrderBookFragment
import com.binance.trades.ui.TradesFragment
import com.binance.ui.ViewPagerAdapter
import com.binance.util.InMemory
import com.github.salomonbrys.kodein.android.KodeinAppCompatActivity
import com.github.salomonbrys.kodein.instance
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit

class SingleCurrencyPairActivity : KodeinAppCompatActivity() {

    private val currencyPairSubject by instance<Subject<List<CurrencyPairMarketData>>>("currencyPairSubject")

    private lateinit var activityViewModel: SingleCurrencyPairActivityViewModel

    companion object {
        private val TAG: String = QuoteCurrencyPairsFragment.javaClass.name
        val EXTRA_SYMBOL: String = "EXTRA_SYMBOL"
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews()

        val symbol = getSymbolExtra()
        currencyPairSubject
                .throttleFirst(4, TimeUnit.SECONDS)
                .flatMap {
                    Observable.fromIterable(it)
                            .filter { it.symbol == symbol }
                            .firstElement()
                            .toObservable()
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { currencyPairMarketData ->
                            activityViewModel.currencyPairMarketData.value = currencyPairMarketData
                        },
                        { throwable ->
                            Log.e(TAG, "currencyPairSubject broke", throwable)
                        })

    }

    private fun setupViews() {
        val binding: com.binance.databinding.ActivityCurrencyPairBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_currency_pair)
        val currentCurrencyPairSymbol = getSymbolExtra()

        activityViewModel = ViewModelProviders.of(this)
                .get(SingleCurrencyPairActivityViewModel::class.java)

        activityViewModel.currencyPairMarketData.observe(
                this,
                Observer {
                    //FIXME: map data to views
//                    it?.let { recyclerViewAdapter.updateItems(it) }
                })
        setupViewPager(binding.liveMarketDataPager)
    }


    private fun setupViewPager(pager: ViewPager) {
        val adapter = ViewPagerAdapter(supportFragmentManager)

        val currencyPair = getSymbolExtra()
        adapter.addFragment(OrderBookFragment.newInstance(currencyPair), getString(R.string.order_book))
        adapter.addFragment(TradesFragment.newInstance(currencyPair), getString(R.string.trades))

        pager.adapter = adapter
    }

    private fun getSymbolExtra(): String {
        if (intent != null && intent.hasExtra(EXTRA_SYMBOL)) {
            return intent.getStringExtra(EXTRA_SYMBOL)
        }
        return ""
    }
}