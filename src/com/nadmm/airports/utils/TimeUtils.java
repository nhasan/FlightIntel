/*
 * FlightIntel for Pilots
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import com.nadmm.airports.PreferencesActivity;

public class TimeUtils {

    public static CharSequence formatLongDateTime( long time ) {
        return DateFormat.format( "MMM dd, yyyy h:mmaa", new Date( time ) );
    }

    public static String formatDateTime( Context context, long millis ) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
        boolean local = prefs.getBoolean( PreferencesActivity.SHOW_LOCAL_TIME, false );
        if ( local ) {
            return formatDateTimeLocal( context, millis );
        } else {
            return formatDateTimeUTC( context, millis );
        }
    }

    public static String formatDateTimeYear( Context context, long millis ) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
        boolean local = prefs.getBoolean( PreferencesActivity.SHOW_LOCAL_TIME, false );
        if ( local ) {
            return formatDateTimeYearLocal( context, millis );
        } else {
            return formatDateTimeYearUTC( context, millis );
        }
    }

    public static String formatDateRange( Context context, long startMillis, long endMillis ) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
        boolean local = prefs.getBoolean( PreferencesActivity.SHOW_LOCAL_TIME, false );
        if ( local ) {
            return formatDateRangeLocal( context, startMillis, endMillis );
        } else {
            return formatDateRangeUTC( context, startMillis, endMillis );
        }
    }

    @SuppressWarnings("deprecation")
    public static String formatDateTimeUTC( Context context, long millis ) {
        String s = DateUtils.formatDateRange( context, millis, millis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_UTC );
        return String.format( "%s UTC", s );
    }

    @SuppressWarnings("deprecation")
    public static String formatDateTimeYearUTC( Context context, long millis ) {
        String s = DateUtils.formatDateRange( context, millis, millis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_UTC );
        return String.format( "%s UTC", s );
    }

    @SuppressWarnings("deprecation")
    public static String formatDateTimeLocal( Context context, long millis ) {
        String s = DateUtils.formatDateRange( context, millis, millis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_ABBREV_ALL );
        return String.format( "%s %s", s, getLocalTimeZoneName() );
    }

    @SuppressWarnings("deprecation")
    public static String formatDateTimeYearLocal( Context context, long millis ) {
        String s = DateUtils.formatDateRange( context, millis, millis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_ABBREV_ALL );
        return String.format( "%s %s", s, getLocalTimeZoneName() );
    }

    @SuppressWarnings("deprecation")
    public static String formatDateRangeUTC( Context context,
            long startMillis, long endMillis ) {
        String s = DateUtils.formatDateRange( context, startMillis, endMillis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_UTC );
        return String.format( "%s UTC", s );
    }

    @SuppressWarnings("deprecation")
    public static String formatDateRangeLocal( Context context,
            long startMillis, long endMillis ) {
        String s = DateUtils.formatDateRange( context, startMillis, endMillis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_ABBREV_ALL );
        return String.format( "%s %s", s, getLocalTimeZoneName() );
    }

    public static String getLocalTimeZoneName() {
        TimeZone tz = TimeZone.getDefault();
        return tz.getDisplayName( tz.inDaylightTime( new Date() ), TimeZone.SHORT );
    }

    public static CharSequence formatElapsedTime( long time ) {
        Date now = new Date();
        return formatElapsedTime( now.getTime(), time );
    }

    public static CharSequence formatElapsedTime( long time1, long time2 ) {
        return DateUtils.getRelativeTimeSpanString( time2, time1,
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE );
    }

    public static String getTimeZoneAsString( TimeZone tz ) {
        Date now = new Date();
        String tzName = tz.getDisplayName( tz.inDaylightTime( now ), TimeZone.SHORT );
        SimpleDateFormat tzFormat = new SimpleDateFormat( "'(UTC'Z')'", Locale.US );
        tzFormat.setTimeZone( tz );
        return String.format( "%s %s", tzName, tzFormat.format( now ) );
    }

}
