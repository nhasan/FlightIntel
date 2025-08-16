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
import com.nadmm.airports.utils.WxUtils.computeFlightCategory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.xml.parsers.SAXParserFactory

class MetarParser {

    fun parse(xmlFile: File, stationIds: List<String>): List<Metar> {
        val parsedMetars = mutableMapOf<String, Metar>()

        val handler = MetarHandler(xmlFile, parsedMetars)
        val factory = SAXParserFactory.newInstance()
        factory.newSAXParser().parse(xmlFile, handler)

        // Now put the missing ones
        stationIds.forEach { stationId ->
            parsedMetars.getOrPut(stationId) {
                Metar(stationId = stationId)
            }
        }

        return parsedMetars.values.toList()
    }

    private inner class MetarHandler(
        private val xmlFile: File,
        private val parsedMetars: MutableMap<String, Metar>
    ) : DefaultHandler() {
        private lateinit var metar: Metar
        private val sb = StringBuilder()

        override fun characters(ch: CharArray, start: Int, length: Int) {
            sb.append(ch, start, length)
        }

        override fun startElement(
            uri: String, localName: String, qName: String,
            attributes: Attributes
        ) {
            sb.clear()
            if (qName.equals("metar", ignoreCase = true)) {
                metar = Metar(fetchTime = xmlFile.lastModified())
            } else if (qName.equals("sky_condition", ignoreCase = true)) {
                val skyCover = attributes.getValue("sky_cover")
                val cloudBaseAGL = attributes.getValue("cloud_base_ft_agl")?.toInt() ?: 0
                val skyCondition = SkyCondition.of(skyCover, cloudBaseAGL)
                metar.skyConditions.add(skyCondition)
            }
        }

        // In endElement function
        override fun endElement(uri: String, localName: String, qName: String) {
            val text = sb.trim().toString()

            when (qName.lowercase()) {
                "metar" -> {
                    metar.isValid = metar.stationId != null && metar.rawText != null
                    if (metar.isValid) {
                        parseRemarks(metar)
                        setMissingFields(metar)
                    }
                    metar.stationId?.let { parsedMetars[it] = metar }
                }
                "raw_text" -> metar.rawText = text
                "observation_time" -> {
                    try {
                        val time = Instant.parse(text)
                        metar.observationTime = time.toEpochMilli()
                    } catch (_: DateTimeParseException) { // Use a more specific exception
                    }
                }
                "station_id" -> metar.stationId = text
                "elevation_m" -> metar.stationElevationMeters = text.toFloatOrNull() ?: 0.0f
                "temp_c" -> metar.tempCelsius = text.toFloatOrNull() ?: 0.0f
                "dewpoint_c" -> metar.dewpointCelsius = text.toFloatOrNull() ?: 0.0f
                "wind_dir_degrees" -> metar.windDirDegrees = text.toIntOrNull() ?: 0
                "wind_speed_kt" -> metar.windSpeedKnots = text.toIntOrNull() ?: 0
                "wind_gust_kt" -> metar.windGustKnots = text.toIntOrNull() ?: 0
                "visibility_statute_mi" -> metar.visibilitySM = text.toFloatOrNull() ?: 10.1f
                "altim_in_hg" -> metar.altimeterHg = text.toFloatOrNull() ?: 0.0f
                "sea_level_pressure_mb" -> metar.seaLevelPressureMb = text.toFloatOrNull() ?: 0.0f
                "wx_string" -> WxSymbol.parseWxSymbols(metar.wxList, text)
                "flight_category" -> metar.flightCategory = text
                "three_hr_pressure_tendency_mb" -> metar.pressureTend3HrMb = text.toFloatOrNull() ?: 0.0f
                "maxt_c" -> metar.maxTempCelsiusLast6Hours = text.toFloatOrNull() ?: 0.0f
                "mint_c" -> metar.minTempCelsiusLast6Hours = text.toFloatOrNull() ?: 0.0f
                "maxt24hr_c" -> metar.maxTempCelsiusLast24Hours = text.toFloatOrNull() ?: 0.0f
                "mint24hr_c" -> metar.minTempCelsiusLast24Hours = text.toFloatOrNull() ?: 0.0f
                "precip_in" -> metar.precipInches = text.toFloatOrNull() ?: 0.0f
                "pcp3hr_in" -> metar.precip3HrInches = text.toFloatOrNull() ?: 0.0f
                "pcp6hr_in" -> metar.precip6HrInches = text.toFloatOrNull() ?: 0.0f
                "pcp24hr_in" -> metar.precip24HrInches = text.toFloatOrNull() ?: 0.0f
                "snow_in" -> metar.snowInches = text.toFloatOrNull() ?: 0.0f
                "vert_vis_ft" -> metar.vertVisibilityFeet = text.toIntOrNull() ?: Int.MAX_VALUE
                "metar_type" -> metar.metarType = text

                // Flags
                "auto", "auto_station" -> if (text.isTrue()) metar.flags.add(MetarFlag.AUTOMATED_STATION)
                "corrected" -> if (text.isTrue()) metar.flags.add(MetarFlag.CORRECTED)
                "maintenance_indicator_on" -> if (text.isTrue()) metar.flags.add(MetarFlag.MAINTENANCE_INDICATOR_ON)
                "present_weather_sensor_off" -> if (text.isTrue()) metar.flags.add(MetarFlag.PRESENT_WEATHER_SENSOR_OFF)
                "lightning_sensor_off" -> if (text.isTrue()) metar.flags.add(MetarFlag.LIGHTNING_SENSOR_OFF)
                "freezing_rain_sensor_off" -> if (text.isTrue()) metar.flags.add(MetarFlag.FREEZING_RAIN_SENSOR_OFF)
                "no_signal" -> if (text.isTrue()) metar.flags.add(MetarFlag.NO_SIGNAL)
            }
        }
    }

