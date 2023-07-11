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
package com.nadmm.airports.utils

import kotlin.math.roundToInt

object DataUtils {
    private const val DOT = "\u2022"
    private const val DASH = "\u2013"
    private val sMorseCodes = HashMap<String, String>()
    private val sAtcIdToName: MutableMap<String, String> = HashMap()
    private val sAtcNameToId: MutableMap<String, String> = HashMap()
    private val sDatisApts: MutableSet<String> = HashSet()
    private val sPhoneticAlphabets: MutableMap<String, String> = HashMap()

    fun decodePhoneNumber(phone: String): String {
        var i = 0
        val builder = StringBuilder()
        while (i < phone.length) {
            val c = phone[i++]
            if ("ABC".indexOf(c) >= 0) {
                builder.append('2')
            } else if ("DEF".indexOf(c) >= 0) {
                builder.append('3')
            } else if ("GHI".indexOf(c) >= 0) {
                builder.append('4')
            } else if ("JKL".indexOf(c) >= 0) {
                builder.append('5')
            } else if ("MNO".indexOf(c) >= 0) {
                builder.append('6')
            } else if ("PQRS".indexOf(c) >= 0) {
                builder.append('7')
            } else if ("TUV".indexOf(c) >= 0) {
                builder.append('8')
            } else if ("WXYZ".indexOf(c) >= 0) {
                builder.append('9')
            } else {
                builder.append(c)
            }
        }
        return builder.toString()
    }

