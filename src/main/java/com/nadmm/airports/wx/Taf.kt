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

@Parcelize
@Serializable
class TurbulenceCondition(
    var intensity: Int = Int.MAX_VALUE,
    var minAltitudeFeetAGL: Int = Int.MAX_VALUE,
    var maxAltitudeFeetAGL: Int = Int.MAX_VALUE
) : Parcelable

@Parcelize
@Serializable
class IcingCondition(
    var intensity: Int = Int.MAX_VALUE,
    var minAltitudeFeetAGL: Int = Int.MAX_VALUE,
    var maxAltitudeFeetAGL: Int = Int.MAX_VALUE
) : Parcelable

@Parcelize
@Serializable
class Temperature(
    var validTime: Long = Long.MAX_VALUE,
    var surfaceTempCentigrade: Float = Float.MAX_VALUE,
    var maxTempCentigrade: Float = Float.MAX_VALUE,
    var minTempCentigrade: Float = Float.MAX_VALUE
) : Parcelable

@Parcelize
@Serializable
class Forecast(
    var changeIndicator: String? = null,
    var timeFrom: Long = 0,
    var timeTo: Long = 0,
    var timeBecoming: Long = 0,
    var probability: Int = Int.MAX_VALUE,
    var windDirDegrees: Int = Int.MAX_VALUE,
    var windSpeedKnots: Int = Int.MAX_VALUE,
    var windGustKnots: Int = Int.MAX_VALUE,
    var windShearDirDegrees: Int = Int.MAX_VALUE,
    var windShearSpeedKnots: Int = Int.MAX_VALUE,
    var windShearHeightFeetAGL: Int = Int.MAX_VALUE,
    var visibilitySM: Float = Float.MAX_VALUE,
    var altimeterHg: Float = Float.MAX_VALUE,
    var vertVisibilityFeet: Int = Int.MAX_VALUE,
    var wxList: ArrayList<WxSymbol> = ArrayList(),
    var skyConditions: ArrayList<SkyCondition> = ArrayList(),
    var turbulenceConditions: ArrayList<TurbulenceCondition> = ArrayList(),
    var icingConditions: ArrayList<IcingCondition> = ArrayList(),
    var temperatures: ArrayList<Temperature> = ArrayList()
) : Parcelable

@Parcelize
@Serializable
class Taf (
    var isValid: Boolean = false,
    var stationId: String? = null,
    var rawText: String? = null,
    var fetchTime: Long = 0,
    var issueTime: Long = 0,
    var bulletinTime: Long = 0,
    var validTimeFrom: Long = 0,
    var validTimeTo: Long = 0,
    var stationElevationMeters: Float = Float.MAX_VALUE,
    var remarks: String? = null,
    var forecasts: ArrayList<Forecast> = ArrayList()
) : Parcelable
