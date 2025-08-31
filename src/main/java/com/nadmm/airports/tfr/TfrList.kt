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
package com.nadmm.airports.tfr

import android.location.Location
import com.nadmm.airports.ActivityBase
import com.nadmm.airports.utils.FormatUtils.formatFeetAgl
import com.nadmm.airports.utils.FormatUtils.formatFeetMsl
import com.nadmm.airports.utils.TimeUtils
import java.io.Serializable
import java.util.Date

class TfrList : Serializable {
    var isValid = false
    var fetchTime: Long = Long.MAX_VALUE
    var entries: ArrayList<Tfr> = ArrayList()

    enum class AltitudeType(val description: String) {
        AGL("AGL"),
        MSL("MSL"),
        Unknown("???");

        override fun toString() = description
    }

    class Tfr : Serializable, Comparable<Tfr> {
        var notamId: String? = null
        var name: String? = null
        var city: String? = null
        var state: String? = null
        var facility: String? = null
        var facilityType: String? = null
        var type: String? = null
        var text: String? = null
        var minAltitudeFeet: Int = Int.MAX_VALUE
        var minAltitudeType: AltitudeType = AltitudeType.Unknown
        var maxAltitudeFeet: Int = Int.MAX_VALUE
        var maxAltitudeType: AltitudeType = AltitudeType.Unknown
        var location: Location? = null
        var createTime: Long = Long.MAX_VALUE
        var modifyTime: Long = Long.MAX_VALUE
        var activeTime: Long = Long.MAX_VALUE
        var expireTime: Long = Long.MAX_VALUE

        fun formatAltitudeRange(): String {
            val sb = StringBuilder()
            if (minAltitudeFeet < Int.MAX_VALUE && maxAltitudeFeet < Int.MAX_VALUE && maxAltitudeFeet > 0) {
                if (minAltitudeFeet == 0) {
                    sb.append("Surface")
                } else {
                    sb.append(formatAltitude(minAltitudeFeet, minAltitudeType))
                }
                sb.append(" up to ")
                if (maxAltitudeFeet >= 91000) {
                    sb.append("unlimited")
                } else {
                    sb.append(formatAltitude(maxAltitudeFeet, maxAltitudeType))
                }
            } else if (minAltitudeFeet < Int.MAX_VALUE
                && maxAltitudeFeet == Int.MAX_VALUE
            ) {
                if (minAltitudeFeet == 0) {
                    sb.append("Surface")
                } else {
                    sb.append(formatAltitude(minAltitudeFeet, minAltitudeType))
                }
                sb.append(" and above")
            } else if (minAltitudeFeet == Int.MAX_VALUE
                && maxAltitudeFeet < Int.MAX_VALUE
            ) {
                sb.append(formatAltitude(maxAltitudeFeet, maxAltitudeType))
                sb.append(" and below")
            } else {
                sb.append("Altitude not specified")
            }
            return sb.toString()
        }

        private fun formatAltitude(altitude: Int, type: AltitudeType): String {
            return if (type === AltitudeType.AGL) {
                formatFeetAgl(altitude.toFloat())
            } else {
                formatFeetMsl(altitude.toFloat())
            }
        }

        fun formatTimeRange(context: ActivityBase?): String {
            val sb = StringBuilder()
            if (activeTime < Long.MAX_VALUE && expireTime < Long.MAX_VALUE) {
                sb.append(TimeUtils.formatDateRange(context, activeTime, expireTime))
            } else if (activeTime < Long.MAX_VALUE) {
                sb.append(TimeUtils.formatDateTimeYear(context, activeTime))
                sb.append(" onwards")
            } else {
                sb.append("Until further notice")
            }
            return sb.toString()
        }

        val isActive: Boolean
            get() {
                val now = Date().time
                return if (activeTime < Long.MAX_VALUE) {
                    now in activeTime until expireTime
                } else {
                    true
                }
            }
        val isExpired: Boolean
            get() {
                val now = Date().time
                return if (expireTime < Long.MAX_VALUE) {
                    now >= expireTime
                } else {
                    false
                }
            }

        fun formatLocation(): String {
            val location = StringBuilder()
            if (city != null) {
                location.append(city)
                location.append(", ")
            }
            location.append(state)
            return location.toString()
        }

        override fun compareTo(other: Tfr): Int {
            return other.modifyTime.compareTo(modifyTime)
        }
    }
}