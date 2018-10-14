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

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.location.Location;
import android.provider.BaseColumns;

import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.utils.GeoUtils;

import java.util.Arrays;

public class NearbyDofCursor extends MatrixCursor {

    private static final String[] sColumns = new String[] {
            BaseColumns._ID,
            DatabaseManager.DOF.OAS_CODE,
            DatabaseManager.DOF.VERIFICATION_STATUS,
            DatabaseManager.DOF.OBSTACLE_TYPE,
            DatabaseManager.DOF.COUNT,
            DatabaseManager.DOF.HEIGHT_AGL,
            DatabaseManager.DOF.HEIGHT_MSL,
            DatabaseManager.DOF.LIGHTING_TYPE,
            DatabaseManager.DOF.MARKING_TYPE,
            DatabaseManager.LocationColumns.BEARING,
            DatabaseManager.LocationColumns.DISTANCE
    };


    public NearbyDofCursor( SQLiteDatabase db, Location location, int radius ) {
        super( sColumns );

        // Get the bounding box first to do a quick query as a first cut
        double[] box = GeoUtils.getBoundingBoxRadians( location, radius );
        double radLatMin = box[ 0 ];
        double radLatMax = box[ 1 ];
        double radLonMin = box[ 2 ];
        double radLonMax = box[ 3 ];

        // Check if 180th Meridian lies within the bounding Box
        boolean isCrossingMeridian180 = ( radLonMin > radLonMax );

        String selection = "("
                +DatabaseManager.DOF.LATITUDE_DEGREES+">=? AND "+DatabaseManager.DOF.LATITUDE_DEGREES+"<=?"
                +") AND ("+DatabaseManager.DOF.LONGITUDE_DEGREES+">=? "
                +(isCrossingMeridian180? "OR " : "AND ")+DatabaseManager.DOF.LONGITUDE_DEGREES+"<=?)";
        String[] selectionArgs = {
                String.valueOf( Math.toDegrees( radLatMin ) ),
                String.valueOf( Math.toDegrees( radLatMax ) ),
                String.valueOf( Math.toDegrees( radLonMin ) ),
                String.valueOf( Math.toDegrees( radLonMax ) )
        };

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( DatabaseManager.DOF.TABLE_NAME );

        Cursor c = builder.query( db, new String[] { "*" }, selection, selectionArgs,
                null, null, null, null );
        if ( c.moveToFirst() ) {
            float declination = GeoUtils.getMagneticDeclination( location );
            DOFData[] dofList = new DOFData[ c.getCount() ];
            do {
                DOFData obst = new DOFData();
                obst.setFromCursor( c, location, declination );
                dofList[ c.getPosition() ] = obst;
            } while ( c.moveToNext() );

            // Sort the list based on distance from current location
            Arrays.sort( dofList );

            for ( DOFData dof : dofList ) {
                if ( dof.DISTANCE <= radius ) {
                    if ( !location.hasAltitude() || location.getAltitude()-100 <= dof.HEIGHT_MSL ) {
                        MatrixCursor.RowBuilder row = newRow();
                        row.add( getPosition() )
                                .add( dof.OAS_CODE )
                                .add( dof.VERIFICATION_STATUS )
                                .add( dof.OBSTACLE_TYPE )
                                .add( dof.COUNT )
                                .add( dof.HEIGHT_AGL )
                                .add( dof.HEIGHT_MSL )
                                .add( dof.LIGHTING_TYPE )
                                .add( dof.MARKING_TYPE )
                                .add( dof.BEARING )
                                .add( dof.DISTANCE );
                    }
                }
            }
        }

        c.close();
    }

    private final class DOFData implements Comparable<DOFData> {
        private String OAS_CODE;
        private String VERIFICATION_STATUS;
        private String OBSTACLE_TYPE;
        private int COUNT;
        private int HEIGHT_AGL;
        private int HEIGHT_MSL;
        private String LIGHTING_TYPE;
        private String MARKING_TYPE;
        private float BEARING;
        private float DISTANCE;

        private void setFromCursor( Cursor c, Location location, float declination ) {
            OAS_CODE = c.getString( c.getColumnIndex( DatabaseManager.DOF.OAS_CODE ) );
            VERIFICATION_STATUS = c.getString( c.getColumnIndex( DatabaseManager.DOF.VERIFICATION_STATUS ) );
            OBSTACLE_TYPE = c.getString( c.getColumnIndex( DatabaseManager.DOF.OBSTACLE_TYPE ) );
            COUNT = c.getInt( c.getColumnIndex( DatabaseManager.DOF.COUNT ) );
            HEIGHT_AGL = c.getInt( c.getColumnIndex( DatabaseManager.DOF.HEIGHT_AGL ) );
            HEIGHT_MSL = c.getInt( c.getColumnIndex( DatabaseManager.DOF.HEIGHT_MSL ) );
            LIGHTING_TYPE = c.getString( c.getColumnIndex( DatabaseManager.DOF.LIGHTING_TYPE ) );
            MARKING_TYPE = c.getString( c.getColumnIndex( DatabaseManager.DOF.MARKING_TYPE ) );

            float[] results = new float[ 2 ];
            Location.distanceBetween(
                    location.getLatitude(),
                    location.getLongitude(),
                    c.getDouble( c.getColumnIndex( DatabaseManager.DOF.LATITUDE_DEGREES ) ),
                    c.getDouble( c.getColumnIndex( DatabaseManager.DOF.LONGITUDE_DEGREES ) ),
                    results );
            DISTANCE = results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE;
            BEARING = ( results[ 1 ]+declination+360 )%360;
        }

        @Override
        public int compareTo( DOFData another ) {
            if ( this.HEIGHT_MSL > another.HEIGHT_MSL ) {
                return -1;
            } else if ( this.HEIGHT_MSL < another.HEIGHT_MSL ) {
                return 1;
            }
            return 0;
        }
    }

}
