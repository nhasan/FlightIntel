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
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.ListMenuFragment
import com.nadmm.airports.R
import java.text.DecimalFormat
import java.util.*

class UnitConvertFragment : E6bFragmentBase(), OnItemClickListener {
    companion object {
        private val mUnitTypeMap = HashMap<String, Array<Unit>>()

        init {
            mUnitTypeMap["Temperature"] = arrayOf(
                Celsius(),
                Fahrenheit(),
                Rankine(),
                Kelvin()
            )
            mUnitTypeMap["Length"] = arrayOf(
                StatuteMile(),
                NauticalMile(),
                Yard(),
                Foot(),
                Inch(),
                KiloMeter(),
                Centimeter(),
                Millimeter(),
                Meter()
            )
            mUnitTypeMap["Speed"] = arrayOf(
                Knot(),
                MilePerHour(),
                KilometerPerHour(),
                FootPerSecond(),
                MeterPerSecond()
            )
            mUnitTypeMap["Volume"] = arrayOf(
                Gallon(),
                Liter(),
                Quart(),
                FluidOunce(),
                MilliLiter()
            )
            mUnitTypeMap["Mass"] = arrayOf(
                Pound(),
                Ounce(),
                KiloGram(),
                Gram()
            )
            mUnitTypeMap["Pressure"] = arrayOf(
                InchOfHg(),
                HectoPascal(),
                Millibar()
            )
        }
    }

    private val mUnitAdapters = HashMap<String, ArrayAdapter<Unit>>()
    private var mFormat: DecimalFormat? = null
    private var mFromUnitSpinner: TextInputLayout? = null
    private var mToUnitSpinner: TextInputLayout? = null
    private var mFromUnitValue: TextInputLayout? = null
    private var mToUnitValue: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_unit_convert_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subTitle = arguments?.getString(ListMenuFragment.SUBTITLE_TEXT)
        supportActionBar!!.subtitle = subTitle