    @JvmStatic
    fun getMorseCode(text: String): String {
        val morseCode = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (morseCode.isNotEmpty()) {
                morseCode.append("  ")
            }
            morseCode.append(sMorseCodes[text.substring(i, i + 1)])
            ++i
        }
        return morseCode.toString()
    }

    fun calculateMagneticHeading(trueHeading: Int, variation: Int): Int {
        var magneticHeading = (trueHeading + variation + 360) % 360
        if (magneticHeading == 0) {
            magneticHeading = 360
        }
        return magneticHeading
    }

    @JvmStatic
    fun calculateRadial(dir: Float, variance: Int): Int {
        var heading = (dir + 360).roundToInt() % 360
        heading = calculateMagneticHeading(heading, variance)
        return (heading + 180) % 360
    }

    fun decodeLandingFacilityType(siteNumber: String): String {
        return when (siteNumber[siteNumber.length - 1]) {
            'A' -> "Airport"
            'B' -> "Balloonport"
            'C' -> "Seaplane base"
            'G' -> "Gliderport"
            'H' -> "Heliport"
            'S' -> "STOLport"
            'U' -> "Ultralight park"
            else -> ""
        }
    }

    @JvmStatic
    fun decodeOwnershipType(ownership: String): String {
        return when (ownership) {
            "PU" -> "Public owned"
            "PR" -> "Private owned"
            "MA" -> "Airforce owned"
            "MN" -> "Navy owned"
            "MR" -> "Army owned"
            else -> "Unknown ownership"
        }
    }

    @JvmStatic
    fun decodeFacilityUse(use: String): String {
        return when (use) {
            "PU" -> "Public use"
            "PR" -> "Private Use"
            else -> "Unknown"
        }
    }

    fun decodeFuelTypes(fuelTypes: String): String {
        val decodedFuel = StringBuilder()
        var start = 0
        while (start < fuelTypes.length) {
            val end = (start + 5).coerceAtMost(fuelTypes.length)
            val type = fuelTypes.substring(start, end).trim { it <= ' ' }
            if (decodedFuel.isNotEmpty()) {
                decodedFuel.append(", ")
            }
            when (type) {
                "A" -> decodedFuel.append("JET-A")
                "A+" -> decodedFuel.append("JET-A+")
                "A1" -> decodedFuel.append("JET-A1")
                "A1+" -> decodedFuel.append("JET-A1+")
                "B" -> decodedFuel.append("JET-B")
                "B+" -> decodedFuel.append("JET-B+")
                "MOGAS" -> decodedFuel.append("MOGAS")
                else -> decodedFuel.append(type)
            }
            start = end
        }
        return decodedFuel.toString()
    }

    fun decodeStatus(status: String): String {
        return when (status) {
            "O" -> "Operational"
            "CI" -> "Closed Indefinitely"
            "CP" -> "Closed Permanently"
            else -> "Unknown"
        }
    }

    fun decodeStorage(s: String): ArrayList<String> {
        val storages = ArrayList<String>()
        var start = 0
        while (start < s.length) {
            var end = s.indexOf(",", start)
            if (end == -1) {
                end = s.length
            }
            when (val type = s.substring(start, end).trim { it <= ' ' }) {
                "BUOY" -> storages.add("Buoy")
                "HGR" -> storages.add("Hangar")
                "TIE" -> storages.add("Tiedown")
                else -> storages.add(type)
            }
            start = end + 1
        }
        return storages
    }

    fun decodeServices(s: String): ArrayList<String> {
        val services = ArrayList<String>()
        var start = 0
        while (start < s.length) {
            var end = s.indexOf(",", start)
            if (end == -1) {
                end = s.length
            }
            when (val type = s.substring(start, end).trim { it <= ' ' }) {
                "AFRT" -> services.add("Air freight")
                "AGRI" -> services.add("Crop dusting")
                "AMB" -> services.add("Air ambulance")
                "AVNCS" -> services.add("Avionics")
                "BCHGR" -> services.add("Beaching gear")
                "CARGO" -> services.add("Cargo")
                "CHTR" -> services.add("Charter")
                "GLD" -> services.add("Glider")
                "INSTR" -> services.add("Flight instruction")
                "PAJA" -> services.add("Parachute jump activity")
                "RNTL" -> services.add("Rental")
                "SALES" -> services.add("Sales")
                "SURV" -> services.add("Survey")
                "TOW" -> services.add("Glider towing")
                else -> services.add(type)
            }
            start = end + 1
        }
        return services
    }

    fun decodeSurfaceType(surfaceType: String): String {
        val decodedSurfaceType = StringBuilder()
        var start = 0
        while (start < surfaceType.length) {
            var end = surfaceType.indexOf("-", start)
            if (end == -1) {
                end = surfaceType.length
            }
            val type = surfaceType.substring(start, end).trim { it <= ' ' }
            if (decodedSurfaceType.isNotEmpty()) {
                decodedSurfaceType.append(", ")
            }
            when (type) {
                "CONC" -> decodedSurfaceType.append("Concrete")
                "ASPH" -> decodedSurfaceType.append("Asphalt")
                "SNOW" -> decodedSurfaceType.append("Snow")
                "ICE" -> decodedSurfaceType.append("Ice")
                "MATS" -> decodedSurfaceType.append("Landing mats")
                "TREATED" -> decodedSurfaceType.append("Treated")
                "GRAVEL" -> decodedSurfaceType.append("Gravel")
                "TURF" -> decodedSurfaceType.append("Grass")
                "DIRT" -> decodedSurfaceType.append("Soil")
                "WATER" -> decodedSurfaceType.append("Water")
                "E" -> decodedSurfaceType.append("Excellent")
                "G" -> decodedSurfaceType.append("Good")
                "F" -> decodedSurfaceType.append("Fair")
                "P" -> decodedSurfaceType.append("Poor")
                "L" -> decodedSurfaceType.append("Failed")
            }
            start = end + 1
        }
        return decodedSurfaceType.toString()
    }

    fun decodeSurfaceTreatment(treatment: String): String {
        return when (treatment) {
            "GRVD" -> "Grooved"
            "PFC" -> "Porous friction course"
            "AFSC" -> "Aggregate friction seal coat"
            "RFSC" -> "Rubberized friction seal coat"
            "WC" -> "Wire comb"
            else -> "None"
        }
    }

    fun decodeWindIndicator(windIndicator: String): String {
        return when (windIndicator) {
            "Y-L" -> "Lighted"
            "Y" -> "Unlighted"
            else -> "No"
        }
    }

    fun decodeBeacon(beacon: String): String {
        return when (beacon) {
            "CG" -> "White-green"
            "CY" -> "White-yellow"
            "CGY" -> "White-Green-Yellow"
            "SCG" -> "Split-white-green"
            "C" -> "White"
            "Y" -> "Yellow"
            "G" -> "Green"
            else -> "No"
        }
    }

    fun decodeRunwayEdgeLights(edgeLights: String): String {
        return when (edgeLights) {
            "HIGH" -> "High intensity"
            "MED" -> "Medium intensity"
            "LOW" -> "Low intensity"
            "NSTD" -> "Non-standard"
            "PERI" -> "Perimeter"
            "STRB" -> "Strobe"
            "FLD" -> "Flood"
            else -> "None"
        }
    }

    fun decodeRunwayMarking(marking: String): String {
        return when (marking) {
            "PIR" -> "Precision"
            "NPI" -> "Non-precision"
            "BSC" -> "Basic"
            "NRS" -> "Numbers only"
            "NSTD" -> "Non-standard"
            "BUOY" -> "Buoys"
            "STOL" -> "STOL"
            else -> "None"
        }
    }

    fun decodeRunwayMarkingCondition(condition: String): String {
        return when (condition) {
            "G" -> "Good"
            "F" -> "Fair"
            "P" -> "Poor"
            else -> ""
        }
    }

    fun decodeGlideSlope(glideSlope: String): String {
        return when (glideSlope) {
            "S2L" -> "2-box SAVASI on left side"
            "S2R" -> "2-box SAVASI on right side"
            "V2L" -> "2-box VASI on left side"
            "V2R" -> "2-box VASI on right side"
            "V4L" -> "4-box VASI on left side"
            "V4R" -> "4-box VASI on right side"
            "V6L" -> "6-box VASI on left side"
            "V6R" -> "6-box VASI on right side"
            "V12" -> "12-box VASI on both sides"
            "V16" -> "16-box VASI on both sides"
            "P2L" -> "2-light PAPI on left side"
            "P2R" -> "2-light PAPI on right side"
            "P4L" -> "4-light PAPI on left side"
            "P4R" -> "4-light PAPI on right side"
            "NSTD" -> "Non-standars VASI"
            "PVT" -> "Private use only"
            "VAS" -> "Non-specific VASI"
            "NONE" -> "None"
            "N" -> "None"
            "TRIL" -> "Tri-color VASI on left side"
            "TRIR" -> "Tri-color VASI on right side"
            "PSIL" -> "Pulsating VASI on left side"
            "PSIR" -> "Pulsating VASI on right side"
            "PNIL" -> "Panel system on left side"
            "PNIR" -> "Panel system on right side"
            else -> glideSlope
        }
    }

    fun decodeControllingObjectLighted(lighted: String): String {
        return when (lighted) {
            "M" -> "MARKED"
            "L" -> "LIGHTED"
            "ML" -> "MARKED & LIGHTED"
            else -> lighted
        }
    }

    fun decodeControllingObjectOffset(offset: String): Int {
        var end = 0
        while (end < offset.length && offset[end] >= '0' && offset[end] <= '9') {
            ++end
        }
        return try {
            offset.substring(0, end).toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }

    fun decodeControllingObjectOffsetDirection(offset: String): String {
        var end = 0
        while (end < offset.length && offset[end] >= '0' && offset[end] <= '9') {
            ++end
        }
        return when (offset.substring(end)) {
            "R" -> "RIGHT"
            "L" -> "LEFT"
            "B", "L/R" -> "BOTH SIDES"
            else -> "VICINITY"
        }
    }

    fun decodeRVRLocations(rvr: String): String {
        var decodedRvr = ""
        var index = 0
        while (index < rvr.length) {
            if (decodedRvr.isNotEmpty()) {
                decodedRvr += ", "
            }
            if (rvr[index] == 'T') {
                decodedRvr += "TOUCHDOWN"
            } else if (rvr[index] == 'M') {
                decodedRvr += "MIDPOINT"
            } else if (rvr[index] == 'R') {
                decodedRvr += "ROLLOUT"
            }
            ++index
        }
        return decodedRvr
    }

    @JvmStatic
    fun decodeNavProtectedAltitude(alt: String): String {
        return when (alt) {
            "T" -> "Terminal (25NM)"
            "L" -> "Low altitude (40NM)"
            "H" -> "High altitude (130NM)"
            else -> alt
        }
    }

    fun decodeArtcc(artcc: String): String {
        return when (artcc) {
            "ZAB" -> "Albuquerque Center"
            "ZAN" -> "Anchorage Center"
            "ZAP" -> "Anchorage Oceanic"
            "ZAU" -> "Chicago Center"
            "ZBW" -> "Boston Center"
            "ZDC" -> "Washington Center"
            "ZDV" -> "Denver Center"
            "ZFW" -> "Fort Worth Center"
            "ZHN" -> "Honolulu Control Facility"
            "ZHU" -> "Houston Center"
            "ZID" -> "Indianapolis Center"
            "ZJX" -> "Jacksonville Center"
            "ZKC" -> "Kansas City Center"
            "ZLA" -> "Los Angeles Center"
            "ZLC" -> "Salt Lake City Center"
            "ZMA" -> "Miami Center"
            "ZME" -> "Memphis Center"
            "ZMP" -> "Minneapolis Center"
            "ZNY" -> "New York Center"
            "ZOA" -> "Oakland Center"
            "ZOB" -> "Cleveland Center"
            "ZSE" -> "Seattle Center"
            "ZSU" -> "San Juan Center"
            "ZTL" -> "Atlanta Center"
            "ZUA" -> "Guam Center"
            else -> ""
        }

    }

    fun decodeFaaRegion(code: String): String {
        return when (code) {
            "AAL" -> "Alaska"
            "ACE" -> "Central"
            "AEA" -> "Eastern"
            "AGL" -> "Great Lakes"
            "AIN" -> "International"
            "ANE" -> "New England"
            "ANM" -> "Northwest Mountain"
            "ASO" -> "Southern"
            "ASW" -> "Southwest"
            "AWP" -> "Western Pacific"
            else -> ""
        }

    }

    fun decodeAirspace(airspace: String): String {
        var value = ""
        if (airspace[0] == 'Y') {
            value += "Class B"
        }
        if (airspace[1] == 'Y') {
            if (value.isNotEmpty()) {
                value += ", "
            }
            value += "Class C"
        }
        if (airspace[2] == 'Y') {
            if (value.isNotEmpty()) {
                value += ", "
            }
            value += "Class D"
        }
        if (airspace[3] == 'Y') {
            if (value.isNotEmpty()) {
                value += ", "
            }
            value += "Class E"
        }
        return value
    }

    @JvmStatic
    fun decodeChartCode(chartCode: String): String {
        return if (chartCode.equals("APD", ignoreCase = true)) {
            "Airport Diagram (APD)"
        } else if (chartCode.equals("MIN", ignoreCase = true)) {
            "Minimums (MIN)"
        } else if (chartCode.equals("STAR", ignoreCase = true)) {
            "Terminal Arrivals (STAR)"
        } else if (chartCode.equals("IAP", ignoreCase = true)) {
            "Approach Procedures (IAP)"
        } else if (chartCode.equals("DP", ignoreCase = true)) {
            "Departure Procedures (DP)"
        } else if (chartCode.equals("DPO", ignoreCase = true)) {
            "Obstacle Departures (DPO)"
        } else if (chartCode.equals("LAH", ignoreCase = true)) {
            "LAHSO Operations (LAH)"
        } else if (chartCode.equals("HOT", ignoreCase = true)) {
            "Airport Hot Spots (HOT)"
        } else {
            "Other"
        }
    }

    @JvmStatic
    fun decodeUserAction(userAction: String?): String {
        if (!userAction.isNullOrEmpty()) {
            return when (userAction[0]) {
                'C' -> "Changed"
                'A' -> "Added"
                'D' -> "Deleted"
                else -> ""
            }
        }
        return ""
    }

    @JvmStatic
    fun isDirectionalNavaid(type: String): Boolean {
        return type == "VOR" || type == "VOR/DME" || type == "DME"
                || type == "VORTAC" || type == "VOT" || type == "TACAN"
    }

    @JvmStatic
    fun getTacanChannelFrequency(channel: String): Double {
        var freq = 0.0
        if (channel.isNotEmpty()) {
            val type = channel.substring(channel.length - 1)
            val num = Integer.valueOf(channel.substring(0, channel.length - 1))
            var offset = num.toDouble() / 10
            if (type == "Y") {
                offset += 0.05
            }
            if (num in 17..59) {
                freq = 106.3 + offset
            } else if (num in 70..126) {
                freq = 105.3 + offset
            }
        }
        return freq
    }

    fun getApproachLightSystemDescription(als: String): String {
        return when (als) {
            "ALSAF" -> "3,000 ft high intst apch lighting system with cntrln sequenced flashers"
            "ALSF1" -> "std 2,400 ft high intst apch lighting system with sequenced flashers, CAT I"
            "ALSF2" -> "std 2,400 ft high intst apch lighting system with sequenced flashers, CAT II"
            "MALS" -> "1,400 ft med intst apch lighting system"
            "MALSF" -> "1,400 ft med intst apch lighting system with sequenced flashers"
            "MALSR" -> "1,400 ft med intst apch lighting system with rwy alignment indicator lights"
            "SSALS" -> "Simplified short apch lighting system"
            "SSALF" -> "Simplified short apch lighting system with sequenced flashers"
            "SSALR" -> "Simplified short apch lighting system with rwy alignment indicator lights"
            "NEON" -> "Neon ladder apch lighting system"
            "ODALS" -> "Omni-directional apch lighting system"
            "LDIN" -> "Lead-in apch lighting system"
            "MIL OVRN" -> "Military overrun apch lighting system"
            else -> ""
        }
    }

    // Defines the order in which NOTAMs will be displayed by subject
    val notamSubjects: Array<String>
        get() =// Defines the order in which NOTAMs will be displayed by subject
            arrayOf(
                "Aerodrome",
                "Obstructions",
                "Movement and Landing Area",
                "Navigation",
                "Communications",
                "Airspace",
                "Services",
                "Flight Data Center",
                "Other"
            )

    fun getNotamSubjectFromKeyword(keyword: String): String {
        return when (keyword) {
            "RWY" -> {
                "Movement and Landing Area"
            }
            "TWY" -> {
                "Movement and Landing Area"
            }
            "APRON" -> {
                "Movement and Landing Area"
            }
            "RAMP" -> {
                "Movement and Landing Area"
            }
            "AD" -> {
                "Aerodrome"
            }
            "AIRSPACE" -> {
                "Airspace"
            }
            "OBST" -> {
                "Obstructions"
            }
            "NAV" -> {
                "Navigation"
            }
            "COM" -> {
                "Communications"
            }
            "SVC" -> {
                "Services"
            }
            "FDC" -> {
                "Flight Data Center"
            }
            else -> {
                "Other"
            }
        }
    }

    fun getAtcFacilityId(name: String): Array<String>? {
        return if (sAtcNameToId.containsKey(name)) sAtcNameToId[name]!!
            .split(":").toTypedArray() else null
    }

    fun getAtcFacilityName(id: String): String? {
        return sAtcIdToName[id]
    }

    fun metersToFeet(meters: Int): Int {
        return (3.2808 * meters).roundToInt()
    }

    fun getRunwayHeading(runwayId: String): Int {
        var index = 0
        while (index < runwayId.length) {
            if (!Character.isDigit(runwayId[index])) {
                break
            }
            ++index
        }
        var heading = 0
        try {
            heading = Integer.valueOf(runwayId.substring(0, index)) * 10
        } catch (ignored: Exception) {
        }
        return heading
    }

    fun isDatisAvailable(icaoCode: String): Boolean {
        return sDatisApts.contains(icaoCode)
    }

    fun getPhoneticAlphabet(letter: String): String {
        return sPhoneticAlphabets.getOrDefault(letter, "")
    }

    init {
        sMorseCodes["A"] = DOT + DASH
        sMorseCodes["B"] =
            DASH + DOT + DOT + DOT
        sMorseCodes["C"] =
            DASH + DOT + DASH + DOT
        sMorseCodes["D"] =
            DASH + DOT + DOT
        sMorseCodes["E"] = DOT
        sMorseCodes["F"] =
            DOT + DOT + DASH + DOT
        sMorseCodes["G"] =
            DASH + DASH + DOT
        sMorseCodes["H"] =
            DOT + DOT + DOT + DOT
        sMorseCodes["I"] =
            DOT + DOT
        sMorseCodes["J"] =
            DOT + DASH + DASH + DASH
        sMorseCodes["K"] =
            DASH + DOT + DASH
        sMorseCodes["L"] =
            DOT + DASH + DOT + DOT
        sMorseCodes["M"] = DASH + DASH
        sMorseCodes["N"] =
            DASH + DOT
        sMorseCodes["O"] =
            DASH + DASH + DASH
        sMorseCodes["P"] =
            DOT + DASH + DASH + DOT
        sMorseCodes["Q"] =
            DASH + DASH + DOT + DASH
        sMorseCodes["R"] =
            DOT + DASH + DOT
        sMorseCodes["S"] =
            DOT + DOT + DOT
        sMorseCodes["T"] = DASH
        sMorseCodes["U"] =
            DOT + DOT + DASH
        sMorseCodes["V"] =
            DOT + DOT + DOT + DASH
        sMorseCodes["W"] =
            DOT + DASH + DASH
        sMorseCodes["X"] =
            DASH + DOT + DOT + DASH
        sMorseCodes["Y"] =
            DASH + DOT + DASH + DASH
        sMorseCodes["Z"] =
            DASH + DASH + DOT + DOT
        sMorseCodes["1"] =
            DOT + DASH + DASH + DASH + DASH
        sMorseCodes["2"] =
            DOT + DOT + DASH + DASH + DASH
        sMorseCodes["3"] =
            DOT + DOT + DOT + DASH + DASH
        sMorseCodes["4"] =
            DOT + DOT + DOT + DOT + DASH
        sMorseCodes["5"] =
            DOT + DOT + DOT + DOT + DOT
        sMorseCodes["6"] =
            DASH + DOT + DOT + DOT + DOT
        sMorseCodes["7"] =
            DASH + DASH + DOT + DOT + DOT
        sMorseCodes["8"] =
            DASH + DASH + DASH + DOT + DOT
        sMorseCodes["9"] =
            DASH + DASH + DASH + DASH + DOT
        sMorseCodes["0"] =
            DASH + DASH + DASH + DASH + DASH
    }

    init {
        sAtcIdToName["A11:TRACON"] = "Anchorage"
        sAtcIdToName["A80:TRACON"] = "Atlanta"
        sAtcIdToName["A90:TRACON"] = "Boston"
        sAtcIdToName["C90:TRACON"] = "Chicago"
        sAtcIdToName["D01:TRACON"] = "Denver"
        sAtcIdToName["D10:TRACON"] = "Dallas-Ft Worth"
        sAtcIdToName["D21:TRACON"] = "Detroit"
        sAtcIdToName["F11:TRACON"] = "Central Florida"
        sAtcIdToName["I90:TRACON"] = "Houston"
        sAtcIdToName["K90:TRACON"] = "Cape"
        sAtcIdToName["L30:TRACON"] = "Las Vegas"
        sAtcIdToName["M03:TRACON"] = "Memphis"
        sAtcIdToName["M98:TRACON"] = "Minneapolis"
        sAtcIdToName["N90:TRACON"] = "New York"
        sAtcIdToName["NCT:TRACON"] = "Norcal"
        sAtcIdToName["NMM:TRACON"] = "Meridian"
        sAtcIdToName["P31:TRACON"] = "Pensacola"
        sAtcIdToName["P50:TRACON"] = "Phoenix"
        sAtcIdToName["P80:TRACON"] = "Portland"
        sAtcIdToName["PCT:TRACON"] = "Potomac"
        sAtcIdToName["R90:TRACON"] = "Omaha"
        sAtcIdToName["S46:TRACON"] = "Seattle"
        sAtcIdToName["S56:TRACON"] = "Salt Lake City"
        sAtcIdToName["SCT:TRACON"] = "Socal"
        sAtcIdToName["T75:TRACON"] = "St Louis"
        sAtcIdToName["U90:TRACON"] = "Tucson"
        sAtcIdToName["Y90:TRACON"] = "Yankee"
        sAtcIdToName["BOI:TRACON"] = "Big Sky"
        sAtcIdToName["BTR:TRACON"] = "Baton Rouge"
        sAtcIdToName["FSM:TRACON"] = "Razorback"
        sAtcIdToName["IND:TRACON"] = "Indianapolis"
        sAtcIdToName["LIT:TRACON"] = "Little Rock"
        sAtcIdToName["MCI:TRACON"] = "Kansas City"
        sAtcIdToName["MDT:TRACON"] = "Harrisburg"
        sAtcIdToName["MGM:TRACON"] = "Montgomery"
        sAtcIdToName["OKC:TRACON"] = "Oke City"
        sAtcIdToName["PSC:TRACON"] = "Chinook"
        sAtcIdToName["ABE:ATCT"] = "Allentown"
        sAtcIdToName["ABI:ATCT"] = "Abilene"
        sAtcIdToName["ABQ:ATCT"] = "Albuquerque"
        sAtcIdToName["ACT:ATCT"] = "Waco"
        sAtcIdToName["ACY:ATCT"] = "Atlantic City"
        sAtcIdToName["AGS:ATCT"] = "Augusta"
        sAtcIdToName["ALB:ATCT"] = "Albany"
        sAtcIdToName["ALO:ATCT"] = "Waterloo"
        sAtcIdToName["AMA:ATCT"] = "Amarillo"
        sAtcIdToName["ASE:ATCT"] = "Aspen"
        sAtcIdToName["AUS:ATCT"] = "Austin"
        sAtcIdToName["AVL:ATCT"] = "Asheville"
        sAtcIdToName["AVP:ATCT"] = "Wilkes-Barre"
        sAtcIdToName["AZO:ATCT"] = "Kalamazoo"
        sAtcIdToName["BFL:ATCT"] = "Bakersfield"
        sAtcIdToName["BGM:ATCT"] = "Binghamton"
        sAtcIdToName["BGR:ATCT"] = "Bangor"
        sAtcIdToName["BHM:ATCT"] = "Birmingham"
        sAtcIdToName["BIL:ATCT"] = "Billings"
        sAtcIdToName["BIS:ATCT"] = "Bismarck"
        sAtcIdToName["BNA:ATCT"] = "Nashville"
        sAtcIdToName["BOI:ATCT"] = "Boise"
        sAtcIdToName["BTR:ATCT"] = "Ryan"
        sAtcIdToName["BTV:ATCT"] = "Burlington"
        sAtcIdToName["BUF:ATCT"] = "Buffalo"
        sAtcIdToName["CAE:ATCT"] = "Columbia"
        sAtcIdToName["CAK:ATCT"] = "Akron-Canton"
        sAtcIdToName["CHA:ATCT"] = "Chattanooga"
        sAtcIdToName["CHS:ATCT"] = "Charlston"
        sAtcIdToName["CID:ATCT"] = "Cedar Rapids"
        sAtcIdToName["CKB:ATCT"] = "Clarksburg"
        sAtcIdToName["CLE:ATCT"] = "Cleveland"
        sAtcIdToName["CLT:ATCT"] = "Charlotte"
        sAtcIdToName["CMH:ATCT"] = "Columbus"
        sAtcIdToName["CMI:ATCT"] = "Champaign"
        sAtcIdToName["COS:ATCT"] = "Springs"
        sAtcIdToName["CPR:ATCT"] = "Casper"
        sAtcIdToName["CRP:ATCT"] = "Corpus Christi"
        sAtcIdToName["CRW:ATCT"] = "Charlston"
        sAtcIdToName["CVG:ATCT"] = "Cincinnati"
        sAtcIdToName["DAB:ATCT"] = "Daytona Beach"
        sAtcIdToName["DAY:ATCT"] = "Dayton"
        sAtcIdToName["DLH:ATCT"] = "Duluth"
        sAtcIdToName["DMA:ATCT"] = "D-M"
        sAtcIdToName["DSM:ATCT"] = "Des Moines"
        sAtcIdToName["ELM:ATCT"] = "Elmira"
        sAtcIdToName["ELP:ATCT"] = "El Paso"
        sAtcIdToName["ERI:ATCT"] = "Erie"
        sAtcIdToName["EUG:ATCT"] = "Eugene"
        sAtcIdToName["EVV:ATCT"] = "Evansville"
        sAtcIdToName["FAI:ATCT"] = "Fairbanks"
        sAtcIdToName["FAR:ATCT"] = "Fargo"
        sAtcIdToName["FAT:ATCT"] = "Fresno"
        sAtcIdToName["FAY:ATCT"] = "Fayetteville"
        sAtcIdToName["FLO:ATCT"] = "Florence"
        sAtcIdToName["FNT:ATCT"] = "Flint"
        sAtcIdToName["FSD:ATCT"] = "Sioux Falls"
        sAtcIdToName["FSM:ATCT"] = "Fort Smith"
        sAtcIdToName["FWA:ATCT"] = "Fort Wayne"
        sAtcIdToName["GEG:ATCT"] = "Spokane"
        sAtcIdToName["GGG:ATCT"] = "Eastex"
        sAtcIdToName["GPT:ATCT"] = "Gulfport"
        sAtcIdToName["GRB:ATCT"] = "Greenbay"
        sAtcIdToName["GRR:ATCT"] = "Grand Rapids"
        sAtcIdToName["GSO:ATCT"] = "Greensboro"
        sAtcIdToName["GSP:ATCT"] = "Greer"
        sAtcIdToName["GTF:ATCT"] = "Great Falls"
        sAtcIdToName["HLN:ATCT"] = "Helena"
        sAtcIdToName["HSV:ATCT"] = "Huntsville"
        sAtcIdToName["HTS:ATCT"] = "Huntington"
        sAtcIdToName["HUF:ATCT"] = "Hulman"
        sAtcIdToName["ICT:ATCT"] = "Wichita"
        sAtcIdToName["ILM:ATCT"] = "Wilmington"
        sAtcIdToName["IND:ATCT"] = "Indy"
        sAtcIdToName["ITO:ATCT"] = "Hilo"
        sAtcIdToName["JAN:ATCT"] = "Jackson"
        sAtcIdToName["JAX:ATCT"] = "Jacksonville"
        sAtcIdToName["LAN:ATCT"] = "Lansing"
        sAtcIdToName["LBB:ATCT"] = "Lubbock"
        sAtcIdToName["LCH:ATCT"] = "Lake Charles"
        sAtcIdToName["LEX:ATCT"] = "Lexington"
        sAtcIdToName["LFT:ATCT"] = "Lafayette"
        sAtcIdToName["LIT:ATCT"] = "Adams"
        sAtcIdToName["MAF:ATCT"] = "Midland"
        sAtcIdToName["MBS:ATCT"] = "Saginaw"
        sAtcIdToName["MCI:ATCT"] = "International"
        sAtcIdToName["MDT:ATCT"] = "Harrisburg Intl"
        sAtcIdToName["MFD:ATCT"] = "Mansfield"
        sAtcIdToName["MGM:ATCT"] = "Dannelly"
        sAtcIdToName["MIA:ATCT"] = "Miami"
        sAtcIdToName["MKE:ATCT"] = "Milwaukee"
        sAtcIdToName["MKG:ATCT"] = "Muskegon"
        sAtcIdToName["MLI:ATCT"] = "Quad City"
        sAtcIdToName["MLU:ATCT"] = "Monroe"
        sAtcIdToName["MOB:ATCT"] = "Mobile"
        sAtcIdToName["MSN:ATCT"] = "Madison"
        sAtcIdToName["MSY:ATCT"] = "New Orleans"
        sAtcIdToName["MWH:ATCT"] = "Grant County"
        sAtcIdToName["MYR:ATCT"] = "Myrtle Beach"
        sAtcIdToName["OKC:ATCT"] = "Rogers"
        sAtcIdToName["ORF:ATCT"] = "Norfolk"
        sAtcIdToName["PBI:ATCT"] = "Palm Beach"
        sAtcIdToName["PHL:ATCT"] = "Philadelphia"
        sAtcIdToName["PIA:ATCT"] = "Peoria"
        sAtcIdToName["PIT:ATCT"] = "Pittsburgh"
        sAtcIdToName["PSC:ATCT"] = "Tri-Cities"
        sAtcIdToName["PVD:ATCT"] = "Providence"
        sAtcIdToName["PWM:ATCT"] = "Portland"
        sAtcIdToName["RDG:ATCT"] = "Reading"
        sAtcIdToName["RDU:ATCT"] = "Raleigh-Durham"
        sAtcIdToName["RFD:ATCT"] = "Rockford"
        sAtcIdToName["RNO:ATCT"] = "Reno"
        sAtcIdToName["ROA:ATCT"] = "Roanoke"
        sAtcIdToName["ROC:ATCT"] = "Rochester"
        sAtcIdToName["ROW:ATCT"] = "Roswell"
        sAtcIdToName["RST:ATCT"] = "Rochester"
        sAtcIdToName["RSW:ATCT"] = "Fort Meyers"
        sAtcIdToName["SAT:ATCT"] = "San Antonio"
        sAtcIdToName["SAV:ATCT"] = "Savannah"
        sAtcIdToName["SBA:ATCT"] = "Santa Barbara"
        sAtcIdToName["SBN:ATCT"] = "South Bend"
        sAtcIdToName["SDF:ATCT"] = "Standiford"
        sAtcIdToName["SGF:ATCT"] = "Springfield"
        sAtcIdToName["SHV:ATCT"] = "Shreveport"
        sAtcIdToName["SPI:ATCT"] = "Springfield"
        sAtcIdToName["SUX:ATCT"] = "Sioux City"
        sAtcIdToName["SYR:ATCT"] = "Syracuse"
        sAtcIdToName["TLH:ATCT"] = "Tallahassee"
        sAtcIdToName["TOL:ATCT"] = "Toledo"
        sAtcIdToName["TPA:ATCT"] = "Tampa"
        sAtcIdToName["TRI:ATCT"] = "Tri City"
        sAtcIdToName["TUL:ATCT"] = "Tulsa"
        sAtcIdToName["TWF:ATCT"] = "Twin Falls"
        sAtcIdToName["TYS:ATCT"] = "Knoxville"
        sAtcIdToName["YNG:ATCT"] = "Youngstown"
        for ((key, value) in sAtcIdToName) {
            sAtcNameToId[value] = key
        }
    }

    init {
        sDatisApts.add("KABQ")
        sDatisApts.add("KADW")
        sDatisApts.add("KALB")
        sDatisApts.add("KATL")
        sDatisApts.add("KAUS")
        sDatisApts.add("KBDL")
        sDatisApts.add("KBNA")
        sDatisApts.add("KBOI")
        sDatisApts.add("KBOS")
        sDatisApts.add("KBUF")
        sDatisApts.add("KBUR")
        sDatisApts.add("KBWI")
        sDatisApts.add("KCHS")
        sDatisApts.add("KCLE")
        sDatisApts.add("KCLT")
        sDatisApts.add("KCMH")
        sDatisApts.add("KCVG")
        sDatisApts.add("KDAL")
        sDatisApts.add("KDCA")
        sDatisApts.add("KDEN")
        sDatisApts.add("KDFW")
        sDatisApts.add("KDTW")
        sDatisApts.add("KELP")
        sDatisApts.add("KEWR")
        sDatisApts.add("KFLL")
        sDatisApts.add("KGSO")
        sDatisApts.add("KHOU")
        sDatisApts.add("KHPN")
        sDatisApts.add("KIAD")
        sDatisApts.add("KIAH")
        sDatisApts.add("KIND")
        sDatisApts.add("KJAX")
        sDatisApts.add("KJFK")
        sDatisApts.add("KLAS")
        sDatisApts.add("KLAX")
        sDatisApts.add("KLGA")
        sDatisApts.add("KLIT")
        sDatisApts.add("KMCI")
        sDatisApts.add("KMCO")
        sDatisApts.add("KMDW")
        sDatisApts.add("KMEM")
        sDatisApts.add("KMIA")
        sDatisApts.add("KMKE")
        sDatisApts.add("KMSP")
        sDatisApts.add("KMSY")
        sDatisApts.add("KOAK")
        sDatisApts.add("KOKC")
        sDatisApts.add("KOMA")
        sDatisApts.add("KONT")
        sDatisApts.add("KORD")
        sDatisApts.add("KPBI")
        sDatisApts.add("KPDX")
        sDatisApts.add("KPHL")
        sDatisApts.add("KPHX")
        sDatisApts.add("KPIT")
        sDatisApts.add("KPVD")
        sDatisApts.add("KRDU")
        sDatisApts.add("KRNO")
        sDatisApts.add("KRSW")
        sDatisApts.add("KSAN")
        sDatisApts.add("KSAT")
        sDatisApts.add("KSDF")
        sDatisApts.add("KSEA")
        sDatisApts.add("KSFO")
        sDatisApts.add("KSJC")
        sDatisApts.add("KSLC")
        sDatisApts.add("KSMF")
        sDatisApts.add("KSNA")
        sDatisApts.add("KSTL")
        sDatisApts.add("KTEB")
        sDatisApts.add("KTPA")
        sDatisApts.add("KTUL")
        sDatisApts.add("KVNY")
        sDatisApts.add("PANC")
        sDatisApts.add("PHNL")
        sDatisApts.add("TJSJ")
    }

    init {
        sPhoneticAlphabets["A"] = "Alpha"
        sPhoneticAlphabets["B"] = "Bravo"
        sPhoneticAlphabets["C"] = "Charlie"
        sPhoneticAlphabets["D"] = "Delta"
        sPhoneticAlphabets["E"] = "Echo"
        sPhoneticAlphabets["F"] = "Foxtrot"
        sPhoneticAlphabets["G"] = "Golf"
        sPhoneticAlphabets["H"] = "Hotel"
        sPhoneticAlphabets["I"] = "India"
        sPhoneticAlphabets["J"] = "Juliet"
        sPhoneticAlphabets["K"] = "Kilo"
        sPhoneticAlphabets["L"] = "Lima"
        sPhoneticAlphabets["M"] = "Mike"
        sPhoneticAlphabets["N"] = "November"
        sPhoneticAlphabets["O"] = "Oscar"
        sPhoneticAlphabets["P"] = "Papa"
        sPhoneticAlphabets["Q"] = "Quebec"
        sPhoneticAlphabets["R"] = "Romeo"
        sPhoneticAlphabets["S"] = "Sierra"
        sPhoneticAlphabets["T"] = "Tango"
        sPhoneticAlphabets["U"] = "Uniform"
        sPhoneticAlphabets["V"] = "Victor"
        sPhoneticAlphabets["W"] = "Whiskey"
        sPhoneticAlphabets["X"] = "Xray"
        sPhoneticAlphabets["Y"] = "Yankee"
        sPhoneticAlphabets["Z"] = "Zulu"
    }
}