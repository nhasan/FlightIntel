/*
 * FlightIntel for Pilots
 *
 * Copyright 2016-2025 Nadeem Hasan <nhasan@nadmm.com>
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

object ClassBUtils {
    private val classBNames = HashMap<String, String>()
    private val classBFileNames = HashMap<String, String>()
    var classBFacilityList: String = ""
        private set

    init {
        classBNames.put("ATL", "Atlanta")
        classBNames.put("BOS", "Boston")
        classBNames.put("CLT", "Charlotte")
        classBNames.put("ORD", "Chicago")
        classBNames.put("MDW", "Chicago")
        classBNames.put("CVG", "Cincinnati")
        classBNames.put("CLE", "Cleveland")
        classBNames.put("DFW", "Dallas-Ft. Worth")
        classBNames.put("DEN", "Denver")
        classBNames.put("DTW", "Detroit")
        classBNames.put("HNL", "Honolulu")
        classBNames.put("IAH", "Houston")
        classBNames.put("MCI", "Kansas City")
        classBNames.put("LAS", "Las Vegas")
        classBNames.put("LAX", "Los Angeles")
        classBNames.put("MEM", "Memphis")
        classBNames.put("MIA", "Miami")
        classBNames.put("MSP", "Minneapolis")
        classBNames.put("MSY", "New Orleans")
        classBNames.put("EWR", "New York")
        classBNames.put("JFK", "New York")
        classBNames.put("LGA", "New York")
        classBNames.put("MCO", "Orlando")
        classBNames.put("PHL", "Philadelphia")
        classBNames.put("PHX", "Phoenix")
        classBNames.put("PIT", "Pittsburgh")
        classBNames.put("STL", "St. Louis")
        classBNames.put("SLC", "Salt Lake City")
        classBNames.put("SAN", "San Diego")
        classBNames.put("NKX", "San Diego")
        classBNames.put("SFO", "San Francisco")
        classBNames.put("SEA", "Seattle")
        classBNames.put("TPA", "Tampa")
        classBNames.put("ADW", "Washington Tri-Area")
        classBNames.put("BWI", "Washington Tri-Area")
        classBNames.put("DCA", "Washington Tri-Area")
        classBNames.put("IAD", "Washington Tri-Area")

        classBFileNames.put("ATL", "Atlanta_Class_B.pdf")
        classBFileNames.put("BOS", "Boston_Class_B.pdf")
        classBFileNames.put("CLT", "Charlotte_Class_B.pdf")
        classBFileNames.put("ORD", "Chicago_Class_B.pdf")
        classBFileNames.put("MDW", "Chicago_Class_B.pdf")
        classBFileNames.put("CVG", "Cincinnati_Class_B.pdf")
        classBFileNames.put("CLE", "Cleveland_Class_B.pdf")
        classBFileNames.put("DFW", "Dallas-Ft_Worth_Class_B.pdf")
        classBFileNames.put("DEN", "Denver_Class_B.pdf")
        classBFileNames.put("DTW", "Detroit_Class_B.pdf")
        classBFileNames.put("HNL", "Honolulu_Class_B.pdf")
        classBFileNames.put("IAH", "Houston_Class_B.pdf")
        classBFileNames.put("MCI", "Kansas_City_Class_B.pdf")
        classBFileNames.put("LAS", "Las_Vegas_Class_B.pdf")
        classBFileNames.put("LAX", "Los_Angeles_Class_B.pdf")
        classBFileNames.put("MEM", "Memphis_Class_B.pdf")
        classBFileNames.put("MIA", "Miami_Class_B.pdf")
        classBFileNames.put("MSP", "Minneapolis_Class_B.pdf")
        classBFileNames.put("MSY", "New_Orleans_Class_B.pdf")
        classBFileNames.put("EWR", "New_York_Class_B.pdf")
        classBFileNames.put("JFK", "New_York_Class_B.pdf")
        classBFileNames.put("LGA", "New_York_Class_B.pdf")
        classBFileNames.put("MCO", "Orlando_Class_B.pdf")
        classBFileNames.put("PHL", "Philadelphia_Class_B.pdf")
        classBFileNames.put("PHX", "Phoenix_Class_B.pdf")
        classBFileNames.put("PIT", "Pittsburgh_Class_B.pdf")
        classBFileNames.put("STL", "St._Louis_Class_B.pdf")
        classBFileNames.put("SLC", "Salt_Lake_Class_B.pdf")
        classBFileNames.put("SAN", "San_Diego_Class_B.pdf")
        classBFileNames.put("NKX", "San_Diego_Class_B.pdf")
        classBFileNames.put("SFO", "San_Francisco_Class_B.pdf")
        classBFileNames.put("SEA", "Seattle_Class_B.pdf")
        classBFileNames.put("TPA", "Tampa_Class_B.pdf")
        classBFileNames.put("ADW", "Washington_Tri-Area_Class_B.pdf")
        classBFileNames.put("BWI", "Washington_Tri-Area_Class_B.pdf")
        classBFileNames.put("DCA", "Washington_Tri-Area_Class_B.pdf")
        classBFileNames.put("IAD", "Washington_Tri-Area_Class_B.pdf")

        classBFacilityList = classBNames.keys.joinToString(separator=", ") { "'$it'" }
    }

    fun getClassBName(facility: String?): String? {
        return classBNames[facility]
    }

    fun getClassBFilename(facility: String?): String? {
        return classBFileNames[facility]
    }
}
