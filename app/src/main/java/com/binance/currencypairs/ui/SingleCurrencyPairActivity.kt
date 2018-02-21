package com.binance.currencypairs.ui

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import com.binance.R
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.api.client.domain.general.SymbolInfo
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.databinding.ActivityCurrencyPairBinding
import com.binance.orderbook.ui.OrderBookFragment
import com.binance.trades.ui.TradesFragment
import com.binance.ui.ViewPagerAdapter
import com.binance.util.InMemory
import com.binance.util.getQuotePrecisionFromMinimalPrice
import com.f2prateek.rx.preferences2.Preference
import com.github.salomonbrys.kodein.android.KodeinAppCompatActivity
import com.github.salomonbrys.kodein.instance
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

class SingleCurrencyPairActivity : KodeinAppCompatActivity() {

    private val currencyPairSubject by instance<Subject<List<CurrencyPairMarketData>>>("currencyPairSubject")
    private val exchangeInfo by instance<InMemory<ExchangeInfo>>()
    private val favoritesPreference by instance<Preference<Set<String>>>("favorites")

    private lateinit var activityViewModel: SingleCurrencyPairActivityViewModel
    private var previousCurrencyPairMarketData: CurrencyPairMarketData? = null
    private var quoteCurrencyToUsdMarketData: CurrencyPairMarketData? = null

