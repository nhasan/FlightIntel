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

import java.io.Serializable

class AirSigmet : Serializable {
    @JvmField
    var isValid = false
    @JvmField
    var fetchTime: Long = Long.MAX_VALUE
    @JvmField
    var entries: ArrayList<AirSigmetEntry> = ArrayList()

    class AirSigmetPoint : Serializable {
        @JvmField
        var latitude: Float = Float.MAX_VALUE
        @JvmField
        var longitude: Float = Float.MAX_VALUE

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    class AirSigmetEntry : Serializable {
        @JvmField
        var rawText: String? = null
        @JvmField
        var fromTime: Long = Long.MAX_VALUE
        @JvmField
        var toTime: Long = Long.MAX_VALUE
        @JvmField
        var minAltitudeFeet: Int = Int.MAX_VALUE
        @JvmField
        var maxAltitudeFeet: Int = Int.MAX_VALUE
        @JvmField
        var movementDirDegrees: Int = Int.MAX_VALUE
        @JvmField
        var movementSpeedKnots: Int = Int.MAX_VALUE
        @JvmField
        var hazardType: String? = null
        @JvmField
        var hazardSeverity: String? = null
        @JvmField
        var type: String? = null
        @JvmField
        var points: ArrayList<AirSigmetPoint> = ArrayList()

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}