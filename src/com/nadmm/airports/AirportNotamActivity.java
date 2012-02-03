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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.nadmm.airports.DatabaseManager.Airports;

public class AirportNotamActivity extends NotamActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.airport_notam_view ) );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        NotamTask task = new NotamTask();
        task.execute( siteNumber );
    }

    private final class NotamTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            Cursor[] result = new Cursor[ 1 ];
            String siteNumber = params[ 0 ];
            Cursor apt = getAirportDetails( siteNumber );
            result[ 0 ] = apt;

            String icaoCode = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
            if ( icaoCode == null || icaoCode.length() == 0 ) {
                String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
                icaoCode = "K"+faaCode;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    AirportNotamActivity.this );
            boolean showGPS = prefs.getBoolean( PreferencesActivity.KEY_SHOW_GPS_NOTAMS, false );
            if ( showGPS ) {
                // Also request GPS NOTAMs
                icaoCode += ",KGPS";
            }

            getNotams( icaoCode );

            return result;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            showDetails( result );
        }

    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result [ 0 ];
        String icaoCode = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
        if ( icaoCode == null || icaoCode.length() == 0 ) {
            String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            icaoCode = "K"+faaCode;
        }

        setActionBarTitle( apt );
        showAirportTitle( apt );
        showNotams( icaoCode );

        setContentShown( true );
    }

}
