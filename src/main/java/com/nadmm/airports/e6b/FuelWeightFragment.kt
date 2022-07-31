/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2021 Nadeem Hasan <nhasan@nadmm.com>
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.R
import java.util.*

class FuelWeightFragment : E6bFragmentBase() {
    private var mFuelTotal: TextInputLayout? = null
    private var mFuelWeight: TextInputLayout? = null
    private var mTextView: MaterialAutoCompleteTextView? = null
    private val mFuels: HashMap<String?, Double?> = object : HashMap<String?, Double?>() {
        init {
            put("100LL", 6.08)
            put("Jet A1", 6.71)
            put("Jet A", 6.84)
            put("Jet B", 6.36)
            put("JP-5", 6.76)
            put("JP-8", 6.76)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_fuel_weight_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val names = mFuels.keys.toTypedArray()
        Arrays.sort(names)
        val adapter = ArrayAdapter(
            requireActivity(),
            R.layout.list_item, names
        )
        val fuelTypes = findViewById<TextInputLayout>(R.id.e6b_fuel_types)
        mTextView = fuelTypes!!.editText as MaterialAutoCompleteTextView?
        mTextView!!.setAdapter(adapter)
        mTextView!!.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long -> processInput() }
        mFuelTotal = findViewById(R.id.e6b_edit_total_fuel)
        mFuelWeight = findViewById(R.id.e6b_edit_total_weight)
        addEditField(mFuelTotal)
        addReadOnlyField(mFuelWeight)
        setFragmentContentShown(true)
    }

    override val message: String
        get() = "Fuel density used is valid at 15\u00B0C (59\u00B0F) and is an average" +
                " value. Actual weight varies and depends on the API gravity of the batch" +
                " and the ambient temperature. Use this calculator for only an estimate."

    override fun processInput() {
        try {
            val fuelTotal = parseDouble(mFuelTotal)
            val name = mTextView!!.text.toString()
            if (name.isNotEmpty()) {
                val weight = fuelTotal * mFuels[name]!!
                showDecimalValue(mFuelWeight!!, weight)
            }
        } catch (e: RuntimeException) {
            clearEditText(mFuelWeight)
        }
    }
}