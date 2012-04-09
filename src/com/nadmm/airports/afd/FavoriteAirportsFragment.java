/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.ArrayList;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.utils.AirportsCursorAdapter;
import com.nadmm.airports.utils.AirportsCursorHelper;

public class FavoriteAirportsFragment extends ListFragment {

    public class FavoriteAirportsTask extends AsyncTask<Void, Void, Cursor> {

        private final FavoriteAirportsFragment mFragment;

        public FavoriteAirportsTask( FavoriteAirportsFragment fragment ) {
            super();
            mFragment = fragment;
        }

        @Override
        protected Cursor doInBackground( Void... params ) {
            ActivityBase activity = (ActivityBase) getActivity();
            DatabaseManager dbManager = activity.getDbManager();
            ArrayList<String> favorites = dbManager.getAptFavorites();
            String selection = "";
            for (String site_number : favorites ) {
                if ( selection.length() > 0 ) {
                    selection += ", ";
                }
                selection += "'"+site_number+"'";
            };

            // Query for the favorite airports
            selection = "a."+Airports.SITE_NUMBER+" in ("+selection+")";
            Cursor c = AirportsCursorHelper.query( getActivity(), selection, 
                    null, null, null, Airports.FACILITY_NAME, null );

            return c;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            mFragment.setCursor( c );
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AirportsCursorAdapter adapter = (AirportsCursorAdapter) getListAdapter();
        if ( adapter != null ) {
            Cursor c = adapter.getCursor();
            c.close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        FavoriteAirportsTask task = new FavoriteAirportsTask( this );
        task.execute( (Void[]) null );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );
        setEmptyText( "No favorite airports yet" );
    }

    @Override
    public void onListItemClick( ListView l, View view, int position, long id ) {
        Cursor c = (SQLiteCursor) l.getItemAtPosition( position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        Intent intent = new Intent( getActivity(), AirportDetailsActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        startActivity( intent );
    }

    public void setCursor( final Cursor c ) {
        // We may get called here after activity has detached
        if ( getActivity() != null ) {
            AirportsCursorAdapter adapter = (AirportsCursorAdapter) getListAdapter();
            if ( adapter == null ) {
                adapter = new AirportsCursorAdapter( getActivity(), c );
                setListAdapter( adapter );
            } else {
                adapter.changeCursor( c );
            }
        }
    }

}