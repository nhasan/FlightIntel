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
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos1;
import com.nadmm.airports.DatabaseManager.Wxs;

public class FavoriteWxFragment extends WxListFragmentBase {

    public class FavoriteWxTask extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected Cursor doInBackground( Void... params ) {
            ActivityBase activity = (ActivityBase) getActivity();

            SQLiteDatabase db = activity.getDatabase( DatabaseManager.DB_FADDS );

            DatabaseManager dbManager = activity.getDbManager();
            ArrayList<String> favorites = dbManager.getWxFavorites();

            String selectionList = "";
            for (String stationId : favorites ) {
                if ( selectionList.length() > 0 ) {
                    selectionList += ", ";
                }
                selectionList += "'"+stationId+"'";
            };

            String selection = Wxs.STATION_ID+" in ("+selectionList+")";

            String[] wxColumns = new String[] {
                "x."+BaseColumns._ID,
                Wxs.STATION_ID,
                Wxs.STATION_NAME,
                Wxs.STATION_ELEVATOIN_METER,
                "x."+Wxs.STATION_LATITUDE_DEGREES,
                "x."+Wxs.STATION_LONGITUDE_DEGREES,
                Awos1.WX_SENSOR_IDENT,
                Awos1.WX_SENSOR_TYPE,
                Awos1.STATION_FREQUENCY,
                Awos1.SECOND_STATION_FREQUENCY,
                Awos1.STATION_PHONE_NUMBER,
                Airports.ASSOC_CITY,
                Airports.ASSOC_STATE
            };

            String sortOrder = Wxs.STATION_NAME;

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Wxs.TABLE_NAME+" x"
                    +" LEFT JOIN "+Airports.TABLE_NAME+" a"
                    +" ON x."+Wxs.STATION_ID+" = a."+Airports.ICAO_CODE
                    +" LEFT JOIN "+Awos1.TABLE_NAME+" w"
                    +" ON w."+Awos1.WX_SENSOR_IDENT+" = a."+Airports.FAA_CODE );
            Cursor c = builder.query( db, wxColumns, selection, 
                    null, null, null, sortOrder, null );

            return c;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            setCursor( c );
        }

    }

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

}
