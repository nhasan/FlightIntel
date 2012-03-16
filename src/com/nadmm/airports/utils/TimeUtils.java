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

import java.util.Date;
import java.util.Formatter;
import java.util.TimeZone;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;

import com.nadmm.airports.PreferencesActivity;

public class TimeUtils {
    
    private static Formatter mFormatter = new Formatter( new StringBuilder() );

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

    public static String formatDateRange( Context context, long startMillis, long endMillis ) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context );
        boolean local = prefs.getBoolean( PreferencesActivity.SHOW_LOCAL_TIME, false );
        if ( local ) {
            return formatDateRangeLocal( context, startMillis, endMillis );
        } else {
            return formatDateRangeUTC( context, startMillis, endMillis );
        }
    }

    public static String formatDateTimeUTC( Context context, long millis ) {
        StringBuilder sb = (StringBuilder) mFormatter.out();
        sb.setLength( 0 );
        Formatter f = DateUtils.formatDateRange( context, mFormatter, millis, millis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_ABBREV_ALL,
                Time.TIMEZONE_UTC );
        return f.toString()+" UTC";
    }

    public static String formatDateTimeLocal( Context context, long millis ) {
        StringBuilder sb = (StringBuilder) mFormatter.out();
        sb.setLength( 0 );
        Formatter f = DateUtils.formatDateRange( context, mFormatter, millis, millis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_ABBREV_ALL );
        return String.format( "%s %s", f.toString(), getLocalTimeZoneName() );
    }

    public static String formatDateRangeUTC( Context context,
            long startMillis, long endMillis ) {
        StringBuilder sb = (StringBuilder) mFormatter.out();
        sb.setLength( 0 );
        Formatter f = DateUtils.formatDateRange( context, mFormatter, startMillis, endMillis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_ABBREV_ALL,
                Time.TIMEZONE_UTC );
        return f.toString()+" UTC";
    }

    public static String formatDateRangeLocal( Context context,
            long startMillis, long endMillis ) {
        StringBuilder sb = (StringBuilder) mFormatter.out();
        sb.setLength( 0 );
        Formatter f = DateUtils.formatDateRange( context, mFormatter, startMillis, endMillis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_ABBREV_ALL );
        return String.format( "%s %s", f.toString(), getLocalTimeZoneName() );
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

}
