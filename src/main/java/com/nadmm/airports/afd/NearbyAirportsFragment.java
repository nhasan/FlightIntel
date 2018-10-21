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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.ListView;

import com.nadmm.airports.LocationListFragmentBase;
import com.nadmm.airports.data.DatabaseManager;

import java.util.Locale;

import static com.nadmm.airports.data.DatabaseManager.Airports;

public class NearbyAirportsFragment extends LocationListFragmentBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setEmptyText( "No airports found nearby." );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        if ( !isLocationUpdateEnabled() ) {
            setActionBarTitle( "Nearby Airports" );
            setActionBarSubtitle( String.format( Locale.US, "Within %d NM radius", getNearbyRadius() ) );
        }
    }

    @Override
    protected LocationTask newLocationTask() {
        return new NearbyAirportsTask();
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        return new AirportsCursorAdapter( context, c );
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        Cursor c = (Cursor) l.getItemAtPosition( position );
        String siteNumber = c.getString( c.getColumnIndex( DatabaseManager.Airports.SITE_NUMBER ) );
        Intent intent = new Intent( getActivity(), AirportActivity.class );
        intent.putExtra( DatabaseManager.Airports.SITE_NUMBER, siteNumber );
        startActivity( intent );
    }

    private final class NearbyAirportsTask extends LocationTask {

        @Override
        protected Cursor doInBackground( Void... params ) {
            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );

            String extraSelection= null;
            Bundle args = getArguments();
            if ( args.containsKey( Airports.ICAO_CODE ) ) {
                String icaoCode = args.getString( Airports.ICAO_CODE );
                if ( !icaoCode.isEmpty() ) {
                    extraSelection = "AND "+Airports.ICAO_CODE+" <> '"+icaoCode+"'";
                }
            }

            return new NearbyAirportsCursor( db, getLastLocation(), getNearbyRadius(), extraSelection );
        }

        @Override
        protected void onPostExecute( final Cursor c ) {
            setCursor( c );
        }

    }

}
