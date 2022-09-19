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
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileReader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.SAXParserFactory

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
            } else if (qName == "TYPE") {
                tfr.type = text.toString().trim { it <= ' ' }
            } else if (qName.uppercase() == "TFR") {
                if (!tfr.name.equals("Latest Update", ignoreCase = true)) {
                    if (tfr.modifyTime == 0L) {
                        tfr.modifyTime = tfr.createTime
                    }
                    tfrList.entries.add(tfr)
                }
            }
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