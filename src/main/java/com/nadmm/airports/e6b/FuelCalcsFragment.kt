/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2023 Nadeem Hasan <nhasan@nadmm.com>
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
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.R

class FuelCalcsFragment : E6bFragmentBase() {
    private var mEdit1: TextInputLayout? = null
    private var mEdit2: TextInputLayout? = null
    private var mEdit3: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_fuel_calcs_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
        setFragmentContentShown(true)
    }

    override val message: String
        get() = "You can directly substitute Gallons with Pounds for Jet fuel"

    override fun processInput() {
        try {
            when (menuId) {
                R.id.E6B_FUEL_ENDURANCE -> {
                    val fuelTotal = parseDouble(mEdit1)
                    val fuelRate = parseDouble(mEdit2)
                    val endurance = fuelTotal / (fuelRate / 60)
                    showValue(mEdit3, endurance)
                }
                R.id.E6B_FUEL_TOTAL_BURNED -> {
                    val fuelRate = parseDouble(mEdit1)
                    val endurance = parseDouble(mEdit2)
                    val fuelTotal = endurance / 60 * fuelRate
                    showValue(mEdit3, fuelTotal)
                }
                R.id.E6B_FUEL_BURN_RATE -> {
                    val fuelTotal = parseDouble(mEdit1)
                    val endurance = parseDouble(mEdit2)
                    val fuelRate = fuelTotal / (endurance / 60)
                    showDecimalValue(mEdit3!!, fuelRate)
                }
            }
        } catch (ignored: NumberFormatException) {
            clearEditText(mEdit3)
        }
    }

    private fun setupUi() {
        val label1 = findViewById<TextView>(R.id.e6b_label_value1)
        val label2 = findViewById<TextView>(R.id.e6b_label_value2)
        val label3 = findViewById<TextView>(R.id.e6b_label_value3)
        mEdit1 = findViewById(R.id.e6b_edit_value1)
        mEdit2 = findViewById(R.id.e6b_edit_value2)
        mEdit3 = findViewById(R.id.e6b_edit_value3)
        when (menuId) {
            R.id.E6B_FUEL_ENDURANCE -> {
                label1!!.setText(R.string.total_fuel)
                addEditField(mEdit1, R.string.gal)
                label2!!.setText(R.string.burn_rate)
                addEditField(mEdit2, R.string.gph)
                label3!!.setText(R.string.endurance)
                addReadOnlyField(mEdit3, R.string.min)
            }
            R.id.E6B_FUEL_BURN_RATE -> {
                label1!!.setText(R.string.total_fuel)
                addEditField(mEdit1, R.string.gal)
                label2!!.setText(R.string.endurance)
                addEditField(mEdit2, R.string.min)
                label3!!.setText(R.string.burn_rate)
                addReadOnlyField(mEdit3, R.string.gph)
            }
            R.id.E6B_FUEL_TOTAL_BURNED -> {
                label1!!.setText(R.string.burn_rate)
                addEditField(mEdit1, R.string.gph)
                label2!!.setText(R.string.endurance)
                addEditField(mEdit2, R.string.min)
                label3!!.setText(R.string.total_fuel)
                addReadOnlyField(mEdit3, R.string.gal)
            }
        }
    }
}