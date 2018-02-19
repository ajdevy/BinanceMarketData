package com.binance.ui

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.view.ViewPager
import com.binance.R
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.currencypairs.ui.QuoteCurrencyPairsFragment
import com.binance.databinding.ActivityMainBinding
import com.binance.util.InMemory
import com.github.salomonbrys.kodein.android.KodeinAppCompatActivity
import com.github.salomonbrys.kodein.instance


class MainActivity : KodeinAppCompatActivity() {

    private val exchangeInfo by instance<InMemory<ExchangeInfo>>()

    companion object {
        val TAG = MainActivity::javaClass.name
    }

    //FIXME: get queote assets form server
    val quoteAssets = listOf("BNB", "BTC", "ETH", "USDT")

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setupViewPager(binding.pager)

        binding.tabs.setupWithViewPager(binding.pager)

    }

    private fun setupViewPager(pager: ViewPager) {
        val adapter = ViewPagerAdapter(supportFragmentManager)

        // Add Fragments to adapter one by one
        //FIXME: refactor to string resource
        adapter.addFragment(QuoteCurrencyPairsFragment.newInstance(true), "Favorites")

        quoteAssets.forEach { adapter.addFragment(QuoteCurrencyPairsFragment.newInstance(it), it) }

        pager.adapter = adapter

    }

//    fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.menu_main, menu)
//        return true
//    }
//
//    fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val id = item.getItemId()
//        return if (id == R.id.action_settings) {
//            true
//        } else super.onOptionsItemSelected(item)
//
//    }
}