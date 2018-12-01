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

import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Wxs;
import com.nadmm.airports.utils.CursorAsyncTask;

import java.util.ArrayList;

import androidx.cursoradapter.widget.CursorAdapter;

public class FavoriteWxFragment extends ListFragmentBase {
    private WxDelegate mDelegate;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mDelegate = new WxDelegate( this );
        setHasOptionsMenu( false );
    }

    @Override
    public void onResume() {
        super.onResume();

        mDelegate.onResume();
        new FavoriteWxTask( this ).execute();
    }

    @Override
    public void onPause() {
        super.onPause();

        mDelegate.onPause();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setEmptyText( "No favorite wx stations selected." );
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
    public void setCursor( Cursor c ) {
        mDelegate.setCursor( c );
        super.setCursor( c );
        mDelegate.requestMetars( NoaaService.ACTION_GET_METAR, false, true );
        getActivityBase().enableDisableSwipeRefresh( isRefreshable() );
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        return mDelegate.newListAdapter( context, c );
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        mDelegate.onListItemClick( l, v, position );
    }

    private Cursor[] doQuery() {
        DatabaseManager dbManager = getDbManager();
        ArrayList<String> favorites = dbManager.getWxFavorites();

        StringBuilder builder = new StringBuilder();
        for (String stationId : favorites ) {
            if ( builder.length() > 0 ) {
                builder.append( ", " );
            }
            builder.append( "'" ).append( stationId ).append( "'" );
        }

        SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
        String selection = Wxs.STATION_ID+" in ("+builder.toString()+")";
        Cursor c = WxCursorHelper.query( db, selection, null, null, null,
                Wxs.STATION_NAME, null );
        return new Cursor[] { c };
    }

    private static class FavoriteWxTask extends CursorAsyncTask<FavoriteWxFragment> {

        private FavoriteWxTask( FavoriteWxFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( FavoriteWxFragment fragment, String... params ) {
            return fragment.doQuery();
        }

        @Override
        protected boolean onResult( FavoriteWxFragment fragment, Cursor[] result ) {
            fragment.setCursor( result[ 0 ] );
            return false;
        }

    }

}
