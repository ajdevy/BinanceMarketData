package com.binance.currencypairs.ui

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
import com.binance.currencypairs.data.CurrencyPairMarketData
import com.binance.databinding.FragmentCurrencyPairsBinding
import com.binance.ui.CurrencyPairAdapter
import com.github.salomonbrys.kodein.android.KodeinSupportFragment
import com.github.salomonbrys.kodein.instance
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.Subject
import java.util.concurrent.TimeUnit


class QuoteCurrencyPairsFragment : KodeinSupportFragment() {

    private val currencyPairSubject by instance<Subject<List<CurrencyPairMarketData>>>("currencyPairSubject")

    private lateinit var listViewModel: CurrencyPairViewHolder

    companion object {
        private val TAG: String = QuoteCurrencyPairsFragment.javaClass.name
        private val EXTRA_QUOTE_ASSET: String = "EXTRA_QUOTE_ASSET"
        private val EXTRA_IS_FAVORITES: String = "EXTRA_IS_FAVORITES"

        fun newInstance(quoteAsset: String): QuoteCurrencyPairsFragment {

            val fragment = QuoteCurrencyPairsFragment()
            val arguments = Bundle()
            arguments.putString(EXTRA_QUOTE_ASSET, quoteAsset)
            fragment.arguments = arguments
            return fragment
        }

        fun newInstance(isFavorites: Boolean): QuoteCurrencyPairsFragment {
            val fragment = QuoteCurrencyPairsFragment()
            val arguments = Bundle()
            arguments.putBoolean(EXTRA_IS_FAVORITES, isFavorites)
            fragment.arguments = arguments
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val binding = DataBindingUtil.inflate<FragmentCurrencyPairsBinding>(
                inflater, R.layout.fragment_currency_pairs, container, false)

        setupListView(binding.recyclerView)

        return binding.root
    }

    private fun setupListView(recyclerView: RecyclerView) {
        val recyclerViewAdapter = CurrencyPairAdapter(getQuoteAssetArgument())
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = recyclerViewAdapter

        listViewModel = ViewModelProviders.of(this)
                .get(CurrencyPairViewHolder::class.java)

        listViewModel.quoteCurrencyToUsdMarketData.observe(
                this,
                Observer {
                    it?.let { recyclerViewAdapter.setUsdToQuoteCurrencyMarketData(it) }
                })

        listViewModel.currencyPairsMarketData.observe(
                this,
                Observer {
                    it?.let { recyclerViewAdapter.setItems(it) }
                })

        val quoteAsset = (getQuoteAssetArgument() ?: "").toLowerCase()
        val isFavorites = isFavoritesArgument()
        currencyPairSubject
                .throttleFirst(4, TimeUnit.SECONDS)
                .flatMap {
                    Observable.fromIterable(it)
                            .filter {
                                if (isFavorites) {
                                    //FIXME: contains in favorites
                                    false
                                } else {
                                    it.symbol.toLowerCase().endsWith(quoteAsset)
                                }
                            }
                            .toList()
                            .toObservable()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(recyclerView)
                .subscribe(
                        { currencyPairMarketDatas ->
                            listViewModel.currencyPairsMarketData.value = currencyPairMarketDatas
                        },
                        { throwable ->
                            Log.e(TAG, "currencyPairSubject broke", throwable)
                        })

        currencyPairSubject
                .throttleFirst(1, TimeUnit.MINUTES)
                .flatMap {
                    Observable.fromIterable(it)
                            .filter {
                                val lowerCaseSymbol = it.symbol.toLowerCase()
                                lowerCaseSymbol.startsWith(quoteAsset) && lowerCaseSymbol.endsWith("usdt")
                            }
                            .firstElement()
                            .toObservable()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(recyclerView)
                .subscribe(
                        { currencyPairMarketData ->
                            listViewModel.quoteCurrencyToUsdMarketData.value = currencyPairMarketData
                        },
                        { throwable ->
                            Log.e(TAG, "currencyPairSubject usd to quote currency broke", throwable)
                        })

        currencyPairSubject
                .throttleFirst(1, TimeUnit.MINUTES)
                .flatMap {
                    Observable.fromIterable(it)
                            .filter {
                                val lowerCaseSymbol = it.symbol.toLowerCase()
                                lowerCaseSymbol.startsWith(quoteAsset) && lowerCaseSymbol.endsWith("usdt")
                            }
                            .firstElement()
                            .toObservable()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .bindToLifecycle(recyclerView)
                .subscribe(
                        { currencyPairMarketData ->
                            listViewModel.quoteCurrencyToUsdMarketData.value = currencyPairMarketData
                        },
                        { throwable ->
                            Log.e(TAG, "currencyPairSubject usd to quote currency broke", throwable)
                        })
    }

    private fun getQuoteAssetArgument(): String {
        if (arguments != null) {
            return arguments.getString(EXTRA_QUOTE_ASSET, "")
        }
        return ""
    }

    private fun isFavoritesArgument(): Boolean {
        if (arguments != null) {
            return arguments.getBoolean(EXTRA_IS_FAVORITES)
        }
        return false
    }
}