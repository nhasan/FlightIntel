/*
 * FlightIntel for Pilots
 *
 * Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** One altitude of a winds aloft forecast; nulls where the forecast omits them. */
@Serializable
data class WindsAloftRow(
    val altitudeFt: Int = 0,
    // Null for light and variable, or when below the altitudes forecast
    val directionDeg: Int? = null,
    val speedKt: Int? = null,
    val temperatureC: Int? = null,
    val lightAndVariable: Boolean = false,
    // False below station elevation + 1,500 ft, where no wind is forecast
    val hasWind: Boolean = true
)

/** The forecast for one time, as epoch millis UTC. */
@Serializable
data class WindsAloftForecast(
    val timeMillis: Long = 0,
    val hoursAhead: Int = 0,
    val rows: List<WindsAloftRow> = emptyList()
)

@Serializable
data class WindsAloft(
    val stationId: String = "",
    val fetchTime: Long = 0,
    val forecasts: List<WindsAloftForecast> = emptyList()
)

/**
 * Builds winds aloft tables, like the retired NWS FB forecasts, from model
 * winds and temperatures at pressure levels, as Open-Meteo returns them.
 */
object WindsAloftBuilder {
    /** The altitudes of the FB forecasts, in feet MSL. */
    val ALTITUDES_FT = listOf(3000, 6000, 9000, 12000, 18000, 24000, 30000, 34000, 39000)

    /** Pressure levels to request, from near the surface to above FL390. */
    val PRESSURE_LEVELS_HPA = listOf(
        1000, 975, 950, 925, 900, 850, 800, 750, 700, 650,
        600, 550, 500, 450, 400, 350, 300, 250, 200, 150
    )

    /** Hours ahead of now to forecast, as the FB forecasts did, plus now. */
    val HOURS_AHEAD = listOf(0, 6, 12, 24)

    // FB conventions: no wind within 1,500 ft of the station, no temperature
    // at 3,000 ft or within 2,500 ft of it, and light and variable below 5 kt
    private const val NO_WIND_ABOVE_STATION_FT = 1500
    private const val NO_TEMP_ABOVE_STATION_FT = 2500
    private const val NO_TEMP_ALTITUDE_FT = 3000
    private const val LIGHT_AND_VARIABLE_KT = 5

    private const val FEET_PER_METER = 3.28084

    /** Wind and temperature at one height, with wind as components in knots. */
    data class Level(val heightFt: Double, val u: Double, val v: Double, val temperatureC: Double)

    /** The hourly variables to request, for the query. */
    fun hourlyVariables(): String = PRESSURE_LEVELS_HPA.joinToString(",") { p ->
        "temperature_${p}hPa,wind_speed_${p}hPa,wind_direction_${p}hPa,geopotential_height_${p}hPa"
    }

    /**
     * Builds the forecasts for [hoursAhead] of [nowMillis] from an Open-Meteo
     * response with the [hourlyVariables], winds in knots and times in UTC.
     */
    fun build(
        json: String, stationId: String, elevationFt: Int,
        nowMillis: Long, hoursAhead: List<Int> = HOURS_AHEAD
    ): WindsAloft {
        val hourly = Json.parseToJsonElement(json).jsonObject["hourly"]?.jsonObject
            ?: return WindsAloft(stationId, nowMillis)
        val times = hourly["time"]?.jsonArray?.map { parseTime(it.jsonPrimitive.contentOrNull) }
            ?: return WindsAloft(stationId, nowMillis)

        val forecasts = hoursAhead.mapNotNull { ahead ->
            val target = nowMillis + ahead * HOUR_MILLIS
            // The first hour at or after the target, so "now" is this hour
            val index = times.indexOfFirst { it != null && it >= target - HOUR_MILLIS + 1 }
            if (index < 0) return@mapNotNull null
            val levels = levelsAt(hourly, index)
            if (levels.isEmpty()) return@mapNotNull null
            WindsAloftForecast(times[index]!!, ahead, rows(levels, elevationFt))
        }
        return WindsAloft(stationId, nowMillis, forecasts)
    }

