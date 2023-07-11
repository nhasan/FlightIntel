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
import kotlin.math.roundToInt

class TrueAltitudeFragment : E6bFragmentBase() {
    private var mIaEdit: TextInputLayout? = null
    private var mOatEdit: TextInputLayout? = null
    private var mStationAltitudeEdit: TextInputLayout? = null
    private var mTrueAltitudeEdit: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_altimetry_ta_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mIaEdit = findViewById(R.id.e6b_edit_ia)
        mOatEdit = findViewById(R.id.e6b_edit_oat)
        mStationAltitudeEdit = findViewById(R.id.e6b_edit_station_altitude)
        mTrueAltitudeEdit = findViewById(R.id.e6b_edit_ta)
        addEditField(mIaEdit)
        addEditField(mOatEdit)
        addEditField(mStationAltitudeEdit)
        addReadOnlyField(mTrueAltitudeEdit)
        setFragmentContentShown(true)
    }

    override val message: String
        get() = "Use altitude of the wx station whose altimeter setting is being used."

    override fun processInput() {
        try {
            val ia = parseDouble(mIaEdit)
            val oat = parseDouble(mOatEdit)
            val stationAltitude = parseDouble(mStationAltitudeEdit)
            if (ia > 65620) {
                mIaEdit!!.error = "Valid values: 0 to 65,620"
                throw NumberFormatException()
            } else {
                mIaEdit!!.error = ""
            }
            var isaTempC = -56.5
            if (ia <= 36089.24) {
                isaTempC = 15.0 - 0.0019812 * ia
            }
            val ta = (ia + (ia - stationAltitude) * (oat - isaTempC) / (273.15 + oat)).roundToInt()
            showValue(mTrueAltitudeEdit, ta.toDouble())
        } catch (ignored: NumberFormatException) {
            clearEditText(mTrueAltitudeEdit)
        }
    }
}