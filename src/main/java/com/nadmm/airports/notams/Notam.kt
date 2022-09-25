/*
 * FlightIntel for Pilots
 *
 * Copyright 2021 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.notams

import java.time.OffsetDateTime

class Notam {
    var id: String? = null
    var notamID: String? = null
    var series: String? = null
    var number = 0
    var year = 0
    var type: String? = null
    var category: String? = null
    var issued: OffsetDateTime? = null
    var lastUpdated: OffsetDateTime? = null
    var effectiveStart: OffsetDateTime? = null
    var effectiveEnd: OffsetDateTime? = null
    var estimatedEnd: String? = null
    var location: String? = null
    var affectedFIR: String? = null
    var selectionCode: String? = null
    var traffic: String? = null
    var purpose: String? = null
    var scope: String? = null
    var minimumFL = 0
    var maximumFL = 0
    var latitude: String? = null
    var longitude: String? = null
    var radius = 0
    var classification: String? = null
    var schedule: String? = null
    var xovernotamID: String? = null
    var text: String? = null
}