    /** The table for one time: a row for each FB altitude. */
    fun rows(levels: List<Level>, elevationFt: Int): List<WindsAloftRow> =
        ALTITUDES_FT.map { altitude ->
            val level = interpolate(levels, altitude.toDouble())
            val hasWind = altitude >= elevationFt + NO_WIND_ABOVE_STATION_FT
            val hasTemp = altitude > NO_TEMP_ALTITUDE_FT &&
                    altitude >= elevationFt + NO_TEMP_ABOVE_STATION_FT
            if (level == null || !hasWind) {
                return@map WindsAloftRow(altitude, hasWind = false,
                    temperatureC = level?.takeIf { hasTemp }?.temperatureC?.roundToInt())
            }
            val speed = hypot(level.u, level.v)
            val temp = if (hasTemp) level.temperatureC.roundToInt() else null
            if (speed < LIGHT_AND_VARIABLE_KT) {
                WindsAloftRow(altitude, temperatureC = temp, lightAndVariable = true)
            } else {
                WindsAloftRow(altitude, direction(level.u, level.v), speed.roundToInt(), temp)
            }
        }

    /**
     * Wind and temperature at [heightFt], linearly between the levels around
     * it. Wind is interpolated as components, so directions either side of
     * north average correctly. Null outside the levels.
     */
    fun interpolate(levels: List<Level>, heightFt: Double): Level? {
        val sorted = levels.sortedBy { it.heightFt }
        val above = sorted.indexOfFirst { it.heightFt >= heightFt }
        if (above < 0) return null
        if (sorted[above].heightFt == heightFt) return sorted[above]
        if (above == 0) return null
        val low = sorted[above - 1]
        val high = sorted[above]
        val f = (heightFt - low.heightFt) / (high.heightFt - low.heightFt)
        return Level(
            heightFt,
            low.u + (high.u - low.u) * f,
            low.v + (high.v - low.v) * f,
            low.temperatureC + (high.temperatureC - low.temperatureC) * f
        )
    }

    /** Wind components, toward east and north, for wind from [directionDeg]. */
    fun components(directionDeg: Double, speedKt: Double): Pair<Double, Double> {
        val rad = Math.toRadians(directionDeg)
        return -speedKt * sin(rad) to -speedKt * cos(rad)
    }

    /** The direction the wind blows from, 1 to 360 degrees true. */
    fun direction(u: Double, v: Double): Int {
        val deg = (Math.toDegrees(atan2(-u, -v)) + 360) % 360
        val rounded = deg.roundToInt() % 360
        return if (rounded == 0) 360 else rounded
    }

    private fun levelsAt(hourly: JsonObject, index: Int): List<Level> =
        PRESSURE_LEVELS_HPA.mapNotNull { p ->
            val height = value(hourly, "geopotential_height_${p}hPa", index) ?: return@mapNotNull null
            val speed = value(hourly, "wind_speed_${p}hPa", index) ?: return@mapNotNull null
            val dir = value(hourly, "wind_direction_${p}hPa", index) ?: return@mapNotNull null
            val temp = value(hourly, "temperature_${p}hPa", index) ?: return@mapNotNull null
            val (u, v) = components(dir, speed)
            Level(height * FEET_PER_METER, u, v, temp)
        }

    private fun value(hourly: JsonObject, name: String, index: Int): Double? =
        (hourly[name] as? JsonArray)?.getOrNull(index)?.jsonPrimitive?.doubleOrNull

    // Open-Meteo times are "2026-11-27T22:00" in UTC
    private fun parseTime(text: String?): Long? = try {
        text?.let { LocalDateTime.parse(it).toInstant(ZoneOffset.UTC).toEpochMilli() }
    } catch (_: Exception) {
        null
    }

    private const val HOUR_MILLIS = 60 * 60 * 1000L
}
