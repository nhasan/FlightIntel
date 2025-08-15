/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.nadmm.airports.R
import com.nadmm.airports.wx.Metar
import com.nadmm.airports.wx.SkyCondition
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

object WxUtils {
    private const val FLIGHT_CATEGORY_VFR = "VFR"
    private const val FLIGHT_CATEGORY_MVFR = "MVFR"
    private const val FLIGHT_CATEGORY_IFR = "IFR"
    private const val FLIGHT_CATEGORY_LIFR = "LIFR"
    const val FLIGHT_CATEGORY_UNKN = "UNKN"

    const val ISA_PRESSURE_HG = 29.9212
    const val ISA_PRESSURE_MBAR = 1013.25
    const val HG_TO_MBAR = ISA_PRESSURE_MBAR/ISA_PRESSURE_HG
    const val TROPOPAUSE_FT = 36089

    private fun getFlightCategoryColor(flightCategory: String): Int {
        return when (flightCategory) {
            FLIGHT_CATEGORY_VFR -> Color.argb(255, 0, 160, 32)
            FLIGHT_CATEGORY_MVFR -> Color.argb(255, 0, 144, 224)
            FLIGHT_CATEGORY_IFR -> Color.argb(255, 192, 32, 0)
            FLIGHT_CATEGORY_LIFR -> Color.argb(255, 200, 0, 160)
            else -> Color.GRAY
        }
    }

    @JvmStatic
    fun showColorizedDrawable(tv: TextView?, flightCategory: String, resid: Int) {
        tv?.let {
            val color = getFlightCategoryColor(flightCategory)
            UiUtils.setTintedTextViewDrawable(it, resid, ColorStateList.valueOf(color))
        }
    }

    @JvmStatic
    fun setFlightCategoryDrawable(tv: TextView, flightCategory: String) {
        var d = UiUtils.getDrawableFromCache(flightCategory)
        if (d == null) {
            d = when (flightCategory) {
                FLIGHT_CATEGORY_VFR -> AppCompatResources.getDrawable(tv.context, R.drawable.wx_vfr_32)
                FLIGHT_CATEGORY_MVFR -> AppCompatResources.getDrawable(tv.context, R.drawable.wx_mvfr_32)
                FLIGHT_CATEGORY_IFR -> AppCompatResources.getDrawable(tv.context, R.drawable.wx_ifr_32)
                FLIGHT_CATEGORY_LIFR -> AppCompatResources.getDrawable(tv.context, R.drawable.wx_lifr_32)
                else -> null
            }
        }
        d?.let {
            UiUtils.putDrawableIntoCache(flightCategory, it)
            UiUtils.setTextViewDrawable(tv, it)
        }
    }

    fun setColorizedCeilingDrawable(tv: TextView?, metar: Metar) {
        val sky = metar.skyConditions[metar.skyConditions.size - 1]
        showColorizedDrawable(tv, metar.flightCategory, sky.drawable)
    }

    private fun getSkycoverDrawable(context: Context?, metar: Metar): Drawable? {
        val sky = metar.skyConditions[metar.skyConditions.size - 1]
        val color = getFlightCategoryColor(metar.flightCategory)
        return UiUtils.getTintedDrawable(context, sky.drawable, ColorStateList.valueOf(color))
    }

