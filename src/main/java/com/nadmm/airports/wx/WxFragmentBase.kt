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

abstract class WxFragmentBase(protected val action: String) : FragmentBase() {

    private val filter: IntentFilter = IntentFilter().apply { addAction(action) }
    private var broadcastReceiver: BroadcastReceiver = makeBroadcastReceiver()

    private fun makeBroadcastReceiver(): BroadcastReceiver {
        // Create a BroadcastReceiver that will handle the broadcast intents
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (filter.matchAction(intent.action)) {
                    handleBroadcast(intent)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.registerReceiver(broadcastReceiver, filter)
    }

    override fun onPause() {
        super.onPause()

        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.unregisterReceiver(broadcastReceiver)
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

    protected fun getSelectedItemText(textInputLayout: TextInputLayout): String {
        val textView = getAutoCompleteTextView(textInputLayout)
        return textView?.getText().toString()
    }

    protected open fun handleBroadcast(intent: Intent) {
    }
}
