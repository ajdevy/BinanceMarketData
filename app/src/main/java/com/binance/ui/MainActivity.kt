package com.binance.ui

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.Menu
import android.view.MenuItem
import com.binance.R
import com.binance.api.client.domain.general.ExchangeInfo
import com.binance.currencypairs.ui.QuoteCurrencyPairsFragment
import com.binance.databinding.ActivityMainBinding
import com.binance.util.InMemory
import com.github.salomonbrys.kodein.android.KodeinAppCompatActivity
import com.github.salomonbrys.kodein.instance


class MainActivity : KodeinAppCompatActivity() {

    private val exchangeInfo by instance<InMemory<ExchangeInfo>>()

    private val quoteAssets = listOf("BNB", "BTC", "ETH", "USDT")

    companion object {
        val TAG = MainActivity::javaClass.name
    }

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
        adapter.addFragment(QuoteCurrencyPairsFragment.newInstance(true), getString(R.string.favorites))

        quoteAssets.forEach { adapter.addFragment(QuoteCurrencyPairsFragment.newInstance(it), it) }
        pager.offscreenPageLimit = adapter.count
        pager.adapter = adapter
        //scroll to the second page by default
        pager.setCurrentItem(1, false)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}