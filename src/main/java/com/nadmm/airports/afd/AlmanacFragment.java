/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2018 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.afd;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.SolarCalculator;
import com.nadmm.airports.utils.TimeUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class AlmanacFragment extends FragmentBase {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.almanac_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setActionBarTitle( "Sunrise and sunset", "" );

        String siteNumber = getArguments().getString( Airports.SITE_NUMBER );
        setBackgroundTask( new AlmanacDetailsTask( this ) ).execute( siteNumber );
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        showAirportTitle( apt );
        showSolarInfo( result );
        setFragmentContentShown( true );
    }

    @SuppressLint( "SetTextI18n" )
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

        DateFormat format = new SimpleDateFormat( "HH:mm", Locale.US );
        TimeZone local = now.getTimeZone();
        TimeZone utc = TimeZone.getTimeZone( "GMT" );

        TextView tv = findViewById( R.id.sunrise_sunset_label );
        DateFormat date = DateFormat.getDateInstance();
        tv.setText( "Sunrise and Sunset ("+date.format( now.getTime() )+")" );

        LinearLayout layout = findViewById( R.id.morning_info_layout );
        format.setTimeZone( local );
        addRow( layout, "Morning civil twilight (Local)",
                format.format( morningTwilight.getTime() ) );
        format.setTimeZone( utc );
        addRow( layout, "Morning civil twilight (UTC)",
                format.format( morningTwilight.getTime() ) );

        layout = findViewById( R.id.sunrise_info_layout );
        if ( sunrise != null ) {
            format.setTimeZone( local );
            addRow( layout, "Sunrise (Local)", format.format( sunrise.getTime() ) );
            format.setTimeZone( utc );
            addRow( layout, "Sunrise (UTC)", format.format( sunrise.getTime() ) );
        } else {
            addRow( layout, "Sunrise (Local)", "Sun does not rise" );
            addRow( layout, "Sunrise (UTC)", "Sun does not rise" );
        }

        layout = findViewById( R.id.sunset_info_layout );
        if ( sunset != null ) {
            format.setTimeZone( local );
            addRow( layout, "Sunset (Local)", format.format( sunset.getTime() ) );
            format.setTimeZone( utc );
            addRow( layout, "Sunset (UTC)", format.format( sunset.getTime() ) );
        } else {
            addRow( layout, "Sunset (Local)", "Sun does not set" );
            addRow( layout, "Sunset (UTC)", "Sun does not set" );
        }

        layout = findViewById( R.id.evening_info_layout );
        format.setTimeZone( local );
        addRow( layout, "Evening civil twilight (Local)",
                format.format( eveningTwilight.getTime() ) );
        format.setTimeZone( utc );
        addRow( layout, "Evening civil twilight (UTC)",
                format.format( eveningTwilight.getTime() ) );

        layout = findViewById( R.id.current_time_layout );
        format.setTimeZone( local );
        addRow( layout, "Local time zone", TimeUtils.getTimeZoneAsString( local ) );
        addRow( layout, "Current time (Local)", format.format( now.getTime() ) );
        format.setTimeZone( utc );
        addRow( layout, "Current time (UTC)", format.format( now.getTime() ) );
        // Determine FAR 1.1 definition of day/night for logging flight time
        boolean day = ( now.compareTo( morningTwilight ) >= 0
                && now.compareTo( eveningTwilight ) <= 0 );
        addRow( layout, "FAR 1.1 day/night", day? "Day" : "Night" );
        // Determine FAR 61.75(b) definition of day/night for carrying passengers
        if ( sunset != null && sunrise != null ) {
            Calendar far6175bBegin = (Calendar) sunset.clone();
            far6175bBegin.add( Calendar.HOUR_OF_DAY, 1 );
            Calendar far6175bEnd = (Calendar) sunrise.clone();
            far6175bEnd.add( Calendar.HOUR_OF_DAY, -1 );
            day = ( now.compareTo( far6175bEnd ) >= 0 && now.compareTo( far6175bBegin ) <= 0 );
            addRow( layout, "FAR 61.75(b) day/night", day? "Day" : "Night" );
        }
    }

    private static class AlmanacDetailsTask extends CursorAsyncTask<AlmanacFragment> {

        private AlmanacDetailsTask( AlmanacFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( AlmanacFragment fragment, String... params ) {
            String siteNumber = params[ 0 ];
            Cursor c = fragment.getAirportDetails( siteNumber );
            return new Cursor[] { c };
        }

        @Override
        protected boolean onResult( AlmanacFragment fragment, Cursor[] result ) {
            fragment.showDetails( result );
            return true;
        }

    }

}
