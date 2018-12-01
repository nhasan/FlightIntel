/*
 * FlightIntel for Pilots
 *
 * Copyright 2018 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.dof;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.nadmm.airports.LocationListFragmentBase;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.utils.CursorAsyncTask;

import java.util.Locale;

import androidx.cursoradapter.widget.CursorAdapter;

public class NearbyObstaclesFragment extends LocationListFragmentBase {

    private int mRadius = 5;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setEmptyText( "No obstacles found nearby." );
        setActionBarTitle( "Nearby Obstacles", "" );
        setActionBarSubtitle( String.format( Locale.US, "Within %d NM radius", mRadius ) );
    }

    @Override
    protected void startLocationTask() {
        setBackgroundTask( new NearbyObstaclesTask( this ) ).execute();
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        return new DofCursorAdapter( context, c );
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {

    }

    private Cursor[] doQuery() {
        SQLiteDatabase db = getDatabase( DatabaseManager.DB_DOF );
        Cursor c = new NearbyDofCursor( db, getLastLocation(), mRadius );
        return new Cursor[] { c };
    }

    private static class NearbyObstaclesTask extends CursorAsyncTask<NearbyObstaclesFragment> {

        private NearbyObstaclesTask( NearbyObstaclesFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( NearbyObstaclesFragment fragment, String... params ) {
            return fragment.doQuery();
        }

        @Override
        protected boolean onResult( NearbyObstaclesFragment fragment, Cursor[] result ) {
            fragment.setCursor( result[ 0 ] );
            return false;
        }

    }

}
