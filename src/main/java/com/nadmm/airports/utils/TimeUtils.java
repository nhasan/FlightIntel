/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;

import com.nadmm.airports.PreferencesActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeUtils {

    private static final SimpleDateFormat ISO3339_FORMAT_UTC =
            new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss", Locale.US );
    private static final SimpleDateFormat ISO3339_FORMAT =
            new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.US );
    private static final SimpleDateFormat ISO3339_MILLIS_FORMAT =
            new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.US );
    private static final SimpleDateFormat FAA_FORMAT =
            new SimpleDateFormat( "MM/dd/yyyy", Locale.US );

    public static CharSequence formatLongDateTime( long time ) {
        return DateFormat.format( "MMM dd, yyyy h:mmaa", new Date( time ) );
    }

    static {
        ISO3339_FORMAT_UTC.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        FAA_FORMAT.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
    }

    public static String formatDateTime( Context context, long millis ) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
        boolean local = prefs.getBoolean( PreferencesActivity.KEY_SHOW_LOCAL_TIME, false );
        if ( local ) {
            return formatDateTimeLocal( context, millis );
        } else {
            return formatDateTimeUTC( context, millis );
        }
    }

    public static String formatDateTimeYear( Context context, long millis ) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
        boolean local = prefs.getBoolean( PreferencesActivity.KEY_SHOW_LOCAL_TIME, false );
        if ( local ) {
            return formatDateTimeYearLocal( context, millis );
        } else {
            return formatDateTimeYearUTC( context, millis );
        }
    }

    public static String formatDateRange( Context context, long startMillis, long endMillis ) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
        boolean local = prefs.getBoolean( PreferencesActivity.KEY_SHOW_LOCAL_TIME, false );
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

    public static String format3339( Date date ) {
        return ISO3339_FORMAT_UTC.format( date )+"Z";
    }

    public static Date parse3339( String s ) {
        // This is needed as SimpleDateFormat does not parse RFC3339 "Z" for UTC.
        // Convert to ISO8601 format first if using "Z"
        String iso8601 = s.replaceAll( "Z$", "+00:00" );
        Date date;
        try {
            date = ISO3339_FORMAT.parse( iso8601 );
        } catch ( ParseException e ) {
            date = null;
        }

        if ( date == null ) {
            try {
                date = ISO3339_MILLIS_FORMAT.parse( iso8601 );
            } catch ( ParseException e ) {
            }
        }

        return date;
    }

    public static Date parseFaaDate( String s ) {
        Date date;
        try {
            date = FAA_FORMAT.parse( s );
        } catch ( ParseException e ) {
            date = null;
        }

        return date;
    }
}