    @JvmStatic
    fun getWindBarbDrawable(context: Context?, metar: Metar, declination: Float): Drawable? {
        var d: Drawable? = null
        if (isWindAvailable(metar)) {
            val dir = GeoUtils.applyDeclination(metar.windDirDegrees, declination)
            val key = "Wind-${metar.flightCategory}-${metar.windSpeedKnots}-$dir"
            d = UiUtils.getDrawableFromCache(key)
            if (d == null) {
                val resid: Int = if (metar.windSpeedKnots >= 48) {
                    R.drawable.windbarb50_24
                } else if (metar.windSpeedKnots >= 43) {
                    R.drawable.windbarb45_24
                } else if (metar.windSpeedKnots >= 38) {
                    R.drawable.windbarb40_24
                } else if (metar.windSpeedKnots >= 33) {
                    R.drawable.windbarb35_24
                } else if (metar.windSpeedKnots >= 28) {
                    R.drawable.windbarb30_24
                } else if (metar.windSpeedKnots >= 23) {
                    R.drawable.windbarb25_24
                } else if (metar.windSpeedKnots >= 18) {
                    R.drawable.windbarb20_24
                } else if (metar.windSpeedKnots >= 13) {
                    R.drawable.windbarb15_24
                } else if (metar.windSpeedKnots >= 8) {
                    R.drawable.windbarb10_24
                } else if (metar.windSpeedKnots >= 3) {
                    R.drawable.windbarb5_24
                } else {
                    R.drawable.windbarb0_24
                }
                d = UiUtils.getRotatedDrawable(context!!, resid, dir.toFloat())
                val color = getFlightCategoryColor(metar.flightCategory)
                d.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                    color, BlendModeCompat.SRC_ATOP)
                UiUtils.putDrawableIntoCache(key, d)
            }
        }
        return d
    }

    @JvmStatic
    fun getErrorDrawable(context: Context): Drawable? {
        val resid = R.drawable.ic_outline_error_outline_24
        val key = resid.toString()
        var d = UiUtils.getDrawableFromCache(key)
        if (d == null) {
            d = AppCompatResources.getDrawable(context, resid)
            UiUtils.putDrawableIntoCache(key, d)
        }
        return d
    }

    @JvmStatic
    fun setColorizedWxDrawable(tv: TextView, metar: Metar?, declination: Float) {
        if (metar != null) {
            val context = tv.context
            if (metar.isValid) {
                val d1 = getSkycoverDrawable(context, metar)
                d1?.let {
                    val d2 = getWindBarbDrawable(tv.context, metar, declination)
                    val result = UiUtils.combineDrawables(context, d1, d2, 4)
                    UiUtils.setTextViewDrawable(tv, result)
                }
            } else {
                val d = getErrorDrawable(context)
                UiUtils.setTextViewDrawable(tv, d)
            }
        } else {
            UiUtils.setTextViewDrawable(tv, null)
        }
    }

    fun isWindAvailable(metar: Metar): Boolean {
        return metar.windDirDegrees > 0 && metar.windDirDegrees < Int.MAX_VALUE
                && metar.windSpeedKnots > 0 && metar.windSpeedKnots < Int.MAX_VALUE
    }

    @JvmStatic
    fun celsiusToFahrenheit(tempCelsius: Float): Float {
        return tempCelsius * 9 / 5 + 32
    }

    @JvmStatic
    fun fahrenheitToCelsius(tempFahrenheit: Float): Float {
        return (tempFahrenheit - 32) * 5 / 9
    }

    @JvmStatic
    fun celsiusToKelvin(tempCelsius: Float): Float {
        return (tempCelsius + 273.15).toFloat()
    }

    @JvmStatic
    fun kelvinToCelsius(tempKelvin: Float): Float {
        return (tempKelvin - 273.15).toFloat()
    }

    @JvmStatic
    fun kelvinToRankine(tempKelvin: Float): Float {
        return (celsiusToFahrenheit(kelvinToCelsius(tempKelvin)) + 459.69).toFloat()
    }

    @JvmStatic
    fun hgToMillibar(altimHg: Float): Float {
        return (33.8639 * altimHg).toFloat()
    }

    private fun getVirtualTemperature(
        tempCelsius: Float, dewpointCelsius: Float,
        stationPressureHg: Float
    ): Float {
        val tempKelvin = celsiusToKelvin(tempCelsius)
        val eMb = getVaporPressure(dewpointCelsius)
        val pMb = hgToMillibar(stationPressureHg)
        return (tempKelvin / (1 - eMb / pMb * (1 - 0.622))).toFloat()
    }

    private fun getVaporPressure(tempCelsius: Float): Float = // Vapor pressure is in mb or hPa
        (6.1094 * exp(17.625 * tempCelsius / (tempCelsius + 243.04))).toFloat()

    private fun getStationPressure(altimHg: Float, elevMeters: Float): Float {
        // Station pressure is in inHg
        return (altimHg * ((288 - 0.0065 * elevMeters) / 288).pow(5.2561)).toFloat()
    }

    @JvmStatic
    fun getRelativeHumidity(metar: Metar): Float {
        return getRelativeHumidity(metar.tempCelsius, metar.dewpointCelsius)
    }

    private fun getRelativeHumidity(tempCelsius: Float, dewpointCelsius: Float): Float {
        val e = getVaporPressure(dewpointCelsius)
        val es = getVaporPressure(tempCelsius)
        return e / es * 100
    }

    @JvmStatic
    fun getPressureAltitude(metar: Metar): Int {
        return getPressureAltitude(metar.altimeterHg, metar.stationElevationMeters)
    }

    private fun getPressureAltitude(altimHg: Float, elevMeters: Float): Int {
        val pMb = hgToMillibar(getStationPressure(altimHg, elevMeters))
        return ((1 - (pMb / 1013.25).pow(0.190284)) * 145366.45).roundToInt()
    }

    @JvmStatic
    fun getDensityAltitude(metar: Metar): Int {
        return getDensityAltitude(
            metar.tempCelsius, metar.dewpointCelsius, metar.altimeterHg,
            metar.stationElevationMeters
        )
    }

    private fun getDensityAltitude(
        tempCelsius: Float, dewpointCelsius: Float,
        altimHg: Float, elevMeters: Float
    ): Int {
        val stationPressureHg = getStationPressure(altimHg, elevMeters)
        val tvKelvin = getVirtualTemperature(tempCelsius, dewpointCelsius, stationPressureHg)
        val tvRankine = kelvinToRankine(tvKelvin)
        return (145366 * (1 - (17.326 * stationPressureHg / tvRankine).pow(0.235))).roundToInt()
    }

    @JvmStatic
    fun getHeadWindComponent(windSpeed: Double, windDir: Double, d: Double): Int {
        return (windSpeed * cos(Math.toRadians(windDir - d))).roundToInt()
    }

    @JvmStatic
    fun getCrossWindComponent(windSpeed: Double, windDir: Double, d: Double): Int {
        return (windSpeed * sin(Math.toRadians(windDir - d))).roundToInt()
    }

    @JvmStatic
    fun getCeiling(layers: List<SkyCondition>): SkyCondition {
        return try {
            // Ceiling is defined as the lowest cloud layer reported as broken or overcast;
            // or the vertical visibility into an indefinite ceiling
            layers.first { layer -> listOf("BKN", "OVC", "OVX").contains(layer.skyCover) }
        } catch (_: NoSuchElementException) {
            SkyCondition.of("NSC", 12000)
        }
    }

    @JvmStatic
    fun computeFlightCategory(skyConditions: List<SkyCondition>, visibilitySM: Float): String {
        val ceiling = getCeiling(skyConditions).cloudBaseAGL
        return when {
            (ceiling < 500 || visibilitySM < 1.0) -> "LIFR"
            (ceiling < 1000 || visibilitySM < 3.0) -> "IFR"
            (ceiling <= 3000 || visibilitySM <= 5.0) -> "MVFR"
            else -> "VFR"
        }
    }

    @JvmStatic
    fun decodeTurbulenceIntensity(intensity: Int): String {
        return when (intensity) {
            1 -> "Light"
            2, 4 -> "Occasional, Moderate"
            3, 5 -> "Frequent, Moderate"
            6, 8 -> "Occasional, Severe"
            7, 9 -> "Frequent, Severe"
            else -> "None"
        }
    }

    @JvmStatic
    fun decodeIcingIntensity(icing: Int): String {
        return when (icing) {
            1, 2, 3 -> "Light icing"
            4, 5, 6 -> "Moderate icing"
            7, 8, 9 -> "Severe icing"
            else -> "No icing"
        }
    }
}