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

class OutsideAirTemperatureFragment : E6bFragmentBase() {
    private var mIatEdit: TextInputLayout? = null
    private var mRecoveryFactorEdit: TextInputLayout? = null
    private var mTasEdit: TextInputLayout? = null
    private var mOatEdit: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_altimetry_oat_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mIatEdit = findViewById(R.id.e6b_edit_iat)
        mRecoveryFactorEdit = findViewById(R.id.e6b_edit_recovery_factor)
        mTasEdit = findViewById(R.id.e6b_edit_tas)
        mOatEdit = findViewById(R.id.e6b_edit_oat)
        showDecimalValue(mRecoveryFactorEdit!!, 0.95, 2)
        addEditField(mIatEdit)
        addEditField(mRecoveryFactorEdit)
        addEditField(mTasEdit)
        addReadOnlyField(mOatEdit)
        setFragmentContentShown(true)
    }

    override val message: String
        get() = "The recovery factor depends on installation, and is usually" +
                " in the range of 0.95 to 1.0, but can be as low as 0.7"

    override fun processInput() {
        try {
            val iat = parseDouble(mIatEdit)
            val k = parseDouble(mRecoveryFactorEdit)
            val tas = parseDouble(mTasEdit)
            val oat = iat - k * tas.pow(2.0) / 7592
            showDecimalValue(mOatEdit!!, oat)
        } catch (ignored: NumberFormatException) {
            clearEditText(mOatEdit)
        }
    }
}