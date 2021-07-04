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

class TopOfDescentFragment : E6bFragmentBase() {
    private var mInitAltEdit: TextInputLayout? = null
    private var mDesiredAltEdit: TextInputLayout? = null
    private var mGsEdit: TextInputLayout? = null
    private var mDescentRateEdit: TextInputLayout? = null
    private var mFixDistanceEdit: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_top_of_descent_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mInitAltEdit = findViewById(R.id.e6b_edit_initial_alt)
        mDesiredAltEdit = findViewById(R.id.e6b_edit_desired_alt)
        mGsEdit = findViewById(R.id.e6b_edit_gs)
        mDescentRateEdit = findViewById(R.id.e6b_edit_descent_rate)
        mFixDistanceEdit = findViewById(R.id.e6b_edit_fix_distance)
        addEditField(mInitAltEdit)
        addEditField(mDesiredAltEdit)
        addEditField(mGsEdit)
        addEditField(mDescentRateEdit)
        addReadOnlyField(mFixDistanceEdit)
        setFragmentContentShown(true)
    }

    override val message: String
        get() = "Find the distance at which to start the descent to arrive at the" +
                " destination at the desired altitude."

    override fun processInput() {
        try {
            val initAlt = parseDouble(mInitAltEdit)
            val desiredAlt = parseDouble(mDesiredAltEdit)
            val gs = parseDouble(mGsEdit)
            val descentRate = parseDouble(mDescentRateEdit)
            val distance = gs * ((initAlt - desiredAlt) / (descentRate * 60))
            showDecimalValue(mFixDistanceEdit!!, distance)
        } catch (ignored: NumberFormatException) {
            clearEditText(mFixDistanceEdit)
        }
    }
}