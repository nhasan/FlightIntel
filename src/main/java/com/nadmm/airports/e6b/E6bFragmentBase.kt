/*
 * FlightIntel for Pilots
 *
 * Copyright 2017-2021 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.e6b

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.ListMenuFragment
import com.nadmm.airports.R
import java.util.*
import kotlin.math.roundToInt

abstract class E6bFragmentBase : FragmentBase() {
    protected val mTextWatcher: TextWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable) {
            processInput()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val label = findViewById<TextView>(R.id.e6b_label)
        val args = arguments
        if (label != null && args != null) {
            val title = args.getString(ListMenuFragment.SUBTITLE_TEXT)
            label.text = title
        }
        val text = message
        if (text != null) {
            val msg = findViewById<TextView>(R.id.e6b_msg)
            msg!!.text = text
        }
    }

    override fun onResume() {
        super.onResume()
        hideKeyboard()
    }

    override fun onPause() {
        super.onPause()
        hideKeyboard()
    }

    protected fun addEditField(textInputLayout: TextInputLayout?, textHintId: Int) {
        addEditField(textInputLayout)
        textInputLayout?.setHint(textHintId)
    }

    protected fun addEditField(textInputLayout: TextInputLayout?) {
        val editText = textInputLayout?.editText
        editText?.addTextChangedListener(mTextWatcher)
    }

    protected fun addSpinnerField(textInputLayout: TextInputLayout) {
        val textView = getAutoCompleteTextView(textInputLayout)
        if (textView != null) {
            textView.onItemClickListener =
                OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long -> processInput() }
        }
    }

    protected fun setSpinnerAdapter(
        textInputLayout: TextInputLayout, adapter: ArrayAdapter<*>?,
        selectedIndex: Int
    ) {
        val textView = getAutoCompleteTextView(textInputLayout)
        if (textView != null) {
            if (adapter != null) {
                textView.setAdapter(adapter)
            }
            textView.setText(adapter!!.getItem(selectedIndex).toString(), false)
        }
    }

    protected fun getSelectedItem(textInputLayout: TextInputLayout): Any? {
        val textView = getAutoCompleteTextView(textInputLayout) ?: return null
        val adapter = textView.adapter as ArrayAdapter<*>
        val text = textView.text.toString()
        if (text.isNotEmpty()) {
            val count = adapter.count
            for (i in 0 until count) {
                val o = adapter.getItem(i)
                if (o.toString() == text) return o
            }
        }
        return null
    }

    protected fun getAutoCompleteTextView(textInputLayout: TextInputLayout): AutoCompleteTextView? {
        return textInputLayout.editText as AutoCompleteTextView?
    }

    protected fun addReadOnlyField(textInputLayout: TextInputLayout, textHintId: Int) {
        addReadOnlyField(textInputLayout)
        textInputLayout.setHint(textHintId)
    }

    protected fun addReadOnlyField(textInputLayout: TextInputLayout?) {
        val editText = textInputLayout?.editText
        editText?.inputType = InputType.TYPE_NULL
        editText?.keyListener = null
    }

    protected fun getValue(textInputLayout: TextInputLayout?): String {
        val edit = textInputLayout?.editText
        return edit?.text?.toString() ?: ""
    }

    protected fun parseDouble(textInputLayout: TextInputLayout?): Double {
        return getValue(textInputLayout).toDouble()
    }

    protected fun parseLong(textInputLayout: TextInputLayout?): Long {
        return getValue(textInputLayout).toLong()
    }

    protected fun parseDirection(textInputLayout: TextInputLayout?): Double {
        var direction = parseDouble(textInputLayout)
        if (direction == 0.0 || direction > 360) {
            textInputLayout?.error = "Valid values: 1 to 360"
            throw NumberFormatException()
        } else {
            direction = Math.toRadians(direction)
            textInputLayout?.error = ""
        }
        return direction
    }

    protected fun parseDeclination(textInputLayout: TextInputLayout?): Double {
        val declination = parseDouble(textInputLayout)
        if (declination < -45 || declination > 45) {
            textInputLayout?.error = "Valid values: -45 to +45"
            throw NumberFormatException()
        } else {
            textInputLayout?.error = ""
        }
        return declination
    }

    protected fun parseAltitude(textInputLayout: TextInputLayout?): Double {
        val altitude = parseDouble(textInputLayout)
        if (altitude < 0 || altitude > 65620) {
            textInputLayout?.error = "Valid values: 0 to 65,620"
            throw NumberFormatException()
        } else {
            textInputLayout?.error = ""
        }
        return altitude
    }

    protected fun parseRunway(textInputLayout: TextInputLayout?): Double {
        val rwy = parseDouble(textInputLayout)
        if (rwy < 1 || rwy > 36) {
            textInputLayout?.error = "Valid values: 1 to 36"
            throw NumberFormatException()
        } else {
            textInputLayout?.error = ""
        }
        return rwy
    }

    protected fun showValue(textInputLayout: TextInputLayout?, value: String?) {
        val editText = textInputLayout?.editText
        editText?.setText(value)
    }

    protected fun showValue(textInputLayout: TextInputLayout?, value: Double) {
        showValue(textInputLayout, value.roundToInt().toString())
    }

    protected fun showDecimalValue(
        textInputLayout: TextInputLayout,
        value: Double,
        decimals: Int = 1
    ) {
        val fmt = String.format(Locale.US, "%%.%df", decimals)
        showValue(textInputLayout, String.format(Locale.US, fmt, value))
    }

    protected fun showDirection(textInputLayout: TextInputLayout, dirRadians: Double) {
        val dirDegrees = Math.toDegrees(normalizeDir(dirRadians))
        showValue(textInputLayout, dirDegrees.roundToInt().toDouble())
    }

    protected fun clearEditText(textInputLayout: TextInputLayout?) {
        showValue(textInputLayout, "")
    }

    protected fun normalizeDir(radians: Double): Double {
        if (radians <= 0) {
            return radians + 2 * Math.PI
        } else if (radians > 2 * Math.PI) {
            return radians - 2 * Math.PI
        }
        return radians
    }

    fun hideKeyboard() {
        val imm = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().rootView.windowToken, 0)
    }

    protected abstract val message: String?
    protected abstract fun processInput()
}