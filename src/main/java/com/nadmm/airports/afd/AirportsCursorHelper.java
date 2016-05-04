/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.States;

import java.util.HashMap;

public class AirportsCursorHelper {

    private static final String[] mQueryColumns = new String[] {
            BaseColumns._ID,
            Airports.SITE_NUMBER,
            Airports.ICAO_CODE,
            Airports.FAA_CODE,
            Airports.FACILITY_NAME,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE,
            Airports.FUEL_TYPES,
            Airports.UNICOM_FREQS,
            Airports.CTAF_FREQ,
            Airports.ELEVATION_MSL,
            Airports.STATUS_CODE,
            Airports.FACILITY_USE,
            Airports.REF_LATTITUDE_DEGREES,
            Airports.REF_LONGITUDE_DEGREES,
            States.STATE_NAME
    };

    private AirportsCursorHelper() {}

    static HashMap<String, String> buildProjectionMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        for ( String col : mQueryColumns ) {
            if ( !col.equals( States.STATE_NAME ) ) {
                map.put( col, col );
            }
        }
        map.put( States.STATE_NAME,
                "IFNULL("+States.STATE_NAME+", "+Airports.ASSOC_COUNTY+")"
                +" AS "+States.STATE_NAME );
        return map;
    }

    public static Cursor query( SQLiteDatabase db, String selection, String[] selectionArgs,
            String groupBy, String having, String sortOrder, String limit ) {
        if ( db == null ) {
            return new MatrixCursor( mQueryColumns );
        }

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Airports.TABLE_NAME+" a LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
        builder.setProjectionMap( buildProjectionMap() );
        return builder.query( db, mQueryColumns, selection, selectionArgs,
                groupBy, having, sortOrder, limit );
    }

}
