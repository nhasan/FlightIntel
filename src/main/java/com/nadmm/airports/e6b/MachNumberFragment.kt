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
import kotlin.math.sqrt

class MachNumberFragment : E6bFragmentBase() {
    private var mTasEdit: TextInputLayout? = null
    private var mOatEdit: TextInputLayout? = null
    private var mMachEdit: TextInputLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_altimetry_mach_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mTasEdit = findViewById(R.id.e6b_edit_tas)
        mOatEdit = findViewById(R.id.e6b_edit_oat)
        mMachEdit = findViewById(R.id.e6b_edit_mach)
        addEditField(mTasEdit)
        addEditField(mOatEdit)
        addReadOnlyField(mMachEdit)
        setFragmentContentShown(true)
    }

    override val message: String
        get() = "Speed of sound and hence Mach number varies directly with OAT."

    override fun processInput() {
        try {
            val tas = parseDouble(mTasEdit)
            val oat = parseDouble(mOatEdit)
            val mach = tas / (38.967854 * sqrt(oat + 273.15))
            showDecimalValue(mMachEdit!!, mach, 2)
        } catch (ignored: NumberFormatException) {
            clearEditText(mMachEdit)
        }
    }
}