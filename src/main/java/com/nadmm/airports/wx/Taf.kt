/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2023 Nadeem Hasan <nhasan@nadmm.com>
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

import java.io.Serializable

class Taf : Serializable {
    class TurbulenceCondition : Serializable {
        @JvmField
        var intensity: Int = Int.MAX_VALUE

        @JvmField
        var minAltitudeFeetAGL: Int = Int.MAX_VALUE

        @JvmField
        var maxAltitudeFeetAGL: Int = Int.MAX_VALUE

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    class IcingCondition : Serializable {
        @JvmField
        var intensity: Int = Int.MAX_VALUE

        @JvmField
        var minAltitudeFeetAGL: Int = Int.MAX_VALUE

        @JvmField
        var maxAltitudeFeetAGL: Int = Int.MAX_VALUE

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    class Temperature : Serializable {
        var validTime: Long = Long.MAX_VALUE
        var surfaceTempCentigrade: Float = Float.MAX_VALUE
        var maxTempCentigrade: Float = Float.MAX_VALUE
        var minTempCentigrade: Float = Float.MAX_VALUE

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    class Forecast : Serializable {
        @JvmField
        var changeIndicator: String? = null
        @JvmField
        var timeFrom: Long = 0
        @JvmField
        var timeTo: Long = 0
        @JvmField
        var timeBecoming: Long = 0
        @JvmField
        var probability: Int = Int.MAX_VALUE
        @JvmField
        var windDirDegrees: Int = Int.MAX_VALUE
        @JvmField
        var windSpeedKnots: Int = Int.MAX_VALUE
        @JvmField
        var windGustKnots: Int = Int.MAX_VALUE
        @JvmField
        var windShearDirDegrees: Int = Int.MAX_VALUE
        @JvmField
        var windShearSpeedKnots: Int = Int.MAX_VALUE
        @JvmField
        var windShearHeightFeetAGL: Int = Int.MAX_VALUE
        @JvmField
        var visibilitySM: Float = Float.MAX_VALUE
        @JvmField
        var altimeterHg: Float = Float.MAX_VALUE
        @JvmField
        var vertVisibilityFeet: Int = Int.MAX_VALUE
        @JvmField
        var wxList: ArrayList<WxSymbol> = ArrayList()
        @JvmField
        var skyConditions: ArrayList<SkyCondition> = ArrayList()
        @JvmField
        var turbulenceConditions: ArrayList<TurbulenceCondition> = ArrayList()
        @JvmField
        var icingConditions: ArrayList<IcingCondition> = ArrayList()
        var temperatures: ArrayList<Temperature> = ArrayList()

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    @JvmField
    var isValid = false
    var stationId: String? = null
    @JvmField
    var rawText: String? = null
    @JvmField
    var fetchTime: Long = 0
    @JvmField
    var issueTime: Long = 0
    var bulletinTime: Long = 0
    @JvmField
    var validTimeFrom: Long = 0
    @JvmField
    var validTimeTo: Long = 0
    var stationElevationMeters: Float = Float.MAX_VALUE

    @JvmField
    var remarks: String? = null
    @JvmField
    var forecasts: ArrayList<Forecast> = ArrayList()

    companion object {
        private const val serialVersionUID = 1L
    }
}