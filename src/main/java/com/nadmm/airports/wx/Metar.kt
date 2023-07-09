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
package com.nadmm.airports.wx

import com.nadmm.airports.utils.WxUtils
import java.io.Serializable
import java.util.EnumSet

class Metar : Serializable {
    enum class Flags {
        Corrected {
            override fun toString(): String {
                return "Corrected METAR"
            }
        },
        Auto {
            override fun toString(): String {
                return "Automated station"
            }
        },
        MaintenanceIndicatorOn {
            override fun toString(): String {
                return "Station needs maintenance"
            }
        },
        PresentWeatherSensorOff {
            override fun toString(): String {
                return "Present weather sensor is not operating"
            }
        },
        LightningSensorOff {
            override fun toString(): String {
                return "Lightning detection sensor is not operating"
            }
        },
        RainSensorOff {
            override fun toString(): String {
                return "Rain sensor is not operating"
            }
        },
        FreezingRainSensorOff {
            override fun toString(): String {
                return "Freezing rain sensor is not operating"
            }
        },
        NoSignal {
            override fun toString(): String {
                return "No signal from station"
            }
        }
    }

    @JvmField
    var isValid = false

    @JvmField
    var stationId: String? = null

    @JvmField
    var rawText: String? = null

    @JvmField
    var observationTime: Long = 0

    @JvmField
    var fetchTime: Long = 0

    @JvmField
    var tempCelsius: Float = Float.MAX_VALUE

    @JvmField
    var dewpointCelsius: Float = Float.MAX_VALUE

    @JvmField
    var windDirDegrees: Int = Int.MAX_VALUE

    @JvmField
    var windSpeedKnots: Int = Int.MAX_VALUE

    @JvmField
    var windGustKnots: Int = Int.MAX_VALUE

    @JvmField
    var windPeakKnots: Int = Int.MAX_VALUE

    @JvmField
    var visibilitySM: Float = Float.MAX_VALUE

    @JvmField
    var altimeterHg: Float = Float.MAX_VALUE

    @JvmField
    var seaLevelPressureMb: Float = Float.MAX_VALUE

    @JvmField
    var wxList: ArrayList<WxSymbol> = ArrayList()

    @JvmField
    var skyConditions: ArrayList<SkyCondition> = ArrayList()

    @JvmField
    var flightCategory: String = WxUtils.FLIGHT_CATEGORY_UNKN

    @JvmField
    var pressureTend3HrMb: Float = Float.MAX_VALUE

    @JvmField
    var maxTemp6HrCentigrade: Float = Float.MAX_VALUE

    @JvmField
    var minTemp6HrCentigrade: Float = Float.MAX_VALUE

    @JvmField
    var maxTemp24HrCentigrade: Float = Float.MAX_VALUE

    @JvmField
    var minTemp24HrCentigrade: Float = Float.MAX_VALUE

    @JvmField
    var precipInches: Float = Float.MAX_VALUE

    @JvmField
    var precip3HrInches: Float = Float.MAX_VALUE

    @JvmField
    var precip6HrInches: Float = Float.MAX_VALUE

    @JvmField
    var precip24HrInches: Float = Float.MAX_VALUE

    @JvmField
    var snowInches: Float = Float.MAX_VALUE

    @JvmField
    var vertVisibilityFeet: Int = Int.MAX_VALUE

    @JvmField
    var metarType: String? = null

    @JvmField
    var stationElevationMeters: Float = Float.MAX_VALUE

    @JvmField
    var flags: EnumSet<Flags> = EnumSet.noneOf(Flags::class.java)

    @JvmField
    var presrr: Boolean = false

    @JvmField
    var presfr: Boolean = false

    @JvmField
    var snincr: Boolean = false

    @JvmField
    var wshft: Boolean = false

    @JvmField
    var fropa: Boolean = false

    @JvmField
    var ltg: Boolean = false

    companion object {
        const val serialVersionUID = 3338578580544822881L
    }
}