        // Create the adapters for all unit types
        for (type in mUnitTypeMap.keys) {
            val objects = mUnitTypeMap[type]
            if (objects != null ) {
                val adapter = ArrayAdapter(
                    requireActivity(),
                    R.layout.list_item, objects
                )
                mUnitAdapters[type] = adapter
            }
        }
        mFormat = DecimalFormat("#,##0.###")
        val types = mUnitTypeMap.keys.toTypedArray()
        Arrays.sort(types)
        val adapter = ArrayAdapter(
            requireActivity(),
            R.layout.list_item, types
        )
        val unitTypeSpinner = findViewById<TextInputLayout>(R.id.e6b_unit_type_spinner)
        val textView = getAutoCompleteTextView(unitTypeSpinner!!)
        textView?.setAdapter(adapter)
        textView?.onItemClickListener = this
        mFromUnitSpinner = findViewById(R.id.e6b_unit_from_spinner)
        mToUnitSpinner = findViewById(R.id.e6b_unit_to_spinner)
        mFromUnitValue = findViewById(R.id.e6b_unit_from_value)
        mToUnitValue = findViewById(R.id.e6b_unit_to_value)
        addSpinnerField(mFromUnitSpinner!!)
        addSpinnerField(mToUnitSpinner!!)
        addEditField(mFromUnitValue)
        addReadOnlyField(mToUnitValue)
        setFragmentContentShown(true)
    }

    override val message: String?
        get() = null

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val type = parent.getItemAtPosition(position).toString()
        val unitAdapter = mUnitAdapters[type]!!
        clearEditText(mFromUnitValue)
        clearEditText(mToUnitValue)
        setSpinnerAdapter(mFromUnitSpinner!!, unitAdapter, 0)
        setSpinnerAdapter(mToUnitSpinner!!, unitAdapter, 1)
    }

    override fun processInput() {
        try {
            val fromUnit = getSelectedItem(mFromUnitSpinner!!) as Unit?
            val toUnit = getSelectedItem(mToUnitSpinner!!) as Unit?
            if (fromUnit != null && toUnit != null) {
                val editText = mFromUnitValue!!.editText
                editText!!.inputType = fromUnit.inputType
                val fromValue = parseDouble(mFromUnitValue)
                val toValue = fromUnit.convertTo(toUnit, fromValue)
                showValue(mToUnitValue, mFormat!!.format(toValue))
            }
        } catch (e: NumberFormatException) {
            clearEditText(mToUnitValue)
        }
    }

    private abstract class Unit {
        protected abstract fun multiplicationFactor(): Double
        protected open fun toNormalized(value: Double): Double {
            return value * multiplicationFactor()
        }

        protected open fun fromNormalized(value: Double): Double {
            return value / multiplicationFactor()
        }

        fun convertTo(to: Unit, value: Double): Double {
            return to.fromNormalized(toNormalized(value))
        }

        open val inputType: Int
            get() = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    // See http://en.wikipedia.org/wiki/Conversion_of_units
    // Temperature conversion via Kelvin
    private class Celsius : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.0
        }

        override fun toNormalized(value: Double): Double {
            return value + 273.15
        }

        override fun fromNormalized(value: Double): Double {
            return value - 273.15
        }

        override val inputType: Int
            get() = super.inputType or InputType.TYPE_NUMBER_FLAG_SIGNED

        override fun toString(): String {
            return "\u00B0C"
        }
    }

    private class Fahrenheit : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.0
        }

        override fun toNormalized(value: Double): Double {
            return (value + 459.67) * 5.0 / 9.0
        }

        override fun fromNormalized(value: Double): Double {
            return value * 9 / 5 - 459.67
        }

        override val inputType: Int
            get() = super.inputType or InputType.TYPE_NUMBER_FLAG_SIGNED

        override fun toString(): String {
            return "\u00B0F"
        }
    }

    private class Rankine : Unit() {
        override fun multiplicationFactor(): Double {
            return 5.0 / 9.0
        }

        override val inputType: Int
            get() = super.inputType or InputType.TYPE_NUMBER_FLAG_SIGNED

        override fun toString(): String {
            return "\u00B0Ra"
        }
    }

    private class Kelvin : Unit() {
        override fun multiplicationFactor(): Double {
            return 1.0
        }

        override val inputType: Int
            get() = super.inputType or InputType.TYPE_NUMBER_FLAG_SIGNED

        override fun toString(): String {
            return "K"
        }
    }

    // Distance conversion via Meters
    private class StatuteMile : Unit() {
        override fun multiplicationFactor(): Double {
            return 1609.344
        }

        override fun toString(): String {
            return "mi"
        }
    }

    private class NauticalMile : Unit() {
        override fun multiplicationFactor(): Double {
            return 1852.0
        }

        override fun toString(): String {
            return "nm"
        }
    }

    private class Yard : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.9144
        }

        override fun toString(): String {
            return "yd"
        }
    }

    private class Foot : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.3048
        }

        override fun toString(): String {
            return "ft"
        }
    }

    private class Inch : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.0254
        }

        override fun toString(): String {
            return "in"
        }
    }

    private class Centimeter : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.01
        }

        override fun toString(): String {
            return "cm"
        }
    }

    private class Millimeter : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.001
        }

        override fun toString(): String {
            return "mm"
        }
    }

    private class KiloMeter : Unit() {
        override fun multiplicationFactor(): Double {
            return 1000.0
        }

        override fun toString(): String {
            return "km"
        }
    }

    private class Meter : Unit() {
        override fun multiplicationFactor(): Double {
            return 1.0
        }

        override fun toString(): String {
            return "m"
        }
    }

    // Speed conversion via meters/s
    private class Knot : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.514444
        }

        override fun toString(): String {
            return "knot"
        }
    }

    private class MilePerHour : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.44704
        }

        override fun toString(): String {
            return "mi/h"
        }
    }

    private class KilometerPerHour : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.277778
        }

        override fun toString(): String {
            return "km/h"
        }
    }

    private class FootPerSecond : Unit() {
        override fun multiplicationFactor(): Double {
            return 0.3048
        }

        override fun toString(): String {
            return "ft/s"
        }
    }

    private class MeterPerSecond : Unit() {
        override fun multiplicationFactor(): Double {
            return 1.0
        }

        override fun toString(): String {
            return "m/s"
        }
    }

    // Volume conversion via Litres
    private class Gallon : Unit() {
        public override fun multiplicationFactor(): Double {
            return 3.785411784
        }

        override fun toString(): String {
            return "gal"
        }
    }

    private class Quart : Unit() {
        public override fun multiplicationFactor(): Double {
            return 0.946353
        }

        override fun toString(): String {
            return "qt"
        }
    }

    private class FluidOunce : Unit() {
        public override fun multiplicationFactor(): Double {
            return 0.0295735296
        }

        override fun toString(): String {
            return "fl oz"
        }
    }

    private class MilliLiter : Unit() {
        public override fun multiplicationFactor(): Double {
            return 0.001
        }

        override fun toString(): String {
            return "mL"
        }
    }

    private class Liter : Unit() {
        public override fun multiplicationFactor(): Double {
            return 1.0
        }

        override fun toString(): String {
            return "L"
        }
    }

    // Pressure conversion via Millibar
    private class InchOfHg : Unit() {
        public override fun multiplicationFactor(): Double {
            return 33.863753
        }

        override fun toString(): String {
            return "inHg"
        }
    }

    private class HectoPascal : Unit() {
        public override fun multiplicationFactor(): Double {
            return 1.0
        }

        override fun toString(): String {
            return "hPa"
        }
    }

    private class Millibar : Unit() {
        public override fun multiplicationFactor(): Double {
            return 1.0
        }

        override fun toString(): String {
            return "mbar"
        }
    }

    // Weight conversion via Grams
    private class Pound : Unit() {
        public override fun multiplicationFactor(): Double {
            return 453.59237
        }

        override fun toString(): String {
            return "lb"
        }
    }

    private class Ounce : Unit() {
        public override fun multiplicationFactor(): Double {
            return 28.349523
        }

        override fun toString(): String {
            return "oz"
        }
    }

    private class KiloGram : Unit() {
        public override fun multiplicationFactor(): Double {
            return 1000.0
        }

        override fun toString(): String {
            return "kg"
        }
    }

    private class Gram : Unit() {
        public override fun multiplicationFactor(): Double {
            return 1.0
        }

        override fun toString(): String {
            return "g"
        }
    }
}