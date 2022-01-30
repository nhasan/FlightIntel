/*
 * FlightIntel for Pilots
 *
 * Copyright 2015-2022 Nadeem Hasan <nhasan@nadmm.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nadmm.airports

import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import android.os.Bundle
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.nadmm.airports.utils.PagerAdapter

abstract class TabPagerActivityBase : ActivityBase() {
    private var mCurrentTabIndex = -1
    private lateinit var mViewPager: ViewPager
    private var mPagerAdapter: PagerAdapter? = null
    private var mTabLayout: TabLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_pager)
        mViewPager = findViewById(R.id.view_pager)
        mPagerAdapter = PagerAdapter(this, supportFragmentManager, mViewPager)
        mViewPager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageScrollStateChanged(state: Int) {
                // Disable the swipe refresh while moving between pages
                if (currentFragment.isRefreshable) {
                    enableDisableSwipeRefresh(state == ViewPager.SCROLL_STATE_IDLE)
                }
            }
        })
        mTabLayout = findViewById(R.id.sliding_tabs)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mTabLayout!!.setupWithViewPager(mViewPager)
        mTabLayout!!.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                mCurrentTabIndex = tab.position
                mViewPager.currentItem = mCurrentTabIndex
                enableDisableSwipeRefresh(currentFragment.isRefreshable)
                showAppBar(true)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {
                if (mCurrentTabIndex != tab.position) {
                    onTabSelected(tab)
                }
            }
        })
        mCurrentTabIndex = savedInstanceState?.getInt(SAVED_TAB) ?: initialTabIndex
        postRunnable({
            if (mCurrentTabIndex >= 0 && mCurrentTabIndex < mTabLayout!!.tabCount) {
                mTabLayout!!.getTabAt(mCurrentTabIndex)!!.select()
                enableDisableSwipeRefresh(currentFragment.isRefreshable)
            }
        }, 0)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVED_TAB, mViewPager.currentItem)
    }

    override fun requestDataRefresh() {
        val fragment = currentFragment
        if (fragment.isRefreshable) {
            fragment.requestDataRefresh()
        }
    }

    protected fun addTab(label: String?, clss: Class<*>?, args: Bundle?) {
        mPagerAdapter!!.addTab(label, clss, args)
    }

    private val currentFragment: FragmentBase
        get() = mPagerAdapter!!.getItem(mCurrentTabIndex) as FragmentBase

    protected open val initialTabIndex: Int
        get() = 0

    companion object {
        private const val SAVED_TAB = "saved_tab"
    }
}