    private fun parseRemarks(metar: Metar) {
        val rawText = metar.rawText ?: return
        val index = rawText.indexOf("RMK")
        if (index == -1) {
            return
        }
        val remarks = rawText.substring(index+4)
            .split("\\s+".toRegex())
            .dropLastWhile { it.isEmpty() }
        val it = remarks.iterator()
        while (it.hasNext()) {
            var rmk = it.next()
            when (rmk) {
                "PRESRR" -> metar.weatherPhenomena.add(WeatherPhenomenon.PRESSURE_RISING_RAPIDLY)
                "PRESFR" -> metar.weatherPhenomena.add(WeatherPhenomenon.PRESSURE_FALLING_RAPIDLY)
                "SNINCR" -> metar.weatherPhenomena.add(WeatherPhenomenon.SNOW_INCREASING_RAPIDLY)
                "WSHFT" -> metar.weatherPhenomena.add(WeatherPhenomenon.WIND_SHIFT)
                "FROPA" -> metar.weatherPhenomena.add(WeatherPhenomenon.FRONTAL_PASSAGE)
                "LTG" -> metar.weatherPhenomena.add(WeatherPhenomenon.LIGHTNING)
                "PNO" -> metar.flags.add(MetarFlag.RAIN_SENSOR_OFF)
                "PK" -> {
                    rmk = if (it.hasNext()) it.next() else ""
                    if (rmk == "WND" && it.hasNext()) {
                        rmk = it.next()
                        metar.windPeakKnots = rmk.substring(3, rmk.indexOf('/')).toIntOrNull() ?: 0
                    }
                }
            }

        }
    }

    private fun setMissingFields(metar: Metar) {
        if (metar.flightCategory == WxUtils.FLIGHT_CATEGORY_UNKN) {
            metar.flightCategory = computeFlightCategory(metar.skyConditions, metar.visibilitySM)
        }
        if (metar.vertVisibilityFeet < Int.MAX_VALUE) {
            // Check to see if we have an OVX layer, if not add it
            metar.skyConditions.find { sky -> sky.skyCover == "OVX" } ?:
                metar.skyConditions.add(SkyCondition.of("OVX", 0))
        }
        if (metar.skyConditions.isEmpty()) {
            // Sky condition is not available in the METAR
            metar.skyConditions.add(SkyCondition.of("SKM", 0))
        }
    }

    private fun CharSequence.isTrue(): Boolean {
        return this.toString().equals("true", ignoreCase = true)
    }
}