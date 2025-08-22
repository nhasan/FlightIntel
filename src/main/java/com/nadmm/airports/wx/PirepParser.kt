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

import android.location.Location
import com.nadmm.airports.utils.GeoUtils
import com.nadmm.airports.utils.GeoUtils.getMagneticDeclination
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileReader
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.SAXParserFactory
import kotlin.math.roundToInt

class PirepParser {

    companion object {
        fun parse(xml: File, location: Location, radiusNM: Int): Pirep {
            val pirep = Pirep()
            pirep.fetchTime = xml.lastModified()
            val input = InputSource(FileReader(xml))
            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            val magneticDeclination = getMagneticDeclination(location)
            val handler = PirepHandler(pirep, location, radiusNM, magneticDeclination)
            val xmlReader = parser.xmlReader
            xmlReader.contentHandler = handler
            xmlReader.parse(input)
            return pirep
        }
    }

    private class PirepHandler(
        private val pirep: Pirep,
        private val currentLocation: Location,
        private val searchRadiusNM: Int,
        private val magneticDeclination: Float
    ) : DefaultHandler() {
        private var entry: PirepEntry = PirepEntry()
        private val sb = StringBuilder()

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            sb.append(ch, start, length)
        }

        override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) {
            if (qName.equals("AircraftReport", ignoreCase = true)) {
                entry = PirepEntry()
            } else if (qName.equals("sky_condition", ignoreCase = true)) {
                val skyCondition = PirepSkyCondition.of(attributes)
                entry.skyConditions.add(skyCondition)
            } else if (qName.equals("turbulence_condition", ignoreCase = true)) {
                val turbulenceCondition = PirepTurbulenceCondition.of(attributes)
                entry.turbulenceConditions.add(turbulenceCondition)
            } else if (qName.equals("icing_condition", ignoreCase = true)) {
                val icingCondition = PirepIcingCondition.of(attributes)
                entry.icingConditions.add(icingCondition)
            } else {
                sb.setLength(0)
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String) {
            val text = sb.toString().trim()
            if (qName.equals("raw_text", ignoreCase = true)) {
                entry.rawText = text
                val remarksStart = entry.rawText.indexOf("/RM ")
                if (remarksStart != -1) {
                    entry.remarks = entry.rawText.substring(remarksStart + 4).trim()
                }
            } else if (qName.equals("report_type", ignoreCase = true)) {
                entry.reportType = text
            } else if (qName.equals("receipt_time", ignoreCase = true)) {
                entry.receiptTime = parseDateTime(text)
            } else if (qName.equals("observation_time", ignoreCase = true)) {
                entry.observationTime = parseDateTime(text)
            } else if (qName.equals("wx_string", ignoreCase = true)) {
                WxSymbol.parseWxSymbols(entry.wxList, text)
            } else if (qName.equals("aircraft_ref", ignoreCase = true)) {
                entry.aircraftRef = if (text == "UNKN") "Unknown" else text
            } else if (qName.equals("latitude", ignoreCase = true)) {
                entry.latitude = text.toFloat()
            } else if (qName.equals("longitude", ignoreCase = true)) {
                entry.longitude = text.toFloat()
            } else if (qName.equals("altitude_ft_msl", ignoreCase = true)) {
                entry.altitudeFeetMsl = text.toInt()
            } else if (qName.equals("visibility_statute_mi", ignoreCase = true)) {
                entry.visibilitySM = text.toInt()
            } else if (qName.equals("temp_c", ignoreCase = true)) {
                entry.tempCelsius = text.toInt()
            } else if (qName.equals("wind_dir_degrees", ignoreCase = true)) {
                entry.windDirDegrees = text.toInt()
            } else if (qName.equals("wind_speed_kt", ignoreCase = true)) {
                entry.windSpeedKnots = text.toInt()
            } else if (qName.equals("vert_gust_kt", ignoreCase = true)) {
                entry.vertGustKnots = text.toInt()
            } else if (qName.equals("no_time_stamp", ignoreCase = true)) {
                if (text.equals("true", ignoreCase = true)) {
                    entry.flags.add(PirepFlags.NoTimeStamp)
                }
            } else if (qName.equals("above_ground_level_indicated", ignoreCase = true)) {
                if (text.equals("true", ignoreCase = true)) {
                    entry.flags.add(PirepFlags.AglIndicated)
                }
            } else if (qName.equals("AircraftReport", ignoreCase = true)) {
                if (validateEntry(entry)) {
                    pirep.entries.add(entry)
                }
            }
        }

        private fun parseDateTime(text: String): Long {
            try {
                val odt = OffsetDateTime.parse(text, DateTimeFormatter.ISO_DATE_TIME)
                return odt.toInstant().toEpochMilli()
            } catch (_: Exception) {
            }
            return Long.MAX_VALUE
        }

        private fun validateEntry(entry: PirepEntry): Boolean {
            val results = FloatArray(2)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                entry.latitude.toDouble(), entry.longitude.toDouble(),
                results
            )
            val distanceNM = results[0] / GeoUtils.METERS_PER_NAUTICAL_MILE
            if (distanceNM <= searchRadiusNM || entry.flags.contains(PirepFlags.BadLocation)) {
                entry.distanceNM = distanceNM.roundToInt()
                entry.bearing = GeoUtils.applyDeclination(results[1].roundToInt(),  magneticDeclination)
                entry.isValid = true
            }
            return entry.isValid
        }
    }
}
