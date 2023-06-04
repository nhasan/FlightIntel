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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.nadmm.airports.R
import kotlin.math.*

class WindTriangleFragment : E6bFragmentBase() {
    private lateinit var mEdit1: TextInputLayout
    private lateinit var mEdit2: TextInputLayout
    private lateinit var mEdit3: TextInputLayout
    private lateinit var mEdit4: TextInputLayout
    private lateinit var mEdit5: TextInputLayout
    private lateinit var mEdit6: TextInputLayout
    private lateinit var mEdit7: TextInputLayout
    private lateinit var mTextMsg: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.e6b_wind_triangle_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()
        setFragmentContentShown(true)
    }

    override val message: String?
        get() = null

    @SuppressLint("SetTextI18n")
    override fun processInput() {
        mTextMsg.text = ""
        try {
            when (menuId) {
                R.id.E6B_WIND_TRIANGLE_WIND -> {
                    val tas = parseDouble(mEdit1)
                    val gs = parseDouble(mEdit2)
                    val hdg = parseDirection(mEdit3)
                    val crs = parseDirection(mEdit4)
                    val ws = sqrt(
                        (tas - gs).pow(2.0)
                                + 4 * tas * gs * sin((hdg - crs) / 2).pow(2.0)
                    ).roundToInt().toDouble()
                    val wdir = crs + atan2(
                        tas * sin(hdg - crs),
                        tas * cos(hdg - crs) - gs
                    )
                    showValue(mEdit5, ws)
                    showDirection(mEdit6, wdir)
                    showDirectionOffset(mEdit7, hdg - crs)
                }
                R.id.E6B_WIND_TRIANGLE_HDG_GS -> {
                    val tas = parseDouble(mEdit1)
                    val ws = parseDouble(mEdit2)
                    val wdir = parseDirection(mEdit3)
                    val crs = parseDirection(mEdit4)
                    val swc = ws / tas * sin(wdir - crs)
                    val hdg = crs + asin(swc)
                    val gs = tas * sqrt(1.0 - swc.pow(2.0)) - ws * cos(wdir - crs)
                    if (gs <= 0 || abs(swc) > 1) {
                        mTextMsg.text = "Course cannot be flown, wind is too strong."
                    }
                    showValue(mEdit5, gs)
                    showDirection(mEdit6, hdg)
                    showDirectionOffset(mEdit7, hdg - crs)
                }
                R.id.E6B_WIND_TRIANGLE_CRS_GS -> {
                    val tas = parseDouble(mEdit1)
                    val ws = parseDouble(mEdit2)
                    val wdir = parseDirection(mEdit3)
                    val hdg = parseDirection(mEdit4)
                    val gs = sqrt(
                        ws.pow(2.0) + tas.pow(2.0)
                            - 2 * ws * tas * cos(hdg - wdir)
                    )
                    val wca = atan2(
                        ws * sin(hdg - wdir),
                        tas - ws * cos(hdg - wdir)
                    )
                    val crs = (hdg + wca) % (2 * Math.PI)
                    showValue(mEdit5, gs)
                    showDirection(mEdit6, crs)
                    showDirectionOffset(mEdit7, hdg - crs)
                }
            }
        } catch (ignored: NumberFormatException) {
            clearEditText(mEdit5)
            clearEditText(mEdit6)
            clearEditText(mEdit7)
        }
    }

    private fun setupUi() {
        val mLabel1 = findViewById<TextView>(R.id.e6b_label_value1)
        val mLabel2 = findViewById<TextView>(R.id.e6b_label_value2)
        val mLabel3 = findViewById<TextView>(R.id.e6b_label_value3)
        val mLabel4 = findViewById<TextView>(R.id.e6b_label_value4)
        val mLabel5 = findViewById<TextView>(R.id.e6b_label_value5)
        val mLabel6 = findViewById<TextView>(R.id.e6b_label_value6)
        mEdit1 = findViewById(R.id.e6b_edit_value1)!!
        mEdit2 = findViewById(R.id.e6b_edit_value2)!!
        mEdit3 = findViewById(R.id.e6b_edit_value3)!!
        mEdit4 = findViewById(R.id.e6b_edit_value4)!!
        mEdit5 = findViewById(R.id.e6b_edit_value5)!!
        mEdit6 = findViewById(R.id.e6b_edit_value6)!!
        mEdit7 = findViewById(R.id.e6b_edit_value7)!!
        mTextMsg = findViewById(R.id.e6b_msg)!!
        addReadOnlyField(mEdit7)
        when (menuId) {
            R.id.E6B_WIND_TRIANGLE_WIND -> {
                // Find wind speed and direction
                mLabel1?.setText(R.string.tas)
                addEditField(mEdit1, R.string.kts)
                mLabel2?.setText(R.string.gs)
                addEditField(mEdit2, R.string.kts)
                mLabel3?.setText(R.string.hdg)
                addEditField(mEdit3, R.string.deg)
                mLabel4?.setText(R.string.crs)
                addEditField(mEdit4, R.string.deg)
                mEdit4.setHint(R.string.deg)
                mLabel5?.setText(R.string.ws)
                addReadOnlyField(mEdit5, R.string.kts)
                mLabel6?.setText(R.string.wdir)
                addReadOnlyField(mEdit6, R.string.deg)
            }
            R.id.E6B_WIND_TRIANGLE_HDG_GS -> {
                // Find HDG and GS
                mLabel1?.setText(R.string.tas)
                addEditField(mEdit1, R.string.kts)
                mLabel2?.setText(R.string.ws)
                addEditField(mEdit2, R.string.kts)
                mLabel3?.setText(R.string.wdir)
                addEditField(mEdit3, R.string.deg)
                mLabel4?.setText(R.string.crs)
                addEditField(mEdit4, R.string.deg)
                mLabel5?.setText(R.string.gs)
                addReadOnlyField(mEdit5, R.string.kts)
                mLabel6?.setText(R.string.hdg)
                addReadOnlyField(mEdit6, R.string.deg)
            }
            R.id.E6B_WIND_TRIANGLE_CRS_GS -> {
                // Find CRS and GS
                mLabel1?.setText(R.string.tas)
                addEditField(mEdit1, R.string.kts)
                mLabel2?.setText(R.string.ws)
                addEditField(mEdit2, R.string.kts)
                mLabel3?.setText(R.string.wdir)
                addEditField(mEdit3, R.string.deg)
                mLabel4?.setText(R.string.hdg)
                addEditField(mEdit4, R.string.deg)
                mLabel5?.setText(R.string.gs)
                addReadOnlyField(mEdit5, R.string.kts)
                mLabel6?.setText(R.string.crs)
                addReadOnlyField(mEdit6, R.string.deg)
            }
        }
    }
}