/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.wx

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.utils.UiUtils

abstract class WxFragmentBase : FragmentBase() {
    private val filter = IntentFilter()
    private var mReceiver: BroadcastReceiver? = null
    protected abstract val product: String?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    public override fun onResume() {
        super.onResume()

        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.registerReceiver(mReceiver!!, filter)
    }

    public override fun onPause() {
        super.onPause()

        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.unregisterReceiver(mReceiver!!)
    }

    protected fun setupBroadcastFilter(action: String) {
        filter.addAction(action)

        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (filter.matchAction(intent.action)) {
                    handleBroadcast(intent)
                }
            }
        }
    }

    protected fun addWxRow(layout: LinearLayout, label: String, code: String?): View {
        val row = addProgressRow(layout, label)
        row.tag = code
        val background = UiUtils.getSelectableItemBackgroundResource(requireActivity())
        row.setBackgroundResource(background)
        return row
    }

    protected fun getAutoCompleteTextView(textInputLayout: TextInputLayout): AutoCompleteTextView? {
        return textInputLayout.getEditText() as AutoCompleteTextView?
    }

    protected fun getSelectedItemPos(textInputLayout: TextInputLayout): Int {
        val textView = getAutoCompleteTextView(textInputLayout)
        if (textView == null) return -1

        val adapter = textView.adapter as ArrayAdapter<*>?
        if (adapter == null) return -1

        val text = textView.getText().toString()
        if (!text.isEmpty()) {
            val count = adapter.count
            for (i in 0..<count) {
                val o: Any? = adapter.getItem(i)
                if (o.toString() == text) return i
            }
        }
        return -1
    }

    protected fun getSelectedItemText(textInputLayout: TextInputLayout): String {
        val textView = getAutoCompleteTextView(textInputLayout)
        return textView?.getText().toString()
    }

    protected open fun handleBroadcast(intent: Intent) {
    }
}
