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

package com.nadmm.airports.notams;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager.Airports;
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
        String siteNumber = args.getString( Airports.SITE_NUMBER );
        setBackgroundTask( new AirportNotamTask( this ) ).execute( siteNumber );
    }

    private void showNotam( Cursor c ) {
        showAirportTitle( c );

        String icaoCode = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( icaoCode == null || icaoCode.length() == 0 ) {
            String faaCode = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
            icaoCode = "K" + faaCode;
        }

        boolean showGPS = getActivityBase().getPrefShowGpsNotam();
        if ( showGPS ) {
            icaoCode += ",KGPS";
        }
        getNotams( icaoCode, "airport" );
    }

    private static class AirportNotamTask extends CursorAsyncTask<AirportNotamFragment> {

        private AirportNotamTask( AirportNotamFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( AirportNotamFragment fragment, String... params ) {
            String siteNumber = params[ 0 ];
            Cursor c = fragment.getAirportDetails( siteNumber );
            return new Cursor[] { c };
        }

        @Override
        protected boolean onResult( AirportNotamFragment fragment, Cursor[] result ) {
            fragment.showNotam( result[ 0 ] );
            return true;
        }

    }

}
