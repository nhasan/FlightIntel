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

package com.nadmm.airports.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import androidx.core.util.Pair;

public class DbUtils {
    public static Pair<String, String[]> getBoundingBoxSelection( String latField, String lonField,
                                                                  Location location, int radius ) {
        double[] box = GeoUtils.getBoundingBoxRadians( location, radius );
        double radLatMin = box[ 0 ];
        double radLatMax = box[ 1 ];
        double radLonMin = box[ 2 ];
        double radLonMax = box[ 3 ];

        // Check if 180th Meridian lies within the bounding Box
        boolean isCrossingMeridian180 = ( radLonMin > radLonMax );

        String selection = "("
                +latField+">=? AND "+latField+"<=?"+") AND ("+lonField+">=? "
                +(isCrossingMeridian180? "OR " : "AND ")+lonField+"<=?)";
        String[] selectionArgs = {
                String.valueOf( Math.toDegrees( radLatMin ) ),
                String.valueOf( Math.toDegrees( radLatMax ) ),
                String.valueOf( Math.toDegrees( radLonMin ) ),
                String.valueOf( Math.toDegrees( radLonMax ) )
        };

        return new Pair<>( selection, selectionArgs );
    }

    public static Cursor getBoundingBoxCursor( SQLiteDatabase db, String tableName,
                                               String latField, String lonField,
                                               Location location, int radius ) {
        return getBoundingBoxCursor( db, tableName, new String[] { "*" }, latField, lonField, location, radius );
    }

    public static Cursor getBoundingBoxCursor( SQLiteDatabase db, String tableName, String[] columns,
                                               String latField, String lonField,
                                               Location location, int radius ) {
        Pair<String, String[]> selection = getBoundingBoxSelection( latField, lonField, location, radius );
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( tableName );

        Cursor c = builder.query( db, columns, selection.first, selection.second,
                null, null, null, null );

        return c;
    }
}