    companion object {
        private val TAG: String = QuoteCurrencyPairsFragment.javaClass.name
        val EXTRA_SYMBOL: String = "EXTRA_SYMBOL"
        val EXTRA_QUOTE_ASSET: String = "EXTRA_QUOTE_ASSET"
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.currencyPairPlaceholder, getAssetCurrency(), getQuoteCurrencyExtra())
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back)

        val binding: com.binance.databinding.ActivityCurrencyPairBinding =
                DataBindingUtil.setContentView(this, R.layout.activity_currency_pair)
        setupViews(binding)

        listenForCurrencyPairChanges(binding)

        listenForUsdToQuoteCurrencyChanges(binding)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.single_currency_acitvity_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val item = menu?.findItem(R.id.action_add_to_favorites)
        item?.let {
            val existingFavorites = favoritesPreference.get()
            val symbol = getSymbolExtra()
            if (existingFavorites.contains(symbol)) {
                item.icon = getDrawable(android.R.drawable.star_big_on)
            } else {
                item.icon = getDrawable(android.R.drawable.star_big_off)
            }
            invalidateOptionsMenu()
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                this.finish()
                return true
            }
            R.id.action_add_to_favorites -> {
                val existingFavorites = favoritesPreference.get()
                val symbol = getSymbolExtra()
                if (existingFavorites.contains(symbol)) {
//                    item.icon = getDrawable(android.R.drawable.star_big_off)
                    favoritesPreference.set(existingFavorites.minus(symbol))
                } else {
                    favoritesPreference.set(existingFavorites.plus(symbol))
//                    item.icon = getDrawable(android.R.drawable.star_big_on)
                }
                invalidateOptionsMenu()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun setupViews(binding: ActivityCurrencyPairBinding) {

        activityViewModel = ViewModelProviders.of(this)
                .get(SingleCurrencyPairActivityViewModel::class.java)

        setupSymbolInfo(binding)

        setupViewPager(binding.liveMarketDataPager)

        setupOrderBookMarketTradesViewPagerClickListener(
                binding.liveMarketDataPager, binding.orderBookButton, binding.tradesButton)
    }

    private var symbolInfo: SymbolInfo? = null

    private fun setupSymbolInfo(binding: ActivityCurrencyPairBinding) {
        val currentCurrencyPairSymbol = getSymbolExtra().toLowerCase()

        exchangeInfo.asObservable()
                .flatMap { Flowable.fromIterable(it.symbols) }
                .filter { it.symbol.toLowerCase() == currentCurrencyPairSymbol }
                .firstElement()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(binding.root)
                .subscribe(
                        {
                            activityViewModel.currencyPairInfo.value = it
                        },
                        {
                            Log.e(TAG, "Could not get info for symbol $currentCurrencyPairSymbol," +
                                    " $exchangeInfo", it)
                        })

        activityViewModel.currencyPairInfo.observe(
                this,
                Observer {
                    it?.let {
                        symbolInfo = it
                    }
                })
    }

    private fun listenForCurrencyPairChanges(binding: ActivityCurrencyPairBinding) {
        val symbol = getSymbolExtra()

        currencyPairSubject
                .throttleFirst(500, TimeUnit.MILLISECONDS)
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
        activityViewModel.currencyPairMarketData.observe(
                this,
                Observer { currentMarketData ->
                    //FIXME: map data to views
                    currentMarketData?.let { currentMarketData ->
                        val symbolInfo = this.symbolInfo
                        //map current price in quote currency
                        val lastPriceString: String
                        val priceChangeString: String
                        val lowPriceString: String
                        val highPriceString: String

                        if (symbolInfo != null) {
                            val priceScale = symbolInfo.getQuotePrecisionFromMinimalPrice()
                            lastPriceString = currentMarketData.lastPrice
                                    .setScale(priceScale, RoundingMode.HALF_UP).toString()
                            priceChangeString = currentMarketData.priceChange
                                    .setScale(priceScale, RoundingMode.HALF_UP).toString()
                            lowPriceString = currentMarketData.lowPrice
                                    .setScale(priceScale, RoundingMode.HALF_UP).toString()
                            highPriceString = currentMarketData.highPrice
                                    .setScale(priceScale, RoundingMode.HALF_UP).toString()
                        } else {
                            lastPriceString = currentMarketData.lastPrice.toString()
                            priceChangeString = currentMarketData.priceChange.toString()
                            lowPriceString = currentMarketData.lowPrice.toString()
                            highPriceString = currentMarketData.highPrice.toString()
                        }

                        binding.lowPriceTextView.text = lowPriceString
                        binding.highPriceTextView.text = highPriceString

                        binding.priceChange.text = priceChangeString
                        binding.currentPriceTextView.text = lastPriceString

                        binding.priceChangePercentage.text =
                                binding.root.context.getString(
                                        R.string.priceChangePercent,
                                        currentMarketData.priceChangePercent.toString())

                        val priceChangeTextColor = getPriceTextColor(
                                binding.root.context, currentMarketData, previousCurrencyPairMarketData)
                        //map current price in quote currency color
                        binding.currentPriceTextView.setTextColor(priceChangeTextColor)
                        binding.priceChange.setTextColor(priceChangeTextColor)
                        binding.priceChangePercentage.setTextColor(priceChangeTextColor)


                        //map current price in usd
                        mapUsdPriceTextView(
                                binding, currentMarketData, quoteCurrencyToUsdMarketData)

                        binding.volumeTextView.text =
                                binding.root.context.getString(
                                        R.string.volumeWithCurrencyPlaceholder,
                                        currentMarketData.volume
                                                .setScale(2, RoundingMode.HALF_UP)
                                                .toString(),
                                        getAssetCurrency())

                        previousCurrencyPairMarketData = currentMarketData
                    }
                })
    }

    private fun listenForUsdToQuoteCurrencyChanges(binding: ActivityCurrencyPairBinding) {
        val quoteAssetString = getQuoteCurrencyExtra().toLowerCase()
        currencyPairSubject
                .throttleFirst(1, TimeUnit.SECONDS)
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

        activityViewModel.quoteCurrencyToUsdMarketData.observe(
                this,
                Observer {
                    it?.let {
                        quoteCurrencyToUsdMarketData = it
                        mapUsdPriceTextView(
                                binding, previousCurrencyPairMarketData, quoteCurrencyToUsdMarketData)
                    }
                })
    }

    private fun mapUsdPriceTextView(binding: ActivityCurrencyPairBinding,
                                    previousCurrencyPairMarketData: CurrencyPairMarketData?,
                                    quoteCurrencyToUsdMarketData: CurrencyPairMarketData?) {
        if (previousCurrencyPairMarketData != null && quoteCurrencyToUsdMarketData != null) {
            binding.currentPriceInUsdTextView.text =
                    getPriceInUsdText(
                            binding.root.context, previousCurrencyPairMarketData, quoteCurrencyToUsdMarketData)
        }
    }

    private fun getPriceInUsdText(context: Context,
                                  currencyPairMarketData: CurrencyPairMarketData,
                                  usdToQuoteMarketData: CurrencyPairMarketData?): String {
        if (usdToQuoteMarketData != null) {
            val priceInUsd = (currencyPairMarketData.lastPrice
                    * usdToQuoteMarketData.lastPrice).setScale(2, RoundingMode.HALF_UP)
            return context.getString(R.string.usdPrice, priceInUsd.toString())
        }
        return ""
    }

    private fun getPriceTextColor(context: Context,
                                  currentMarketData: CurrencyPairMarketData,
                                  previousMarketData: CurrencyPairMarketData?): Int {
        if (previousMarketData != null) {
            if (currentMarketData.lastPrice > previousMarketData.lastPrice) {
                return context.getColor(R.color.buyGreen)
            } else if (currentMarketData.lastPrice < previousMarketData.lastPrice) {
                return context.getColor(R.color.sellRed)
            }
        }
        return context.getColor(R.color.lightGray)

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

    private fun getQuoteCurrencyExtra(): String {
        if (intent != null && intent.hasExtra(EXTRA_QUOTE_ASSET)) {
            return intent.getStringExtra(EXTRA_QUOTE_ASSET)
        }
        return ""
    }

    private fun getAssetCurrency(): String {
        return getSymbolExtra().removeSuffix(getQuoteCurrencyExtra())
    }
}