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

import android.os.Parcelable
import com.nadmm.airports.utils.WxUtils
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

enum class MetarFlag(var description: String) {
    CORRECTED("Corrected METAR"),
    AUTOMATED_STATION("Automated station"),
    MAINTENANCE_INDICATOR_ON("Station needs maintenance"),
    PRESENT_WEATHER_SENSOR_OFF("Present weather sensor is not operating"),
    LIGHTNING_SENSOR_OFF("Lightning detection sensor is not operating"),
    RAIN_SENSOR_OFF("Rain sensor is not operating"),
    FREEZING_RAIN_SENSOR_OFF("Freezing rain sensor is not operating"),
    NO_SIGNAL("No signal from station");

    override fun toString(): String = description
}

enum class WeatherPhenomenon(var description: String) {
    PRESSURE_RISING_RAPIDLY("Pressure rising rapidly"),
    PRESSURE_FALLING_RAPIDLY("Pressure falling rapidly"),
    SNOW_INCREASING_RAPIDLY("Snow increasing rapidly"),
    WIND_SHIFT("Wind shift"),
    FRONTAL_PASSAGE("Frontal passage"),
    LIGHTNING("Lightning detected");

    override fun toString(): String = description
}

@Parcelize
@Serializable
data class Metar(
    var stationId: String? = null,
    var fetchTime: Long = 0L,
    var isValid: Boolean = false,
    var rawText: String? = null,
    var observationTime: Long = 0L,
    var tempCelsius: Float = Float.MAX_VALUE,
    var dewpointCelsius: Float = Float.MAX_VALUE,
    var windDirDegrees: Int = Int.MAX_VALUE,
    var windSpeedKnots: Int = Int.MAX_VALUE,
    var windGustKnots: Int = Int.MAX_VALUE,
    var windPeakKnots: Int = Int.MAX_VALUE,
    var visibilitySM: Float = Float.MAX_VALUE,
    var altimeterHg: Float = Float.MAX_VALUE,
    var seaLevelPressureMb: Float = Float.MAX_VALUE,
    var wxList: MutableList<WxSymbol> = mutableListOf(),
    var skyConditions: MutableList<SkyCondition> = mutableListOf(),
    var flightCategory: String = WxUtils.FLIGHT_CATEGORY_UNKN,
    var pressureTend3HrMb: Float = Float.MAX_VALUE,
    var maxTempCelsiusLast6Hours: Float = Float.MAX_VALUE,
    var minTempCelsiusLast6Hours: Float = Float.MAX_VALUE,
    var maxTempCelsiusLast24Hours: Float = Float.MAX_VALUE,
    var minTempCelsiusLast24Hours: Float = Float.MAX_VALUE,
    var precipInches: Float = Float.MAX_VALUE,
    var precip3HrInches: Float = Float.MAX_VALUE,
    var precip6HrInches: Float = Float.MAX_VALUE,
    var precip24HrInches: Float = Float.MAX_VALUE,
    var snowInches: Float = Float.MAX_VALUE,
    var vertVisibilityFeet: Int = Int.MAX_VALUE,
    var metarType: String? = null,
    var stationElevationMeters: Float = Float.MAX_VALUE,
    var flags: MutableSet<MetarFlag> = mutableSetOf(),
    var weatherPhenomena: MutableSet<WeatherPhenomenon> = mutableSetOf()
) : Parcelable
