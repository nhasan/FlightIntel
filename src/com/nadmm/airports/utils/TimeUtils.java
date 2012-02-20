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

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;

public class TimeUtils {
    
    private static final int SECONDS_PER_MINUTE = 60;
    private static final int SECONDS_PER_HOUR = 60 * 60;
    private static final int SECONDS_PER_DAY = 24 * 60 * 60;

    private static final Formatter sFormatter = new Formatter();

    public static CharSequence formatLongDateTime( long time ) {
        return DateFormat.format( "MMM dd, yyyy h:mmaa", new Date( time ) );
    }

    public static CharSequence formatElapsedTime( long time ) {
        Date now = new Date();
        return formatElapsedTime( now.getTime(), time );
    }

    public static CharSequence formatElapsedTime( long time1, long time2 ) {
        long age = time1-time2;
        return formatDuration( age )+" old";
    }

    public static String formatDateUTC( Context context, long millis ) {
        return formatDateRangeUTC( context, millis, millis );
    }

    public static String formatDateRangeUTC( Context context,
            long startMillis, long endMillis ) {
        String s = DateUtils.formatDateRange( context, startMillis, endMillis,
                DateUtils.FORMAT_24HOUR
                | DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_TIME
                | DateUtils.FORMAT_NO_YEAR
                | DateUtils.FORMAT_ABBREV_ALL
                | DateUtils.FORMAT_UTC );
        return s+" UTC";
    }

    public static String formatDuration( long duration ) {
        StringBuilder builder = new StringBuilder();
        int seconds = (int) Math.floor( duration/1000 );
        int days = 0, hours = 0, minutes = 0;

        if ( seconds > SECONDS_PER_DAY ) {
            days = seconds/SECONDS_PER_DAY;
            seconds -= days*SECONDS_PER_DAY;
            builder.append( days );
            builder.append( "d" );
        }
        if ( seconds > SECONDS_PER_HOUR ) {
            hours = seconds/SECONDS_PER_HOUR;
            seconds -= hours*SECONDS_PER_HOUR;
            if ( builder.length() > 0 ) {
                builder.append( " " );
            }
            builder.append( hours );
            builder.append( "h" );
        }
        if ( seconds > SECONDS_PER_MINUTE ) {
            minutes = seconds/SECONDS_PER_MINUTE;
            seconds -= minutes*SECONDS_PER_MINUTE;
            if ( builder.length() > 0 ) {
                builder.append( " " );
            }
            builder.append( minutes );
            builder.append( "m" );
        }

        return builder.toString();
    }

}
