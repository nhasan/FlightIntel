/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Context
import android.text.format.DateUtils
import com.nadmm.airports.ActivityBase
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    private val ISO3339_FORMAT_UTC = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val ISO3339_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.US)
    private val ISO3339_MILLIS_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.US)
    private val FAA_FORMAT = SimpleDateFormat("MM/dd/yyyy", Locale.US)

    init {
        ISO3339_FORMAT_UTC.timeZone = TimeZone.getTimeZone("UTC")
        FAA_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun formatDateTime(activity: ActivityBase, millis: Long): String {
        if (activity.prefShowLocalTime) {
            return formatDateTimeLocal(activity, millis)
        } else {
            return formatDateTimeUTC(activity, millis)
        }
    }

    fun formatDateTimeYear(activity: ActivityBase, millis: Long): String {
        if (activity.prefShowLocalTime) {
            return formatDateTimeYearLocal(activity, millis)
        } else {
            return formatDateTimeYearUTC(activity, millis)
        }
    }

    fun formatDateRange(activity: ActivityBase, start: Calendar, end: Calendar): String {
        return formatDateRange(activity, start.getTimeInMillis(), end.getTimeInMillis())
    }

    fun formatDateRange(activity: ActivityBase, startMillis: Long, endMillis: Long): String {
        if (activity.prefShowLocalTime) {
            return formatDateRangeLocal(activity, startMillis, endMillis)
        } else {
            return formatDateRangeUTC(activity, startMillis, endMillis)
        }
    }

    @Suppress("deprecation")
    fun formatDateTimeUTC(context: Context?, millis: Long): String {
        val s = DateUtils.formatDateRange(
            context, millis, millis,
            (DateUtils.FORMAT_24HOUR
                    or DateUtils.FORMAT_SHOW_DATE
                    or DateUtils.FORMAT_SHOW_TIME
                    or DateUtils.FORMAT_NO_YEAR
                    or DateUtils.FORMAT_ABBREV_ALL
                    or DateUtils.FORMAT_UTC)
        )
        return "$s UTC"
    }

    @Suppress("deprecation")
    fun formatDateTimeYearUTC(context: Context?, millis: Long): String {
        val s = DateUtils.formatDateRange(
            context, millis, millis,
            (DateUtils.FORMAT_24HOUR
                    or DateUtils.FORMAT_SHOW_DATE
                    or DateUtils.FORMAT_SHOW_TIME
                    or DateUtils.FORMAT_ABBREV_ALL
                    or DateUtils.FORMAT_UTC)
        )
        return String.format("%s UTC", s)
    }

    @Suppress("deprecation")
    fun formatDateTimeLocal(context: Context?, millis: Long): String {
        val s = DateUtils.formatDateRange(
            context, millis, millis,
            (DateUtils.FORMAT_24HOUR
                    or DateUtils.FORMAT_SHOW_DATE
                    or DateUtils.FORMAT_SHOW_TIME
                    or DateUtils.FORMAT_NO_YEAR
                    or DateUtils.FORMAT_ABBREV_ALL)
        )
        return String.format("%s %s", s, localTimeZoneName)
    }

    @Suppress("deprecation")
    fun formatDateTimeYearLocal(context: Context?, millis: Long): String {
        val s = DateUtils.formatDateRange(
            context, millis, millis,
            (DateUtils.FORMAT_24HOUR
                    or DateUtils.FORMAT_SHOW_DATE
                    or DateUtils.FORMAT_SHOW_TIME
                    or DateUtils.FORMAT_ABBREV_ALL)
        )
        return String.format("%s %s", s, localTimeZoneName)
    }

    @Suppress("deprecation")
    fun formatDateRangeUTC(
        context: Context?,
        startMillis: Long, endMillis: Long
    ): String {
        val s = DateUtils.formatDateRange(
            context, startMillis, endMillis,
            (DateUtils.FORMAT_24HOUR
                    or DateUtils.FORMAT_SHOW_DATE
                    or DateUtils.FORMAT_SHOW_TIME
                    or DateUtils.FORMAT_NO_YEAR
                    or DateUtils.FORMAT_ABBREV_ALL
                    or DateUtils.FORMAT_UTC)
        )
        return String.format("%s UTC", s)
    }

    @Suppress("deprecation")
    fun formatDateRangeLocal(
        context: Context?,
        startMillis: Long, endMillis: Long
    ): String {
        val s = DateUtils.formatDateRange(
            context, startMillis, endMillis,
            (DateUtils.FORMAT_24HOUR
                    or DateUtils.FORMAT_SHOW_DATE
                    or DateUtils.FORMAT_SHOW_TIME
                    or DateUtils.FORMAT_NO_YEAR
                    or DateUtils.FORMAT_ABBREV_ALL)
        )
        return String.format("%s %s", s, localTimeZoneName)
    }

    val localTimeZoneName: String
        get() {
            val tz = TimeZone.getDefault()
            return tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT)
        }

    fun formatElapsedTime(time: Long): CharSequence? {
        val now = Date()
        return formatElapsedTime(now.getTime(), time)
    }

    fun formatElapsedTime(time1: Long, time2: Long): CharSequence? {
        if ((time1 - time2) < DateUtils.MINUTE_IN_MILLIS) {
            return "just now"
        }
        return DateUtils.getRelativeTimeSpanString(
            time2, time1,
            DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        )
    }

    fun getTimeZoneAsString(tz: TimeZone): String {
        val now = Date()
        val tzName = tz.getDisplayName(tz.inDaylightTime(now), TimeZone.SHORT)
        val tzFormat = SimpleDateFormat("'(UTC'Z')'", Locale.US)
        tzFormat.timeZone = tz
        return String.format("%s %s", tzName, tzFormat.format(now))
    }

    @JvmStatic
    fun format3339(date: Date): String {
        return ISO3339_FORMAT_UTC.format(date) + "Z"
    }

    @JvmStatic
    fun parse3339(s: String): Date? {
        // This is needed as SimpleDateFormat does not parse RFC3339 "Z" for UTC.
        // Convert to ISO8601 format first if using "Z"
        val iso8601 = s.replace("Z$".toRegex(), "+00:00")
        var date: Date?
        try {
            date = ISO3339_FORMAT.parse(iso8601)
        } catch (_: ParseException) {
            date = null
        }

        if (date == null) {
            try {
                date = ISO3339_MILLIS_FORMAT.parse(iso8601)
            } catch (_: ParseException) {
            }
        }

        return date
    }

    fun parseFaaDate(s: String): Date? {
        var date: Date?
        try {
            date = FAA_FORMAT.parse(s)
        } catch (_: ParseException) {
            date = null
        }

        return date
    }
}
