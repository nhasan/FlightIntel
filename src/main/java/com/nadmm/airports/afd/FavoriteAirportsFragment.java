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
import android.view.View;
import android.widget.ListView;

import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.utils.CursorAsyncTask;

import java.util.ArrayList;

import androidx.cursoradapter.widget.CursorAdapter;

public class FavoriteAirportsFragment extends ListFragmentBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onResume() {
        super.onResume();

        setBackgroundTask( new FavoriteAirportsTask( this ) ).execute( (String[]) null );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setEmptyText( "No favorite airports selected." );
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

    private Cursor[] doQuery() {
        DatabaseManager dbManager = getDbManager();
        ArrayList<String> siteNumbers = dbManager.getAptFavorites();

        StringBuilder builder = new StringBuilder();
        for (String siteNumber : siteNumbers ) {
            if ( builder.length() > 0 ) {
                builder.append( ", " );
            }
            builder.append( "'" ).append( siteNumber).append( "'" );
        }

        SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
        String selection = "a."+Airports.SITE_NUMBER+" in ("+builder.toString()+")";
        Cursor c = AirportsCursorHelper.query( db, selection,
                null, null, null, Airports.FACILITY_NAME, null );

        return new Cursor[] { c };
    }

    private static class FavoriteAirportsTask extends CursorAsyncTask<FavoriteAirportsFragment> {

        private FavoriteAirportsTask( FavoriteAirportsFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( FavoriteAirportsFragment fragment, String... params ) {
            return fragment.doQuery();
        }

        @Override
        protected boolean onResult( FavoriteAirportsFragment fragment, Cursor[] result ) {
            fragment.setCursor( result[ 0 ] );
            return false;
        }
    }

}
