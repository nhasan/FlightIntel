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
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.R
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class TrueAirspeedFragment : E6bFragmentBase() {
    private var mIndicatedAirSpeedEdit: TextInputLayout? = null
    private var mIndicatedAltitudeEdit: TextInputLayout? = null
    private var mAltimeterEdit: TextInputLayout? = null
    private var mTemperatureEdit: TextInputLayout? = null
    private var mTrueAirSpeedEdit: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_altimetry_tas_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mIndicatedAirSpeedEdit = findViewById(R.id.e6b_edit_ias)
        mIndicatedAltitudeEdit = findViewById(R.id.e6b_edit_altitude)
        mAltimeterEdit = findViewById(R.id.e6b_edit_altimeter)
        mTemperatureEdit = findViewById(R.id.e6b_edit_temperature_c)
        mTrueAirSpeedEdit = findViewById(R.id.e6b_edit_tas)
        addEditField(mIndicatedAirSpeedEdit)
        addEditField(mIndicatedAltitudeEdit)
        addEditField(mAltimeterEdit)
        addEditField(mTemperatureEdit)
        addReadOnlyField(mTrueAirSpeedEdit)
        setFragmentContentShown(true)
    }

    override val message: String
        get() = "True airspeed is affected by density altitude. True airspeed" +
                " exceeds indicated airspeed as density altitude increases."

    override fun processInput() {
        try {
            val ias = parseDouble(mIndicatedAirSpeedEdit)
            val altitude = parseDouble(mIndicatedAltitudeEdit)
            val altimeter = parseDouble(mAltimeterEdit)
            val temperatureC = parseDouble(mTemperatureEdit)
            val delta = 145442.2 * (1 - (altimeter / 29.92126).pow(0.190261))
            val pa = altitude + delta
            val stdTempK = 15.0 - 0.0019812 * altitude + 273.15
            val actTempK = temperatureC + 273.15
            val da = pa + stdTempK / 0.0019812 * (1 - (stdTempK / actTempK).pow(0.234969))
            val factor = sqrt(
                ((stdTempK - da * 0.0019812) / stdTempK).pow(1 / 0.234969)
            )
            val tas = (ias / factor).roundToInt() - 1
            showValue(mTrueAirSpeedEdit, tas.toDouble())
        } catch (ignored: NumberFormatException) {
            clearEditText(mTrueAirSpeedEdit)
        }
    }
}