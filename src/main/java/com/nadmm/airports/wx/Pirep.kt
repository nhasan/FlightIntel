/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.wx

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.xml.sax.Attributes

@Parcelize
@Serializable
class PirepSkyCondition(
    var skyCover: String,
    var baseFeetMSL: Int,
    var topFeetMSL: Int
) : Parcelable {
    companion object {
        fun of(attributes: Attributes): PirepSkyCondition {
            val skyCover = attributes.getValue("sky_cover") ?: "SKM"
            val baseFeetMSL = attributes.getValue("cloud_base_ft_msl")?.toInt() ?: Int.MAX_VALUE
            val topFeetMSL = attributes.getValue("cloud_top_ft_msl")?.toInt() ?: Int.MAX_VALUE
            return PirepSkyCondition(skyCover, baseFeetMSL, topFeetMSL)
        }
    }
}

@Parcelize
@Serializable
class PirepTurbulenceCondition(
    var type: String,
    var intensity: String,
    var frequency: String,
    var baseFeetMSL: Int,
    var topFeetMSL: Int
) : Parcelable {
    companion object {
        fun of(attributes: Attributes): PirepTurbulenceCondition {
            val type = attributes.getValue("turbulence_type") ?: ""
            val intensity = attributes.getValue("turbulence_intensity") ?: ""
            val frequency = attributes.getValue("turbulence_freq") ?: ""
            val baseFeetMSL = attributes.getValue("turbulence_base_ft_msl")?.toInt() ?: Int.MAX_VALUE
            val topFeetMSL = attributes.getValue("turbulence_top_ft_msl")?.toInt() ?: Int.MAX_VALUE
            return PirepTurbulenceCondition(type, intensity, frequency, baseFeetMSL, topFeetMSL)
        }
    }
}

@Parcelize
@Serializable
class PirepIcingCondition(
    var type: String,
    var intensity: String,
    var baseFeetMSL: Int,
    var topFeetMSL: Int
) : Parcelable {
    companion object {
        fun of(attributes: Attributes): PirepIcingCondition {
            val type = attributes.getValue("icing_type") ?: ""
            val intensity = attributes.getValue("icing_intensity") ?: ""
            val baseFeetMSL = attributes.getValue("icing_base_ft_msl")?.toInt() ?: Int.MAX_VALUE
            val topFeetMSL = attributes.getValue("icing_top_ft_msl")?.toInt() ?: Int.MAX_VALUE
            return PirepIcingCondition(type, intensity, baseFeetMSL, topFeetMSL)
        }
    }
}

enum class PirepFlags(var description: String) {
    NoTimeStamp("No timestamp"),
    AglIndicated("AGL indicated"),
    BadLocation("Bad location");

    override fun toString(): String = description
}

@Parcelize
@Serializable
class PirepEntry(
    var isValid: Boolean = false,
    var stationId: String = "",
    var receiptTime: Long = Long.MAX_VALUE,
    var observationTime: Long = Long.MAX_VALUE,
    var reportType: String = "",
    var rawText: String = "",
    var aircraftRef: String = "",
    var latitude: Float = 0f,
    var longitude: Float = 0f,
    var distanceNM: Int = 0,
    var bearing: Int = 0,
    var altitudeFeetMsl: Int = Int.MAX_VALUE,
    var visibilitySM: Int = Int.MAX_VALUE,
    var tempCelsius: Int = Int.MAX_VALUE,
    var windDirDegrees: Int = Int.MAX_VALUE,
    var windSpeedKnots: Int = Int.MAX_VALUE,
    var vertGustKnots: Int = Int.MAX_VALUE,
    var flags: MutableList<PirepFlags> = mutableListOf(),
    var skyConditions: MutableList<PirepSkyCondition> = mutableListOf(),
    var wxList: MutableList<WxSymbol> = mutableListOf(),
    var turbulenceConditions: MutableList<PirepTurbulenceCondition> = mutableListOf(),
    var icingConditions: MutableList<PirepIcingCondition> = mutableListOf(),
    var remarks: MutableList<String> = mutableListOf()
) : Parcelable

@Parcelize
@Serializable
class Pirep(
    var fetchTime: Long = 0L,
    var stationId: String = "",
    var entries: MutableList<PirepEntry> = mutableListOf()
) : Parcelable
