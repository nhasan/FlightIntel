/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2022 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.tfr

import com.nadmm.airports.tfr.TfrList.AltitudeType
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.tfr.TfrList.TfrArea
import com.nadmm.airports.tfr.TfrList.TfrAreaPart
import com.nadmm.airports.tfr.TfrList.TfrFix
import com.nadmm.airports.tfr.TfrList.TfrShape
import com.nadmm.airports.tfr.TfrList.TfrVertex
import com.nadmm.airports.tfr.TfrList.TfrVertexType
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileReader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.SAXParserFactory

private val WHITESPACE = Regex("\\s+")

class TfrParser {
    fun parse(xml: File, tfrList: TfrList) {
        try {
            tfrList.fetchTime = xml.lastModified()
            val input = InputSource(FileReader(xml))
            val factory = SAXParserFactory.newInstance()
            val parser = factory.newSAXParser()
            val handler = TfrHandler(tfrList)
            val xmlReader = parser.xmlReader
            xmlReader.contentHandler = handler
            xmlReader.parse(input)
            tfrList.entries.sort()
        } catch (ignored: Exception) {
        }
    }

    private inner class TfrHandler(private val tfrList: TfrList) : DefaultHandler() {
        private var tfr: Tfr = Tfr()
        private val text: StringBuilder
        private var shapeMinAlt: TfrList.Altitude? = null
        private var shapeMaxAlt: TfrList.Altitude? = null
        private var shapeTimes: List<Long?> = emptyList()
        private var area: TfrArea? = null
        private var part: TfrAreaPart? = null
        private var replacedReason: String? = null
        private val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        override fun characters(ch: CharArray, start: Int, length: Int) {
            text.append(ch, start, length)
        }

        override fun startElement(
            uri: String, localName: String, qName: String,
            attributes: Attributes
        ) {
            if (qName.uppercase() == "TFRLIST") {
                tfrList.fetchTime = parseDateTime(attributes.getValue("timestamp"))
            } else if (qName.uppercase() == "TFR") {
                tfr = Tfr()
            } else if (qName == "SHAPE") {
                shapeMinAlt = attributes.getValue("minalt")?.let(::parseAltitude)
                shapeMaxAlt = attributes.getValue("maxalt")?.let(::parseAltitude)
                shapeTimes = parseTimes(attributes)
            } else if (qName == "AREA") {
                val times = parseTimes(attributes)
                area = TfrArea(
                    attributes.getValue("name")?.trim()?.takeIf { it.isNotEmpty() },
                    attributes.getValue("minalt")?.let(::parseAltitude),
                    attributes.getValue("maxalt")?.let(::parseAltitude),
                    times[0], times[1], times[2], times[3],
                    ArrayList()
                )
            } else if (qName == "PART") {
                part = TfrAreaPart(attributes.getValue("op") == "SUBTR", ArrayList())
            } else if (qName == "POINT" || qName == "CIRCLE" || qName == "ARC") {
                parseVertex(qName, attributes)?.let { part?.vertices?.add(it) }
            } else if (qName == "REPLACED") {
                replacedReason = attributes.getValue("reason")
            }
            text.setLength(0)
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (qName.equals("NID", ignoreCase = true)) {
                tfr.notamId = text.toString().trim { it <= ' ' }
            } else if (qName.equals("NAME", ignoreCase = true)) {
                tfr.name = text.toString().trim { it <= ' ' }
            } else if (qName.equals("CITY", ignoreCase = true)) {
                tfr.city = text.toString().trim { it <= ' ' }
            } else if (qName.equals("STATE", ignoreCase = true)) {
                tfr.state = text.toString().trim { it <= ' ' }
            } else if (qName.equals("FACILITY", ignoreCase = true)) {
                tfr.facility = text.toString().trim { it <= ' ' }
            } else if (qName.equals("FACILITYTYPE", ignoreCase = true)) {
                tfr.facilityType = text.toString().trim { it <= ' ' }
            } else if (qName.equals("SRC", ignoreCase = true)) {
                tfr.text = text.toString().trim { it <= ' ' }
            } else if (qName.equals("TYPE", ignoreCase = true)) {
                tfr.type = text.toString().trim { it <= ' ' }
            } else if (qName.equals("MINALT", ignoreCase = true)) {
                parseMinAlt(text.toString())
            } else if (qName.equals("MAXALT", ignoreCase = true)) {
                parseMaxAlt(text.toString())
            } else if (qName == "CREATED") {
                tfr.createTime = parseDateTime(text.toString())
            } else if (qName == "MODIFIED") {
                tfr.modifyTime = parseDateTime(text.toString())
            } else if (qName == "ACTIVE") {
                tfr.activeTime = parseDateTime(text.toString())
            } else if (qName == "EXPIRES") {
                tfr.expireTime = parseDateTime(text.toString())
            } else if (qName == "SHAPE") {
                parseShape(text.toString())?.let { tfr.shapes.add(it) }
            } else if (qName == "PART") {
                part?.let { if (it.vertices.isNotEmpty()) area?.parts?.add(it) }
                part = null
            } else if (qName == "AREA") {
                area?.let { if (it.parts.isNotEmpty()) tfr.areas.add(it) }
                area = null
            } else if (qName == "REASON") {
                tfr.reason = text.toString().trim()
            } else if (qName == "AUTHORITY") {
                tfr.authority = text.toString().trim()
            } else if (qName == "FACILITYNAME") {
                tfr.facilityName = text.toString().trim()
            } else if (qName == "REPLACED") {
                tfr.replacedNotamId = text.toString().trim()
                tfr.replacedReason = replacedReason?.trim()
            } else if (qName == "RESTRICTION") {
                tfr.restriction = text.toString().trim()
            } else if (qName == "INSTRUCTION") {
                text.toString().trim().takeIf { it.isNotEmpty() }?.let { tfr.instructions.add(it) }
            } else if (qName.uppercase() == "TFR") {
                if (!tfr.name.equals("Latest Update", ignoreCase = true)) {
                    if (tfr.modifyTime == 0L) {
                        tfr.modifyTime = tfr.createTime
                    }
                    tfrList.entries.add(tfr)
                }
            }
        }

        /** Parses a shape written as "lat,lon lat,lon ..." in decimal degrees. */
        private fun parseShape(text: String): TfrShape? {
            val points = text.trim().split(WHITESPACE).mapNotNull { point ->
                val (lat, lon) = point.split(',').takeIf { it.size == 2 } ?: return@mapNotNull null
                val latitude = lat.toDoubleOrNull() ?: return@mapNotNull null
                val longitude = lon.toDoubleOrNull() ?: return@mapNotNull null
                if (latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                    latitude to longitude
                } else {
                    null
                }
            }
            if (points.size < 3) return null
            return TfrShape(
                points.map { it.first }.toDoubleArray(),
                points.map { it.second }.toDoubleArray(),
                shapeMinAlt,
                shapeMaxAlt,
                shapeTimes.getOrNull(0),
                shapeTimes.getOrNull(1),
                shapeTimes.getOrNull(2),
                shapeTimes.getOrNull(3)
            )
        }

        /** The active, expires, dailystart and dailyend times of an area. */
        private fun parseTimes(attributes: Attributes): List<Long?> =
            listOf("active", "expires", "dailystart", "dailyend").map { name ->
                attributes.getValue(name)?.let(::parseDateTime)?.takeIf { it < Long.MAX_VALUE }
            }

        private fun parseVertex(qName: String, attributes: Attributes): TfrVertex? {
            val latitude = attributes.getValue("lat")?.toDoubleOrNull() ?: return null
            val longitude = attributes.getValue("lon")?.toDoubleOrNull() ?: return null
            val type = when (qName) {
                "CIRCLE" -> TfrVertexType.CIRCLE
                "ARC" -> if (attributes.getValue("dir") == "CCW") TfrVertexType.ARC_CCW
                    else TfrVertexType.ARC_CW
                else -> TfrVertexType.POINT
            }
            val ident = attributes.getValue("ident")
            val bearing = attributes.getValue("bearing")?.toIntOrNull()
            val distance = attributes.getValue("distance")?.toDoubleOrNull()
            val fix = if (ident != null && bearing != null && distance != null) {
                TfrFix(attributes.getValue("navaid"), attributes.getValue("navtype"),
                    ident, bearing, distance)
            } else {
                null
            }
            return TfrVertex(type, latitude, longitude,
                attributes.getValue("radius")?.toDoubleOrNull(), fix)
        }

        /** Parses an altitude such as "2400M" (MSL) or "0A" (AGL). */
        private fun parseAltitude(alt: String): TfrList.Altitude? {
            val value = alt.trim()
            val feet = value.dropLast(1).toIntOrNull() ?: return null
            val type = when (value.lastOrNull()) {
                'A' -> AltitudeType.AGL
                'M' -> AltitudeType.MSL
                else -> return null
            }
            return TfrList.Altitude(feet, type)
        }

        private fun parseDateTime(text: String): Long {
            var dt = Long.MAX_VALUE
            try {
                dt = sdf.parse(text.trim { it <= ' ' })?.time ?: Long.MAX_VALUE
            } catch (ignored: ParseException) {
            }
            return dt
        }

        private fun parseMinAlt(alt: String) {
            tfr.minAltitudeFeet = alt.substring(0, alt.length - 1).toInt()
            tfr.minAltitudeType =
                if (alt.startsWith("A", alt.length - 1)) AltitudeType.AGL else AltitudeType.MSL
        }

        private fun parseMaxAlt(alt: String) {
            tfr.maxAltitudeFeet = alt.substring(0, alt.length - 1).toInt()
            tfr.maxAltitudeType =
                if (alt.startsWith("A", alt.length - 1)) AltitudeType.AGL else AltitudeType.MSL
        }

        init {
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            text = StringBuilder()
        }
    }
}