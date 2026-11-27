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
package com.nadmm.airports.tfr

import android.text.format.DateUtils
import com.nadmm.airports.ActivityBase
import com.nadmm.airports.tfr.TfrList.Altitude
import com.nadmm.airports.tfr.TfrList.AltitudeType
import com.nadmm.airports.tfr.TfrList.TfrFix
import com.nadmm.airports.utils.TimeUtils
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Text for the parts of a TFR area definition, as the details screen shows them. */
object TfrAreaFormat {
    /** A position such as 39°02'55"N 84°40'04"W. */
    fun formatCoordinates(latitude: Double, longitude: Double): String =
        formatDms(latitude, 2, 'N', 'S') + " " + formatDms(longitude, 3, 'E', 'W')

    private fun formatDms(value: Double, degreeDigits: Int, positive: Char, negative: Char): String {
        // Round to whole seconds first so 59.6" carries into the minutes
        val totalSeconds = (abs(value) * 3600).roundToInt()
        val degrees = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        val hemisphere = if (value < 0) negative else positive
        return String.format(Locale.US, "%0${degreeDigits}d°%02d'%02d\"%c",
            degrees, minutes, seconds, hemisphere)
    }

    /** A fix such as "Falmouth VOR/DME (FLM) 329° radial at 29.2 NM". */
    fun formatFix(fix: TfrFix): String {
        val navaid = listOfNotNull(fix.navaid?.takeIf { it.isNotBlank() }, fix.navaidType)
            .joinToString(" ")
        val name = if (navaid.isEmpty()) fix.ident else "$navaid (${fix.ident})"
        return "$name ${fix.bearing}° radial at ${formatNm(fix.distance)}"
    }

    /** A distance such as "5 NM" or "4.5 NM". */
    fun formatNm(nm: Double): String {
        val rounded = (nm * 10).roundToInt() / 10.0
        val text = if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else rounded.toString()
        return "$text NM"
    }

    /** Vertical limits such as "Surface up to 4,999 ft AGL". */
    fun formatAltitudeRange(min: Altitude?, max: Altitude?): String? = when {
        min != null && max != null -> "${formatLower(min)} up to ${formatUpper(max)}"
        min != null -> "${formatLower(min)} and above"
        max != null -> "Up to ${formatUpper(max)}"
        else -> null
    }

    private fun formatLower(altitude: Altitude) =
        if (altitude.feet == 0) "Surface" else formatAltitude(altitude)

    private fun formatUpper(altitude: Altitude) =
        if (altitude.feet >= 91000) "unlimited" else formatAltitude(altitude)

    private fun formatAltitude(altitude: Altitude): String {
        val type = if (altitude.type == AltitudeType.AGL) "AGL" else "MSL"
        return String.format(Locale.US, "%,d ft %s", altitude.feet, type)
    }

    /**
     * A daily window such as "12:00–23:00 UTC", in local time when the user
     * prefers it.
     */
    @Suppress("deprecation")
    fun formatTimeOfDayRange(activity: ActivityBase, start: Long, end: Long): String {
        val local = activity.prefShowLocalTime
        var flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_24HOUR
        if (!local) flags = flags or DateUtils.FORMAT_UTC
        val range = DateUtils.formatDateRange(activity, start, end, flags)
        return "$range ${if (local) TimeUtils.localTimeZoneName else "UTC"}"
    }
}
