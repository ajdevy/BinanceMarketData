package com.binance.currencypairs.ui

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.View
import android.widget.Button
import com.binance.R
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.databinding.ActivityCurrencyPairBinding
import com.binance.orderbook.ui.OrderBookFragment
import com.binance.trades.ui.TradesFragment
import com.binance.ui.ViewPagerAdapter
import com.github.salomonbrys.kodein.android.KodeinAppCompatActivity
import com.github.salomonbrys.kodein.instance
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
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
        val EXTRA_QUOTE_ASSET: String = "EXTRA_SYMBOL"
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: com.binance.databinding.ActivityCurrencyPairBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_currency_pair)
        setupViews(binding)

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
                .bindToLifecycle(binding.root)
                .subscribe(
                        { currencyPairMarketData ->
                            activityViewModel.currencyPairMarketData.value = currencyPairMarketData
                        },
                        { throwable ->
                            Log.e(TAG, "currencyPairSubject broke", throwable)
                        })

        val quoteAssetString = getQuoteAssetExtra()
        currencyPairSubject
                .throttleFirst(1, TimeUnit.MINUTES)
                .flatMap {
                    Observable.fromIterable(it)
                            .filter {
                                val lowerCaseSymbol = it.symbol.toLowerCase()
                                lowerCaseSymbol.startsWith(quoteAssetString) && lowerCaseSymbol.endsWith("usdt")
                            }
                            .firstElement()
                            .toObservable()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(binding.root)
                .subscribe(
                        { currencyPairMarketData ->
                            activityViewModel.quoteCurrencyToUsdMarketData.value = currencyPairMarketData
                        },
                        { throwable ->
                            Log.e(TAG, "currencyPairSubject usd to quote currency broke", throwable)
                        })
    }

    private fun setupViews(binding: ActivityCurrencyPairBinding) {

        val currentCurrencyPairSymbol = getSymbolExtra()

        activityViewModel = ViewModelProviders.of(this)
                .get(SingleCurrencyPairActivityViewModel::class.java)

        activityViewModel.currencyPairMarketData.observe(
                this,
                Observer {
                    //FIXME: map data to views
//                    it?.let { view.set() }
                })

        activityViewModel.quoteCurrencyToUsdMarketData.observe(
                this,
                Observer {
                    //FIXME: map usd price to view
//                    it?.let { view.set() }
                })
        setupViewPager(binding.liveMarketDataPager)

        setupOrderBookMarketTradesViewPagerClickListener(
                binding.liveMarketDataPager, binding.orderBookButton, binding.tradesButton)
    }

    private fun setupOrderBookMarketTradesViewPagerClickListener(pager: ViewPager,
                                                                 orderBookButton: Button,
                                                                 tradesButton: Button) {
        val orderBookPage = 0
        val marketTradesPage = 1
        val clickListener: View.OnClickListener = View.OnClickListener { view ->
            if (view.id == R.id.orderBookButton) {
                if (pager.currentItem != orderBookPage) {
                    pager.setCurrentItem(orderBookPage, false)
                    markSwitcherButtonSelected(orderBookButton)
                    markSwitcherButtonDeselected(tradesButton)
                }
            } else if (view.id == R.id.tradesButton) {
                if (pager.currentItem != marketTradesPage) {
                    pager.setCurrentItem(marketTradesPage, false)
                    markSwitcherButtonSelected(tradesButton)
                    markSwitcherButtonDeselected(orderBookButton)
                }
            }
        }
        orderBookButton.setOnClickListener(clickListener)
        tradesButton.setOnClickListener(clickListener)
    }

    private fun markSwitcherButtonSelected(button: Button) {
        button.setTextColor(button.context.getColor(R.color.selectedDarkButtonTextColor))
        button.setBackgroundColor(button.context.getColor(R.color.selectedDarkButtonBackground))
    }

    private fun markSwitcherButtonDeselected(button: Button) {
        button.setTextColor(button.context.getColor(R.color.deselectedDarkButtonTextColor))
        button.setBackgroundColor(button.context.getColor(R.color.deselectedDarkButtonBackground))
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

    private fun getQuoteAssetExtra(): String {
        if (intent != null && intent.hasExtra(EXTRA_QUOTE_ASSET)) {
            return intent.getStringExtra(EXTRA_QUOTE_ASSET)
        }
        return ""
    }
}