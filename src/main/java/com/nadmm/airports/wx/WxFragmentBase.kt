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

import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.core.os.BundleCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.launch

abstract class WxFragmentBase(protected val action: String) : FragmentBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NoaaService.Events.events.collect { result ->
                    val resultAction = result.getString(NoaaService.ACTION)
                    if (resultAction == action) {
                        processResult(result)
                    }
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

    protected fun getSelectedItemText(textInputLayout: TextInputLayout): String {
        val textView = getAutoCompleteTextView(textInputLayout)
        return textView?.getText().toString()
    }

    protected open fun processResult(result: Bundle) {
    }

    protected fun <T> getResultObject(result: Bundle, clazz: Class<T>): T {
        return BundleCompat.getParcelable(result, NoaaService.RESULT, clazz)
            ?: clazz.getDeclaredConstructor().newInstance()
    }

}
