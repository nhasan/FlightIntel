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

class ClimbRateFragment : E6bFragmentBase() {
    private var mClimbGradEdit: TextInputLayout? = null
    private var mGsEdit: TextInputLayout? = null
    private var mClimbRateEdit: TextInputLayout? = null
    private var mClimbGradPctEdit: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_climb_rate_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mClimbGradEdit = findViewById(R.id.e6b_edit_climb_grad)
        mGsEdit = findViewById(R.id.e6b_edit_gs)
        mClimbRateEdit = findViewById(R.id.e6b_edit_climb_rate)
        mClimbGradPctEdit = findViewById(R.id.e6b_edit_climb_grad_pct)
        addEditField(mClimbGradEdit)
        addEditField(mGsEdit)
        addReadOnlyField(mClimbRateEdit)
        addReadOnlyField(mClimbGradPctEdit)
        setFragmentContentShown(true)
    }

    override val message: String
        get() = "Find the minimum required climb rate for a departure procedure"

    override fun processInput() {
        try {
            val climbGrad = parseDouble(mClimbGradEdit)
            val gs = parseDouble(mGsEdit)
            val climbRate = climbGrad * gs / 60
            val climbGradPct = climbGrad / 6076.115 * 100
            showValue(mClimbRateEdit, climbRate)
            showDecimalValue(mClimbGradPctEdit!!, climbGradPct)
        } catch (ignored: NumberFormatException) {
            clearEditText(mClimbRateEdit)
            clearEditText(mClimbGradPctEdit)
        }
    }
}