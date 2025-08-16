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

import com.nadmm.airports.utils.TimeUtils
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.text.ParseException
import java.util.Date
import javax.xml.parsers.SAXParserFactory

class TafParser {

    fun parse(xmlFile: File): Taf {
        val taf = Taf().apply {
            fetchTime = xmlFile.lastModified()
        }
        val handler = TafHandler(taf)
        val factory = SAXParserFactory.newInstance()
        factory.newSAXParser().parse(xmlFile, handler)
        return taf
    }

    class TafHandler(private var taf: Taf) : DefaultHandler() {
        private var forecast: Forecast? = null
        private var temperature: Temperature? = null
        private val sb = StringBuilder()
        private val now = Date()

        override fun characters(ch: CharArray, start: Int, length: Int) {
            sb.append(ch, start, length)
        }

        override fun startElement(
            uri: String, localName: String, qName: String,
            attributes: Attributes
        ) {
            // Reset the string builder for elements that will contain character data.
            // This is safer inside a when statement on a per-element basis.
            sb.setLength(0)

            when (qName.lowercase()) {
                "taf" -> taf.isValid = false
                "forecast" -> forecast = Forecast()
                "temperature" -> temperature = Temperature()
                "sky_condition" -> {
                    val name = attributes.getValue("sky_cover")
                    val cloudBase = attributes.getValue("cloud_base_ft_agl")?.toIntOrNull() ?: 0
                    val skyCondition = SkyCondition.of(name, cloudBase)
                    forecast?.skyConditions?.add(skyCondition)
                }
                "turbulence_condition" -> {
                    val turbulence = TurbulenceCondition().apply {
                        intensity = attributes.getValue("turbulence_intensity")?.toIntOrNull() ?: 0
                        minAltitudeFeetAGL = attributes.getValue("turbulence_min_alt_ft_agl")?.toIntOrNull() ?: 0
                        maxAltitudeFeetAGL = attributes.getValue("turbulence_max_alt_ft_agl")?.toIntOrNull() ?: 0
                    }
                    forecast?.turbulenceConditions?.add(turbulence)
                }
                "icing_condition" -> {
                    val icing = IcingCondition().apply {
                        intensity = attributes.getValue("icing_intensity")?.toIntOrNull() ?: 0
                        minAltitudeFeetAGL = attributes.getValue("icing_min_alt_ft_agl")?.toIntOrNull() ?: 0
                        maxAltitudeFeetAGL = attributes.getValue("icing_max_alt_ft_agl")?.toIntOrNull() ?: 0
                    }
                    forecast?.icingConditions?.add(icing)
                }
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            val text = sb.trim().toString()

            when (qName.lowercase()) {
                "raw_text" -> taf.rawText = text
                "issue_time" -> taf.issueTime = safeParseTime(text)
                "bulletin_time" -> taf.bulletinTime = safeParseTime(text)
                "valid_time_from" -> taf.validTimeFrom = safeParseTime(text)
                "valid_time_to" -> taf.validTimeTo = safeParseTime(text)
                "elevation_m" -> taf.stationElevationMeters = text.toFloatOrNull() ?: 0f
                "remarks" -> taf.remarks = text
                "forecast" -> {
                    forecast?.let { currentForecast ->
                        // Only add forecasts that have not yet expired.
                        if (now.time < currentForecast.timeTo) {
                            if (currentForecast.wxList.isEmpty()) {
                                WxSymbol.get("NSW", "")?.let { wx -> currentForecast.wxList.add(wx) }
                            }
                            taf.forecasts.add(currentForecast)
                        }
                    }
                }
                "temperature" -> temperature?.let { forecast?.temperatures?.add(it) }
                "wx_string" -> forecast?.let { WxSymbol.parseWxSymbols(it.wxList, text) }
                "fcst_time_from" -> forecast?.timeFrom = safeParseTime(text)
                "fcst_time_to" -> forecast?.timeTo = safeParseTime(text)
                "time_becoming" -> forecast?.timeBecoming = safeParseTime(text)
                "change_indicator" -> forecast?.changeIndicator = text
                "probability" -> forecast?.probability = text.toIntOrNull() ?: 0
                "wind_dir_degrees" -> forecast?.windDirDegrees = text.toIntOrNull() ?: 0
                "wind_speed_kt" -> forecast?.windSpeedKnots = text.toIntOrNull() ?: 0
                "wind_gust_kt" -> forecast?.windGustKnots = text.toIntOrNull() ?: 0
                "wind_shear_dir_degrees" -> forecast?.windShearDirDegrees = text.toIntOrNull() ?: 0
                "wind_shear_speed_kt" -> forecast?.windShearSpeedKnots = text.toIntOrNull() ?: 0
                "wind_shear_hgt_ft_agl" -> forecast?.windShearHeightFeetAGL = text.toIntOrNull() ?: 0
                "visibility_statute_mi" -> forecast?.visibilitySM = text.toFloatOrNull() ?: 6.1f
                "altim_in_hg" -> forecast?.altimeterHg = text.toFloatOrNull() ?: 0f
                "vert_vis_ft" -> forecast?.vertVisibilityFeet = text.toIntOrNull() ?: 0
                "valid_time" -> temperature?.validTime = safeParseTime(text)
                "sfc_temp_c" -> temperature?.surfaceTempCentigrade = text.toFloatOrNull() ?: Float.MAX_VALUE
                "taf" -> taf.isValid = true
            }
        }

        private fun safeParseTime(timeStr: String): Long {
            return try {
                TimeUtils.parse3339(timeStr).time
            } catch (e: ParseException) {
                0L // Return epoch on parsing failure
            }
        }
    }
}
