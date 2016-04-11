/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2016 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.Awos1;
import com.nadmm.airports.data.DatabaseManager.Wxs;

public class WxCursorHelper {

    private static final String[] sQueryColumns = new String[] {
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
            Awos1.COMMISSIONING_STATUS,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE,
    };

    public static Cursor query( SQLiteDatabase db, String selection, String[] selectionArgs,
            String groupBy, String having, String sortOrder, String limit ) {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Wxs.TABLE_NAME+" x"
                +" LEFT JOIN "+Airports.TABLE_NAME+" a"
                +" ON substr(x."+Wxs.STATION_ID+", 2) = a."+Airports.FAA_CODE
                +" LEFT JOIN "+Awos1.TABLE_NAME+" w"
                +" ON w."+Awos1.WX_SENSOR_IDENT+" = a."+Airports.FAA_CODE );

        return builder.query( db, sQueryColumns, selection, selectionArgs,
                groupBy, having, sortOrder, limit );
    }

}
