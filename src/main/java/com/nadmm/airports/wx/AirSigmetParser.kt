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

import com.nadmm.airports.wx.AirSigmet.AirSigmetEntry
import com.nadmm.airports.wx.AirSigmet.AirSigmetPoint
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileReader
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Date
import javax.xml.parsers.SAXParserFactory

class AirSigmetParser {
    fun parse(xml: File, airSigmet: AirSigmet) {
        try {
            airSigmet.fetchTime = xml.lastModified()
            val input = InputSource(FileReader(xml))
            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            val handler = AirSigmetHandler(airSigmet)
            val xmlReader = parser.xmlReader
            xmlReader.contentHandler = handler
            xmlReader.parse(input)
        } catch (_: Exception) {
        }
    }

    private inner class AirSigmetHandler(private val airSigmet: AirSigmet) :
        DefaultHandler() {
        private lateinit var entry: AirSigmetEntry
        private lateinit var point: AirSigmetPoint
        private val now: Date = Date()
        private val text = StringBuilder()

        override fun characters(ch: CharArray, start: Int, length: Int) {
            text.append(ch, start, length)
        }

        override fun startElement(
            uri: String, localName: String, qName: String,
            attributes: Attributes
        ) {
            if (qName.equals("AIRSIGMET", ignoreCase = true)) {
                entry = AirSigmetEntry()
            } else if (qName.equals("altitude", ignoreCase = true)) {
                attributes.getValue("min_ft_msl")?.let { attr ->
                    entry.minAltitudeFeet = attr.toInt()
                }
                attributes.getValue("max_ft_msl")?.let { attr ->
                    entry.maxAltitudeFeet = attr.toInt()
                }
            } else if (qName.equals("hazard", ignoreCase = true)) {
                entry.hazardType = attributes.getValue("type")
                entry.hazardSeverity = attributes.getValue("severity")
            } else if (qName.equals("point", ignoreCase = true)) {
                point = AirSigmetPoint()
            } else {
                text.setLength(0)
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (qName.equals("raw_text", ignoreCase = true)) {
                entry.rawText = text.toString()
            } else if (qName.equals("valid_time_from", ignoreCase = true)) {
                try {
                    val instant = Instant.parse(text.toString())
                    entry.fromTime = instant.toEpochMilli()
                } catch (_: DateTimeParseException) {
                }
            } else if (qName.equals("valid_time_to", ignoreCase = true)) {
                try {
                    val instant = Instant.parse(text.toString())
                    entry.toTime = instant.toEpochMilli()
                } catch (_: DateTimeParseException) {
                }
            } else if (qName.equals("airsigmet_type", ignoreCase = true)) {
                entry.type = text.toString()
            } else if (qName.equals("movement_dir_degrees", ignoreCase = true)) {
                entry.movementDirDegrees = text.toString().toInt()
            } else if (qName.equals("movement_speed_kt", ignoreCase = true)) {
                entry.movementSpeedKnots = text.toString().toInt()
            } else if (qName.equals("latitude", ignoreCase = true)) {
                point.latitude = text.toString().toFloat()
            } else if (qName.equals("longitude", ignoreCase = true)) {
                point.longitude = text.toString().toFloat()
            } else if (qName.equals("point", ignoreCase = true)) {
                entry.points.add(point)
            } else if (qName.equals("AIRSIGMET", ignoreCase = true)) {
                if (now.time <= entry.toTime) {
                    airSigmet.entries.add(entry)
                }
            } else if (qName.equals("data", ignoreCase = true)) {
                if (airSigmet.entries.isNotEmpty()) {
                    airSigmet.isValid = true
                }
            }
        }
    }
}