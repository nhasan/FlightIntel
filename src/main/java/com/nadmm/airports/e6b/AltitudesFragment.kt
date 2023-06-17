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
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.R
import com.nadmm.airports.utils.WxUtils
import kotlin.math.pow
import kotlin.math.roundToInt

class AltitudesFragment : E6bFragmentBase() {
    private var mElevationEdit: TextInputLayout? = null
    private var mAltimeterEdit: TextInputLayout? = null
    private var mTemperatureEdit: TextInputLayout? = null
    private var mDewpointEdit: TextInputLayout? = null
    private var mPressureAltitudeEdit: TextInputLayout? = null
    private var mDensityAltitudeEdit: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_altimetry_altitudes_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mElevationEdit = findViewById(R.id.e6b_edit_elevation)
        mAltimeterEdit = findViewById(R.id.e6b_edit_altimeter_inhg)
        mTemperatureEdit = findViewById(R.id.e6b_edit_temperature_c)
        mDewpointEdit = findViewById(R.id.e6b_edit_dewpoint_c)
        mPressureAltitudeEdit = findViewById(R.id.e6b_edit_pa)
        mDensityAltitudeEdit = findViewById(R.id.e6b_edit_da)
        addEditField(mElevationEdit)
        addEditField(mAltimeterEdit)
        addEditField(mTemperatureEdit)
        addEditField(mDewpointEdit)
        addReadOnlyField(mPressureAltitudeEdit)
        addReadOnlyField(mDensityAltitudeEdit)
        setFragmentContentShown(true)
    }

    override val message: String
        get() =  "At sea level on a standard day, temperature is 15\u00B0C or 59\u00B0F" +
                " and pressure is 29.92126 inHg or 1013.25 mB."

    override fun processInput() {
        try {
            val elevation = parseLong(mElevationEdit)
            val altimeterHg = parseDouble(mAltimeterEdit)
            val altimeterMb = WxUtils.HG_TO_MBAR * altimeterHg
            val temperatureC = parseDouble(mTemperatureEdit)
            val dewPointC = parseDouble(mDewpointEdit)
            val temperatureK = temperatureC + 273.16

            // Source: https://www.weather.gov/media/epz/wxcalc/pressureAltitude.pdf
            val pa = elevation + ((1 - (altimeterMb / WxUtils.ISA_PRESSURE_MBAR)
                .pow(0.190284)) * 145366.45).roundToInt()
            showValue(mPressureAltitudeEdit, pa.toDouble())

            // Source: https://www.weather.gov/media/epz/wxcalc/densityAltitude.pdf
            // Calculate vapor pressure first
            val e = 6.11 * 10.0.pow(7.5 * dewPointC / (237.7 + dewPointC))
            // Next, calculate virtual temperature in Kelvin
            var tv = temperatureK / (1 - (e / altimeterMb) * (1 - 0.622))
            // Convert Kelvin to Rankin to use in the next step
            tv = 9 * (tv - 273.16) / 5 + 32 + 459.69
            val da = elevation + (145366.45 * (1 - (17.326 * altimeterHg / tv).pow(0.235))).roundToInt()
            showValue(mDensityAltitudeEdit, da.toDouble())
        } catch (ignored: NumberFormatException) {
            clearEditText(mPressureAltitudeEdit)
            clearEditText(mDensityAltitudeEdit)
        }
    }
}