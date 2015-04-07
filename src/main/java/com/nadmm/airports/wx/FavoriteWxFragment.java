/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.ArrayList;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Wxs;

public class FavoriteWxFragment extends WxListFragmentBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        setEmptyText( "No favorite wx stations selected." );
        super.onCreate( savedInstanceState );
    }

    @Override
    public void onResume() {
        super.onResume();

        new FavoriteWxTask().execute( (Void[]) null );
    }

    public class FavoriteWxTask extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground( Void... params ) {
            ActivityBase activity = (ActivityBase) getActivity();
            if ( activity == null ) {
                cancel( false );
                return null;
            }

            DatabaseManager dbManager = activity.getDbManager();
            ArrayList<String> favorites = dbManager.getWxFavorites();

            String selectionList = "";
            for (String stationId : favorites ) {
                if ( selectionList.length() > 0 ) {
                    selectionList += ", ";
                }
                selectionList += "'"+stationId+"'";
            };

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            String selection = Wxs.STATION_ID+" in ("+selectionList+")";
            Cursor c = WxCursorHelper.query( db, selection, null, null, null,
                    Wxs.STATION_NAME, null );

            return c;
        }

        @Override
        protected void onPostExecute( final Cursor c ) {
            setCursor( c );
        }

    }

}
