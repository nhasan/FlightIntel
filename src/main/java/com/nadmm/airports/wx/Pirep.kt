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

import org.xml.sax.Attributes
import java.io.Serializable

class Pirep : Serializable {
    class SkyCondition(
        @JvmField var skyCover: String,
        @JvmField var baseFeetMSL: Int,
        @JvmField var topFeetMSL: Int
    ) : Serializable {
        companion object {
            fun of(attributes: Attributes): SkyCondition {
                val skyCover = attributes.getValue("sky_cover") ?: "SKM"
                val baseFeetMSL = attributes.getValue("cloud_base_ft_msl")?.toInt() ?: Int.MAX_VALUE
                val topFeetMSL = attributes.getValue("cloud_top_ft_msl")?.toInt() ?: Int.MAX_VALUE
                return SkyCondition(skyCover, baseFeetMSL, topFeetMSL)
            }
        }
    }

    class TurbulenceCondition(
        @JvmField var type: String,
        @JvmField var intensity: String,
        @JvmField var frequency: String,
        @JvmField var baseFeetMSL: Int,
        @JvmField var topFeetMSL: Int
    ) : Serializable {
        companion object {
            fun of(attributes: Attributes): TurbulenceCondition {
                val type = attributes.getValue("turbulence_type") ?: ""
                val intensity = attributes.getValue("turbulence_intensity") ?: ""
                val frequency = attributes.getValue("turbulence_freq") ?: ""
                val baseFeetMSL = attributes.getValue("turbulence_base_ft_msl")?.toInt() ?: Int.MAX_VALUE
                val topFeetMSL = attributes.getValue("turbulence_top_ft_msl")?.toInt() ?: Int.MAX_VALUE
                return TurbulenceCondition(type, intensity, frequency, baseFeetMSL, topFeetMSL)
            }
        }
    }

    class IcingCondition(
        @JvmField var type: String,
        @JvmField var intensity: String,
        @JvmField var baseFeetMSL: Int,
        @JvmField var topFeetMSL: Int
    ) : Serializable {
        companion object {
            fun of(attributes: Attributes): IcingCondition {
                val type = attributes.getValue("icing_type") ?: ""
                val intensity = attributes.getValue("icing_intensity") ?: ""
                val baseFeetMSL = attributes.getValue("icing_base_ft_msl")?.toInt() ?: Int.MAX_VALUE
                val topFeetMSL = attributes.getValue("icing_top_ft_msl")?.toInt() ?: Int.MAX_VALUE
                return IcingCondition(type, intensity, baseFeetMSL, topFeetMSL)
            }
        }
    }

    enum class Flags {
        NoTimeStamp {
            override fun toString(): String {
                return "No timestamp"
            }
        },
        AglIndicated {
            override fun toString(): String {
                return "AGL indicated"
            }
        },
        BadLocation {
            override fun toString(): String {
                return "Bad location"
            }
        }
    }

    class PirepEntry : Serializable {
        @JvmField var isValid = false
        @JvmField var receiptTime = Long.MAX_VALUE
        @JvmField var observationTime = Long.MAX_VALUE
        @JvmField var reportType = ""
        @JvmField var rawText = ""
        @JvmField var aircraftRef = ""
        @JvmField var latitude = 0f
        @JvmField var longitude = 0f
        @JvmField var distanceNM = 0
        @JvmField var bearing = 0
        @JvmField var altitudeFeetMsl = Int.MAX_VALUE
        @JvmField var visibilitySM = Int.MAX_VALUE
        @JvmField var tempCelsius = Int.MAX_VALUE
        @JvmField var windDirDegrees = Int.MAX_VALUE
        @JvmField var windSpeedKnots = Int.MAX_VALUE
        @JvmField var vertGustKnots = Int.MAX_VALUE
        @JvmField var flags = ArrayList<Flags>()
        @JvmField var skyConditions = ArrayList<SkyCondition>()
        @JvmField var wxList = ArrayList<WxSymbol>()
        @JvmField var turbulenceConditions = ArrayList<TurbulenceCondition>()
        @JvmField var icingConditions = ArrayList<IcingCondition>()
        @JvmField var remarks = ""
    }

    @JvmField var fetchTime = 0L
    @JvmField var stationId = ""
    @JvmField var entries = ArrayList<PirepEntry>()
}
