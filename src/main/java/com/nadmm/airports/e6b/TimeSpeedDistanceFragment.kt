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
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.ListMenuFragment
import com.nadmm.airports.R

class TimeSpeedDistanceFragment : E6bFragmentBase() {
    private var mEdit1: TextInputLayout? = null
    private var mEdit2: TextInputLayout? = null
    private var mEdit3: TextInputLayout? = null
    private var mMode: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_time_speed_distance_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mMode = requireArguments().getLong(ListMenuFragment.MENU_ID)
        setupUi()
        setFragmentContentShown(true)
    }

    override val message: String?
        get() = null

    override fun processInput() {
        try {
            when (mMode) {
                R.id.E6B_TSD_TIME.toLong() -> {
                    val speed = parseDouble(mEdit1)
                    val distance = parseDouble(mEdit2)
                    val time = distance * 60 / speed
                    showValue(mEdit3, time)
                }
                R.id.E6B_TSD_SPEED.toLong() -> {
                    val distance = parseDouble(mEdit1)
                    val time = parseDouble(mEdit2)
                    val speed = distance / (time / 60)
                    showValue(mEdit3, speed)
                }
                R.id.E6B_TSD_DISTANCE.toLong() -> {
                    val speed = parseDouble(mEdit1)
                    val time = parseDouble(mEdit2)
                    val distance = time / 60 * speed
                    showDecimalValue(mEdit3!!, distance)
                }
            }
        } catch (ignored: NumberFormatException) {
            clearEditText(mEdit3)
        }
    }

    private fun setupUi() {
        val mLabel1 = findViewById<TextView>(R.id.e6b_label_value1)
        val mLabel2 = findViewById<TextView>(R.id.e6b_label_value2)
        val mLabel3 = findViewById<TextView>(R.id.e6b_label_value3)
        mEdit1 = findViewById(R.id.e6b_edit_value1)
        mEdit2 = findViewById(R.id.e6b_edit_value2)
        mEdit3 = findViewById(R.id.e6b_edit_value3)
        when (mMode) {
            R.id.E6B_TSD_TIME.toLong() -> {
                mLabel1!!.setText(R.string.gs)
                addEditField(mEdit1, R.string.kts)
                mLabel2!!.setText(R.string.distance_flown)
                addEditField(mEdit2, R.string.nm)
                mLabel3!!.setText(R.string.time_flown)
                addReadOnlyField(mEdit3!!, R.string.min)
            }
            R.id.E6B_TSD_SPEED.toLong() -> {
                mLabel1!!.setText(R.string.distance_flown)
                addEditField(mEdit1, R.string.nm)
                mLabel2!!.setText(R.string.time_flown)
                addEditField(mEdit2, R.string.min)
                mLabel3!!.setText(R.string.gs)
                addReadOnlyField(mEdit3!!, R.string.kts)
            }
            R.id.E6B_TSD_DISTANCE.toLong() -> {
                mLabel1!!.setText(R.string.gs)
                addEditField(mEdit1, R.string.kts)
                mLabel2!!.setText(R.string.time_flown)
                addEditField(mEdit2, R.string.min)
                mLabel3!!.setText(R.string.distance_flown)
                addReadOnlyField(mEdit3!!, R.string.nm)
            }
        }
    }
}