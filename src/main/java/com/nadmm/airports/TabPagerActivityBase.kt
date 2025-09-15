/*
 * FlightIntel for Pilots
 *
 * Copyright 2015-2025 Nadeem Hasan <nhasan@nadmm.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nadmm.airports

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nadmm.airports.utils.PagerAdapter

abstract class TabPagerActivityBase : ActivityBase() {
    private var activeTabIndex = -1
    private lateinit var fragmentPager: ViewPager2
    private lateinit var pagerAdapter: PagerAdapter
    private lateinit var tabs: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tab_pager)
        fragmentPager = findViewById(R.id.view_pager)
        pagerAdapter = PagerAdapter(this)
        fragmentPager.adapter = pagerAdapter
        tabs = findViewById(R.id.sliding_tabs)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        TabLayoutMediator(tabs, fragmentPager) { tab, position ->
            tab.text = pagerAdapter.getPageTitle(position)
        }.attach()

        fragmentPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                // Disable the swipe refresh while moving between pages
                currentFragment?.let { fragment ->
                    if (fragment.isRefreshable) {
                        enableDisableSwipeRefresh(state == ViewPager2.SCROLL_STATE_IDLE)
                    }
                }
            }
        })

        fragmentPager.post {
            activeTabIndex = savedInstanceState?.getInt(SAVED_TAB) ?: initialTabIndex
            fragmentPager.setCurrentItem(activeTabIndex, false)
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVED_TAB, fragmentPager.currentItem)
    }

    override fun requestDataRefresh() {
        currentFragment?.let { fragment ->
            if (fragment.isRefreshable) {
                fragment.requestDataRefresh()
            }
        }
    }

    protected fun addTab(label: String, clss: Class<out Fragment>, args: Bundle?=null) {
        pagerAdapter.addTab(label, clss, args)
    }

    private val currentFragment: FragmentBase?
        get() = pagerAdapter.getItem(fragmentPager.currentItem)

    protected open val initialTabIndex: Int = 0

    companion object {
        private const val SAVED_TAB = "saved_tab"
    }
}