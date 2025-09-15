/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.utils

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nadmm.airports.FragmentBase

class PagerAdapter(
    private val activity: FragmentActivity,
) : FragmentStateAdapter(activity) {
    private val tabs = ArrayList<TabInfo>()

    inner class TabInfo internal constructor(
        val label: String,
        val clazz: Class<out Fragment>,
        val args: Bundle?,
    )

    fun addTab(label: String, clazz: Class<out Fragment>, args: Bundle?) {
        val info = TabInfo(label, clazz, args)
        tabs.add(info)
        notifyItemInserted(tabs.size-1)
    }

    override fun getItemCount() = tabs.size

    override fun createFragment(position: Int): Fragment {
        val info = tabs[position]
        val fragment = info.clazz.getDeclaredConstructor().newInstance()
        fragment.arguments = info.args
        return fragment
    }

    fun getItem(position: Int): FragmentBase? {
        val fragment = activity.supportFragmentManager.findFragmentByTag("f$position")
        return if (fragment != null) fragment as FragmentBase else null
    }

    fun getPageTitle(position: Int) = tabs[position].label
}