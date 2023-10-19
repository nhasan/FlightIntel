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

import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.wx.Taf.Forecast
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.Date
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

class TafParser {
    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    fun parse(xml: File): Taf {
        val taf = Taf()
        taf.fetchTime = xml.lastModified()
        val factory = SAXParserFactory.newInstance()
        val parser = factory.newSAXParser()
        val xmlReader = parser.xmlReader
        val handler = TafHandler(taf)
        xmlReader.contentHandler = handler
        val input = InputSource(FileReader(xml))
        xmlReader.parse(input)
        return taf
    }

    inner class TafHandler(private var taf: Taf) : DefaultHandler() {
        private var forecast: Forecast? = null
        private var temperature: Taf.Temperature? = null
        private val text = StringBuilder()
        private val now = Date()
        override fun characters(ch: CharArray, start: Int, length: Int) {
            text.appendRange(ch, start, start + length)
        }

        override fun startElement(
            uri: String, localName: String, qName: String,
            attributes: Attributes
        ) {
            if (qName.equals("taf", ignoreCase = true)) {
                taf.isValid = false
            } else if (qName == "forecast") {
                forecast = Forecast()
            } else if (qName == "temperature") {
                temperature = Taf.Temperature()
            } else if (qName.equals("sky_condition", ignoreCase = true)) {
                val name = attributes.getValue("sky_cover")
                var cloudBase = 0
                if (attributes.getIndex("cloud_base_ft_agl") >= 0) {
                    cloudBase = attributes.getValue("cloud_base_ft_agl").toInt()
                }
                val skyCondition = SkyCondition.create(name, cloudBase)
                forecast?.skyConditions?.add(skyCondition)
            } else if (qName.equals("turbulence_condition", ignoreCase = true)) {
                val turbulence = Taf.TurbulenceCondition()
                attributes.getValue("turbulence_intensity")?.let {
                    turbulence.intensity = it.toInt()
                }
                attributes.getValue("turbulence_min_alt_ft_agl")?.let {
                    turbulence.minAltitudeFeetAGL = it.toInt()
                }
                attributes.getValue("turbulence_max_alt_ft_agl")?.let {
                    turbulence.maxAltitudeFeetAGL = it.toInt()
                }
                forecast?.turbulenceConditions?.add(turbulence)
            } else if (qName.equals("icing_condition", ignoreCase = true)) {
                val icing = Taf.IcingCondition()
                attributes.getValue("icing_intensity")?.let {
                    icing.intensity = it.toInt()
                }
                attributes.getValue("icing_min_alt_ft_agl")?.let {
                    icing.minAltitudeFeetAGL = it.toInt()
                }
                attributes.getValue("icing_max_alt_ft_agl")?.let {
                    icing.maxAltitudeFeetAGL = it.toInt()
                }
                forecast?.icingConditions?.add(icing)
            } else {
                text.setLength(0)
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (qName.equals("raw_text", ignoreCase = true)) {
                taf.rawText = text.toString()
            } else if (qName.equals("issue_time", ignoreCase = true)) {
                taf.issueTime = TimeUtils.parse3339(text.toString()).time
            } else if (qName.equals("bulletin_time", ignoreCase = true)) {
                taf.bulletinTime = TimeUtils.parse3339(text.toString()).time
            } else if (qName.equals("valid_time_from", ignoreCase = true)) {
                taf.validTimeFrom = TimeUtils.parse3339(text.toString()).time
            } else if (qName.equals("valid_time_to", ignoreCase = true)) {
                taf.validTimeTo = TimeUtils.parse3339(text.toString()).time
            } else if (qName.equals("elevation_m", ignoreCase = true)) {
                taf.stationElevationMeters = text.toString().toFloat()
            } else if (qName.equals("remarks", ignoreCase = true)) {
                taf.remarks = text.toString()
            } else if (qName.equals("forecast", ignoreCase = true)) {
                forecast?.let {
                    if (now.time < it.timeTo) {
                        if (it.wxList.isEmpty()) {
                            it.wxList.add(WxSymbol.get("NSW", ""))
                        }
                        taf.forecasts.add(it)
                    }
                }
            } else if (qName.equals("temperature", ignoreCase = true)) {
                temperature?.let { forecast?.temperatures?.add(it) }
            } else if (qName.equals("wx_string", ignoreCase = true)) {
                WxSymbol.parseWxSymbols(forecast!!.wxList, text.toString())
            } else if (qName.equals("fcst_time_from", ignoreCase = true)) {
                forecast?.timeFrom = TimeUtils.parse3339(text.toString()).time
            } else if (qName.equals("fcst_time_to", ignoreCase = true)) {
                forecast?.timeTo = TimeUtils.parse3339(text.toString()).time
            } else if (qName.equals("time_becoming", ignoreCase = true)) {
                forecast?.timeBecoming = TimeUtils.parse3339(text.toString()).time
            } else if (qName.equals("change_indicator", ignoreCase = true)) {
                forecast?.changeIndicator = text.toString()
            } else if (qName.equals("probability", ignoreCase = true)) {
                forecast?.probability = text.toString().toInt()
            } else if (qName.equals("wind_dir_degrees", ignoreCase = true)) {
                forecast?.windDirDegrees = text.toString().toIntOrNull() ?: 0
            } else if (qName.equals("wind_speed_kt", ignoreCase = true)) {
                forecast?.windSpeedKnots = text.toString().toInt()
            } else if (qName.equals("wind_gust_kt", ignoreCase = true)) {
                forecast?.windGustKnots = text.toString().toInt()
            } else if (qName.equals("wind_shear_dir_degrees", ignoreCase = true)) {
                forecast?.windShearDirDegrees = text.toString().toInt()
            } else if (qName.equals("wind_shear_speed_kt", ignoreCase = true)) {
                forecast?.windShearSpeedKnots = text.toString().toInt()
            } else if (qName.equals("wind_shear_hgt_ft_agl", ignoreCase = true)) {
                forecast?.windShearHeightFeetAGL = text.toString().toInt()
            } else if (qName.equals("visibility_statute_mi", ignoreCase = true)) {
                forecast?.visibilitySM = text.toString().toFloatOrNull() ?: 6.1F
            } else if (qName.equals("altim_in_hg", ignoreCase = true)) {
                forecast?.altimeterHg = text.toString().toFloat()
            } else if (qName.equals("vert_vis_ft", ignoreCase = true)) {
                forecast?.vertVisibilityFeet = text.toString().toInt()
            } else if (qName.equals("valid_time", ignoreCase = true)) {
                temperature?.validTime = TimeUtils.parse3339(text.toString()).time
            } else if (qName.equals("sfc_temp_c", ignoreCase = true)) {
                temperature?.surfaceTempCentigrade = text.toString().toFloat()
            } else if (qName.equals("taf", ignoreCase = true)) {
                taf.isValid = true
            }
        }
    }
}