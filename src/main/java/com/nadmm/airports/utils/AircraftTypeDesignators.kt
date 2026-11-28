/*
 * FlightIntel for Pilots
 *
 * Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.utils

/**
 * Names for ICAO aircraft type designators, such as "Cessna, 172" for
 * C172, as PIREPs report them. They come from ICAO Doc 8643 through
 * tool/aircraft/update_aircraft_types.py, which writes aircraft_types.tsv,
 * read on first use.
 */
object AircraftTypeDesignators {
    private const val RESOURCE = "/aircraft_types.tsv"

    private val names: Map<String, String> by lazy {
        javaClass.getResourceAsStream(RESOURCE)?.bufferedReader()?.useLines { lines ->
            lines.mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab > 0) line.substring(0, tab) to line.substring(tab + 1) else null
            }.toMap()
        } ?: emptyMap()
    }

    /** The aircraft's name, or empty when the designator is not known. */
    fun getAircraftName(designator: String): String =
        names[designator.trim().uppercase()] ?: ""
}
