/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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
import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.IRefreshable;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.utils.CursorAsyncTask;

import java.util.ArrayList;

public class FavoriteAirportsFragment extends ListFragmentBase implements IRefreshable {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        setEmptyText( "No favorite airports selected." );
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onResume() {
        super.onResume();

        setBackgroundTask( new FavoriteAirportsTask() ).execute( (String[]) null );
    }

    @Override
    public boolean isRefreshable() {
        return false;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return false;
    }

    @Override
    public void requestDataRefresh() {
        getActivityBase().onRefreshingStateChanged( false );
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        return new AirportsCursorAdapter( context, c );
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        Cursor c = (Cursor) l.getItemAtPosition( position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        Intent intent = new Intent( getActivity(), AirportActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        startActivity( intent );
    }

    public class FavoriteAirportsTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            ActivityBase activity = (ActivityBase) getActivity();
            if ( activity == null ) {
                cancel( false );
                return null;
            }

            DatabaseManager dbManager = activity.getDbManager();
            ArrayList<String> siteNumbers = dbManager.getAptFavorites();

            String selection = "";
            for (String siteNumber : siteNumbers ) {
                if ( selection.length() > 0 ) {
                    selection += ", ";
                }
                selection += "'"+siteNumber+"'";
            }

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            selection = "a."+Airports.SITE_NUMBER+" in ("+selection+")";
            Cursor c = AirportsCursorHelper.query( db, selection,
                    null, null, null, Airports.FACILITY_NAME, null );

            return new Cursor[] { c };
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            setCursor( result[ 0 ] );
            return false;
        }
    }

}
