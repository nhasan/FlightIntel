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

package com.nadmm.airports;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.SolarCalculator;

public class AlmanacActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.almanac_detail_view ) );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        AlmanacDetailsTask task = new AlmanacDetailsTask();
        task.execute( siteNumber );
    }

    private final class AlmanacDetailsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 1 ];
            cursors[ 0 ] = getAirportDetails( siteNumber );
            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            showDetails( result );
        }

    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        setActionBarTitle( apt );
        showAirportTitle( apt );
        showSolarInfo( result );

        setContentShown( true );
    }

    private void showSolarInfo( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        String timezoneId = apt.getString( apt.getColumnIndex( Airports.TIMEZONE_ID ) );
        TimeZone tz = TimeZone.getTimeZone( timezoneId );
        double lat = apt.getDouble( apt.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
        double lon = apt.getDouble( apt.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
        Location location = new Location( "" );
        location.setLatitude( lat );
        location.setLongitude( lon );
        Calendar now = Calendar.getInstance( tz );

        SolarCalculator solarCalc = new SolarCalculator( location, now.getTimeZone() );
        Calendar sunrise = solarCalc.getSunriseTime( SolarCalculator.OFFICIAL, now );
        Calendar sunset = solarCalc.getSunsetTime( SolarCalculator.OFFICIAL, now );
        Calendar morningTwilight = solarCalc.getSunriseTime( SolarCalculator.CIVIL, now );
        Calendar eveningTwilight = solarCalc.getSunsetTime( SolarCalculator.CIVIL, now );

        DateFormat format = new SimpleDateFormat( "HH:mm" );
        TimeZone local = now.getTimeZone();
        TimeZone utc = TimeZone.getTimeZone( "GMT" );

        TextView tv = (TextView) findViewById( R.id.sunrise_sunset_label );
        DateFormat date = DateFormat.getDateInstance();
        tv.setText( "Sunrise and Sunset ("+date.format( now.getTime() )+")" );

        TableLayout layout = (TableLayout) findViewById( R.id.morning_info_layout );
        format.setTimeZone( local );
        addRow( layout, "Morning civil twilight (Local)",
                format.format( morningTwilight.getTime() ) );
        addSeparator( layout );
        format.setTimeZone( utc );
        addRow( layout, "Morning civil twilight (UTC)",
                format.format( morningTwilight.getTime() ) );

        layout = (TableLayout) findViewById( R.id.sunrise_info_layout );
        if ( sunrise != null ) {
            format.setTimeZone( local );
            addRow( layout, "Sunrise (Local)", format.format( sunrise.getTime() ) );
            addSeparator( layout );
            format.setTimeZone( utc );
            addRow( layout, "Sunrise (UTC)", format.format( sunrise.getTime() ) );
        } else {
            addRow( layout, "Sunrise (Local)", "Sun does not rise" );
            addSeparator( layout );
            addRow( layout, "Sunrise (UTC)", "Sun does not rise" );
        }

        layout = (TableLayout) findViewById( R.id.sunset_info_layout );
        if ( sunset != null ) {
            format.setTimeZone( local );
            addRow( layout, "Sunset (Local)", format.format( sunset.getTime() ) );
            addSeparator( layout );
            format.setTimeZone( utc );
            addRow( layout, "Sunset (UTC)", format.format( sunset.getTime() ) );
        } else {
            addRow( layout, "Sunset (Local)", "Sun does not set" );
            addSeparator( layout );
            addRow( layout, "Sunset (UTC)", "Sun does not set" );
        }

        layout = (TableLayout) findViewById( R.id.evening_info_layout );
        format.setTimeZone( local );
        addRow( layout, "Evening civil twilight (Local)",
                format.format( eveningTwilight.getTime() ) );
        addSeparator( layout );
        format.setTimeZone( utc );
        addRow( layout, "Evening civil twilight (UTC)",
                format.format( eveningTwilight.getTime() ) );

        layout = (TableLayout) findViewById( R.id.current_time_layout );
        format.setTimeZone( local );
        addRow( layout, "Local time zone", DataUtils.getTimeZoneAsString( local ) );
        addSeparator( layout );
        addRow( layout, "Current time (Local)", format.format( now.getTime() ) );
        addSeparator( layout );
        format.setTimeZone( utc );
        addRow( layout, "Current time (UTC)", format.format( now.getTime() ) );
        addSeparator( layout );
        // Determine FAR 1.1 definition of day/night for logging flight time
        boolean day = ( now.compareTo( morningTwilight ) >= 0
                && now.compareTo( eveningTwilight ) <= 0 );
        addRow( layout, "FAR 1.1 day/night", day? "Day" : "Night" );
        addSeparator( layout );
        // Determine FAR 61.75(b) definition of day/night for carrying passengers
        Calendar far6175bBegin = (Calendar) sunset.clone();
        far6175bBegin.add( Calendar.HOUR_OF_DAY, 1 );
        Calendar far6175bEnd = (Calendar) sunrise.clone();
        far6175bEnd.add( Calendar.HOUR_OF_DAY, -1 );
        day = ( now.compareTo( far6175bEnd ) >= 0 && now.compareTo( far6175bBegin ) <= 0 );
        addRow( layout, "FAR 61.75(b) day/night", day? "Day" : "Night" );
    }

}