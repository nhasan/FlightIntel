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

import android.util.TimeFormatException
import com.nadmm.airports.utils.WxUtils
import com.nadmm.airports.utils.WxUtils.computeFlightCategory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.time.Instant
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

class MetarParser {
    private var mFetchTime: Long = 0
    private val mMetars = HashMap<String?, Metar>()
    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    fun parse(xmlFile: File, stationIds: List<String?>): List<Metar> {
        mFetchTime = xmlFile.lastModified()
        val input = InputSource(FileReader(xmlFile))
        val factory = SAXParserFactory.newInstance()
        val parser = factory.newSAXParser()
        val handler = MetarHandler()
        val xmlReader = parser.xmlReader
        xmlReader.contentHandler = handler
        xmlReader.parse(input)
        val metars = ArrayList(mMetars.values)
        // Now put the missing ones
        for (stationId in stationIds) {
            if (!mMetars.containsKey(stationId)) {
                val metar = Metar()
                metar.stationId = stationId
                metar.fetchTime = mFetchTime
                metars.add(metar)
            }
        }
        return metars
    }

    private inner class MetarHandler : DefaultHandler() {
        private lateinit var metar: Metar
        private val text = StringBuilder()
        override fun characters(ch: CharArray, start: Int, length: Int) {
            text.appendRange(ch, start, start + length)
        }

        override fun startElement(
            uri: String, localName: String, qName: String,
            attributes: Attributes
        ) {
            if (qName.equals("metar", ignoreCase = true)) {
                metar = Metar()
                metar.fetchTime = mFetchTime
            } else if (qName.equals("sky_condition", ignoreCase = true)) {
                val skyCover = attributes.getValue("sky_cover")
                val cloudBaseAGL = attributes.getValue("cloud_base_ft_agl")?.toInt() ?: 0
                val skyCondition = SkyCondition.create(skyCover, cloudBaseAGL)
                metar.skyConditions.add(skyCondition)
            } else {
                text.clear()
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (qName.equals("metar", ignoreCase = true)) {
                metar.isValid = true
                parseRemarks(metar)
                setMissingFields(metar)
                mMetars[metar.stationId] = metar
            } else if (qName.equals("raw_text", ignoreCase = true)) {
                metar.rawText = text.toString()
            } else if (qName.equals("observation_time", ignoreCase = true)) {
                try {
                    val time = Instant.parse(text.toString())
                    metar.observationTime = time.toEpochMilli()
                } catch (ignored: TimeFormatException) {
                }
            } else if (qName.equals("station_id", ignoreCase = true)) {
                metar.stationId = text.toString()
            } else if (qName.equals("elevation_m", ignoreCase = true)) {
                metar.stationElevationMeters = text.toString().toFloat()
            } else if (qName.equals("temp_c", ignoreCase = true)) {
                metar.tempCelsius = text.toString().toFloat()
            } else if (qName.equals("dewpoint_c", ignoreCase = true)) {
                metar.dewpointCelsius = text.toString().toFloat()
            } else if (qName.equals("wind_dir_degrees", ignoreCase = true)) {
                metar.windDirDegrees = text.toString().toIntOrNull() ?: 0
            } else if (qName.equals("wind_speed_kt", ignoreCase = true)) {
                metar.windSpeedKnots = text.toString().toInt()
            } else if (qName.equals("wind_gust_kt", ignoreCase = true)) {
                metar.windGustKnots = text.toString().toInt()
            } else if (qName.equals("visibility_statute_mi", ignoreCase = true)) {
                metar.visibilitySM = text.toString().toFloatOrNull() ?: 10.1F
            } else if (qName.equals("altim_in_hg", ignoreCase = true)) {
                metar.altimeterHg = text.toString().toFloat()
            } else if (qName.equals("sea_level_pressure_mb", ignoreCase = true)) {
                metar.seaLevelPressureMb = text.toString().toFloat()
            } else if (qName.equals("corrected", ignoreCase = true)) {
                if (text.toString().equals("true", ignoreCase = true)) {
                    metar.flags.add(Metar.Flags.Corrected)
                }
            } else if (qName.equals("auto_station", ignoreCase = true)) {
                if (text.toString().equals("true", ignoreCase = true)) {
                    metar.flags.add(Metar.Flags.Auto)
                }
            } else if (qName.equals("auto", ignoreCase = true)) {
                if (text.toString().equals("true", ignoreCase = true)) {
                    metar.flags.add(Metar.Flags.Auto)
                }
            } else if (qName.equals("maintenance_indicator_on", ignoreCase = true)) {
                if (text.toString().equals("true", ignoreCase = true)) {
                    metar.flags.add(Metar.Flags.MaintenanceIndicatorOn)
                }
            } else if (qName.equals("present_weather_sensor_off", ignoreCase = true)) {
                if (text.toString().equals("true", ignoreCase = true)) {
                    metar.flags.add(Metar.Flags.PresentWeatherSensorOff)
                }
            } else if (qName.equals("lightning_sensor_off", ignoreCase = true)) {
                if (text.toString().equals("true", ignoreCase = true)) {
                    metar.flags.add(Metar.Flags.LightningSensorOff)
                }
            } else if (qName.equals("freezing_rain_sensor_off", ignoreCase = true)) {
                if (text.toString().equals("true", ignoreCase = true)) {
                    metar.flags.add(Metar.Flags.FreezingRainSensorOff)
                }
            } else if (qName.equals("no_signal", ignoreCase = true)) {
                if (text.toString().equals("true", ignoreCase = true)) {
                    metar.flags.add(Metar.Flags.NoSignal)
                }
            } else if (qName.equals("wx_string", ignoreCase = true)) {
                WxSymbol.parseWxSymbols(metar.wxList, text.toString())
            } else if (qName.equals("flight_category", ignoreCase = true)) {
                metar.flightCategory = text.toString()
            } else if (qName.equals("three_hr_pressure_tendency_mb", ignoreCase = true)) {
                metar.pressureTend3HrMb = text.toString().toFloat()
            } else if (qName.equals("maxt_c", ignoreCase = true)) {
                metar.maxTemp6HrCentigrade = text.toString().toFloat()
            } else if (qName.equals("mint_c", ignoreCase = true)) {
                metar.minTemp6HrCentigrade = text.toString().toFloat()
            } else if (qName.equals("maxt24hr_c", ignoreCase = true)) {
                metar.maxTemp24HrCentigrade = text.toString().toFloat()
            } else if (qName.equals("mint24hr_c", ignoreCase = true)) {
                metar.minTemp24HrCentigrade = text.toString().toFloat()
            } else if (qName.equals("precip_in", ignoreCase = true)) {
                metar.precipInches = text.toString().toFloat()
            } else if (qName.equals("pcp3hr_in", ignoreCase = true)) {
                metar.precip3HrInches = text.toString().toFloat()
            } else if (qName.equals("pcp6hr_in", ignoreCase = true)) {
                metar.precip6HrInches = text.toString().toFloat()
            } else if (qName.equals("pcp24hr_in", ignoreCase = true)) {
                metar.precip24HrInches = text.toString().toFloat()
            } else if (qName.equals("snow_in", ignoreCase = true)) {
                metar.snowInches = text.toString().toFloat()
            } else if (qName.equals("vert_vis_ft", ignoreCase = true)) {
                metar.vertVisibilityFeet = text.toString().toInt()
            } else if (qName.equals("metar_type", ignoreCase = true)) {
                metar.metarType = text.toString()
            }
        }
    }

    private fun parseRemarks(metar: Metar) {
        val index = metar.rawText?.indexOf("RMK") ?: -1
        if (index == -1) {
            return
        }
        val rmks =
            metar.rawText!!.substring(index+4).split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
        val it = rmks.iterator()
        while (it.hasNext()) {
            var rmk = it.next()
            when (rmk) {
                "PRESRR" -> metar.presrr = true
                "PRESFR" -> metar.presfr = true
                "SNINCR" -> metar.snincr = true
                "WSHFT" -> metar.wshft = true
                "FROPA" -> metar.fropa = true
                "LTG" -> metar.ltg = true
                "PNO" -> metar.flags.add(Metar.Flags.RainSensorOff)
                "PK" -> {
                    rmk = if (it.hasNext()) it.next() else ""
                    if (rmk == "WND" && it.hasNext()) {
                        rmk = it.next()
                        metar.windPeakKnots = rmk.substring(3, rmk.indexOf('/')).toInt()
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
                metar.skyConditions.add(SkyCondition.create("OVX", 0))
        }
        if (metar.skyConditions.isEmpty()) {
            // Sky condition is not available in the METAR
            metar.skyConditions.add(SkyCondition.create("SKM", 0))
        }
    }
}