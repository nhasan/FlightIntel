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

package com.nadmm.airports.notams;

import java.time.OffsetDateTime;

public class Notam {
    String id;
    String notamID;
    String series;
    int number;
    int year;
    String type;
    String category;
    OffsetDateTime issued;
    OffsetDateTime lastUpdated;
    OffsetDateTime effectiveStart;
    OffsetDateTime effectiveEnd;
    String estimatedEnd;
    String location;
    String affectedFIR;
    String selectionCode;
    String traffic;
    String purpose;
    String scope;
    int minimumFL;
    int maximumFL;
    String latitude;
    String longitude;
    int radius;
    String classification;
    String schedule;
    String xovernotamID;
    String text;
}
