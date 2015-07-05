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

package com.nadmm.airports.notams;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;

public class AirportNotamFragment extends NotamFragmentBase {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.airport_notam_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        String siteNumber = args.getString( DatabaseManager.Airports.SITE_NUMBER );
        setBackgroundTask( new AirportNotamTask() ).execute( siteNumber );
    }

    private final class AirportNotamTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            Cursor[] result = new Cursor[ 1 ];
            String siteNumber = params[ 0 ];
            Cursor apt = getAirportDetails( siteNumber );
            result[ 0 ] = apt;

            return result;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            Cursor apt = result[ 0 ];

            showAirportTitle( apt );

            String icaoCode = apt.getString( apt.getColumnIndex( DatabaseManager.Airports.ICAO_CODE ) );
            if ( icaoCode == null || icaoCode.length() == 0 ) {
                String faaCode = apt.getString( apt.getColumnIndex( DatabaseManager.Airports.FAA_CODE ) );
                icaoCode = "K" + faaCode;
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                    getActivity() );
            boolean showGPS = prefs.getBoolean( PreferencesActivity.KEY_SHOW_GPS_NOTAMS, false );
            if ( showGPS ) {
                // Also request GPS NOTAMs
                icaoCode += ",KGPS";
            }
            getNotams( icaoCode );
            return true;
        }

    }

}
