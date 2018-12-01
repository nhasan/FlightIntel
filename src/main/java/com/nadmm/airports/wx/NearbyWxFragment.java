/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2018 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.wx;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.nadmm.airports.LocationListFragmentBase;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.utils.CursorAsyncTask;

import androidx.cursoradapter.widget.CursorAdapter;

public class NearbyWxFragment extends LocationListFragmentBase {
    private WxDelegate mDelegate;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mDelegate = new WxDelegate( this );
    }

    @Override
    public void onResume() {
        super.onResume();

        mDelegate.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        mDelegate.onPause();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setEmptyText( "No wx stations found nearby." );
    }

    @Override
    public boolean isRefreshable() {
        return getListAdapter() != null && !getListAdapter().isEmpty();
    }

    @Override
    public void requestDataRefresh() {
        mDelegate.requestMetars( NoaaService.ACTION_GET_METAR, true, true );
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        return mDelegate.newListAdapter( context, c );
    }

    @Override
    protected void setCursor( Cursor c ) {
        mDelegate.setCursor( c );
        super.setCursor( c );
        getActivityBase().enableDisableSwipeRefresh( isRefreshable() );
        mDelegate.requestMetars( NoaaService.ACTION_GET_METAR, false, true );
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        mDelegate.onListItemClick( l, v, position );
    }

    private Cursor[] doQuery() {
        SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
        Cursor c = new NearbyWxCursor( db, getLastLocation(), getNearbyRadius() );
        return new Cursor[] { c };
    }

    @Override
    protected void startLocationTask() {
        setBackgroundTask( new NearbyWxTask( this ) ).execute();
    }

    private static class NearbyWxTask extends CursorAsyncTask<NearbyWxFragment> {

        private NearbyWxTask( NearbyWxFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( NearbyWxFragment fragment, String... params ) {
            return fragment.doQuery();
        }

        @Override
        protected boolean onResult( NearbyWxFragment fragment, Cursor[] result ) {
            fragment.setCursor( result[ 0 ] );
            return false;
        }
    }

}
