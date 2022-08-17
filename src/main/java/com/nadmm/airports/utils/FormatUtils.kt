/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2022 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.utils

import com.nadmm.airports.utils.WxUtils.celsiusToFahrenheit
import com.nadmm.airports.utils.WxUtils.hgToMillibar
import java.text.DecimalFormat
import java.util.*

object FormatUtils {
    private val sFeetFormat: DecimalFormat = DecimalFormat()
    private val sFeetFormatMsl: DecimalFormat = DecimalFormat()
    private val sFeetFormatAgl: DecimalFormat = DecimalFormat()
    private val sNumberFormat: DecimalFormat = DecimalFormat()
    private val sFreqFormat: DecimalFormat = DecimalFormat()
    private val sSMFormat: DecimalFormat = DecimalFormat()
    private val sNMFormat: DecimalFormat = DecimalFormat()

    init {
        sFeetFormat.applyPattern("#,##0.# ft")
        sFeetFormatMsl.applyPattern("#,##0.# ft MSL")
        sFeetFormatAgl.applyPattern("#,##0.# ft AGL")
        sNumberFormat.applyPattern("#,##0.##")
        sFreqFormat.applyPattern("##0.000")
        sSMFormat.applyPattern("#0.## SM")
        sNMFormat.applyPattern("#0.# NM")
    }

    @JvmStatic
    fun formatFeet(value: Float): String {
        return sFeetFormat.format(value.toDouble())
    }

    @JvmStatic
    fun formatFeetMsl(value: Float): String {
        return sFeetFormatMsl.format(value.toDouble())
    }

    @JvmStatic
    fun formatFeetRangeMsl(base: Int, top: Int): String {
        return if (base == 0 && top < Int.MAX_VALUE) {
            String.format("Surface to %s ft MSL", sNumberFormat.format(top.toLong()))
        } else if (base < Int.MAX_VALUE && top < Int.MAX_VALUE) {
            String.format("%s to %s ft MSL", sNumberFormat.format(base.toLong()),
                sNumberFormat.format(top.toLong()))
        } else if (base < Int.MAX_VALUE) {
            String.format("%s ft MSL and above", sNumberFormat.format(base.toLong()))
        } else if (top < Int.MAX_VALUE) {
            String.format("%s ft MSL and below", sNumberFormat.format(top.toLong()))
        } else {
            ""
        }
    }

    @JvmStatic
    fun formatFeetAgl(value: Float): String {
        return sFeetFormatAgl.format(value.toDouble())
    }

    @JvmStatic
    fun formatFeetRangeAgl(base: Int, top: Int): String {
        return if (base == 0 && top < Int.MAX_VALUE) {
            String.format("Surface to %s ft AGL", sNumberFormat.format(top.toLong()))
        } else if (base < Int.MAX_VALUE && top < Int.MAX_VALUE) {
            String.format("%s to %s ft AGL", sNumberFormat.format(base.toLong()),
                sNumberFormat.format(top.toLong()))
        } else if (base < Int.MAX_VALUE) {
            String.format("%s ft AGL and above", sNumberFormat.format(top.toLong()))
        } else if (top < Int.MAX_VALUE) {
            String.format("%s ft AGL and below", sNumberFormat.format(top.toLong()))
        } else {
            ""
        }
    }

    @JvmStatic
    fun formatNumber(value: Float): String {
        return sNumberFormat.format(value.toDouble())
    }

    fun formatFreq(value: Float): String {
        return sFreqFormat.format(value.toDouble())
    }

    @JvmStatic
    fun formatStatuteMiles(value: Float): String {
        return sSMFormat.format(value.toDouble())
    }

    fun formatNauticalMiles(value: Float): String {
        return sNMFormat.format(value.toDouble())
    }

    @JvmStatic
    fun formatTemperature(tempC: Float): String {
        return String.format(Locale.US, "%s (%s)",
            formatTemperatureC(tempC), formatTemperatureF(tempC))
    }

    private fun formatTemperatureC(tempC: Float): String {
        return String.format(Locale.US, "%.1f\u00B0C", tempC)
    }

    fun formatTemperatureF(tempC: Float): String {
        return String.format(Locale.US, "%.0f\u00B0F", celsiusToFahrenheit(tempC))
    }

    @JvmStatic
    fun formatAltimeter(altimeterHg: Float): String {
        val altimeterMb = hgToMillibar(altimeterHg)
        return String.format(Locale.US, "%.2f\" Hg (%s mb)",
            altimeterHg, formatNumber(altimeterMb)
        )
    }

    fun formatAltimeterHg(altimeterHg: Float): String {
        return String.format(Locale.US, "%.2f\" Hg", altimeterHg)
    }

    fun formatDegrees(degrees: Float): String {
        return String.format(Locale.US, "%.02f\u00B0", degrees)
    }

    @JvmStatic
    fun formatDegrees(degrees: Int): String {
        return String.format(Locale.US, "%03d\u00B0", degrees)
    }

    fun formatRunway(width: Int, length: Int): String {
        return String.format(Locale.US, "%s x %s",
            formatFeet(length.toFloat()), formatFeet(width.toFloat())
        )
    }

}