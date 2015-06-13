/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import java.util.Arrays;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.provider.BaseColumns;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Awos1;
import com.nadmm.airports.DatabaseManager.LocationColumns;
import com.nadmm.airports.DatabaseManager.Wxs;
import com.nadmm.airports.utils.GeoUtils;

public class NearbyWxCursor extends MatrixCursor {

    // Build a cursor out of the sorted wx station list
    private static final String[] sColumns = new String[] {
            BaseColumns._ID,
            Wxs.STATION_ID,
            Awos1.WX_SENSOR_IDENT,
            Awos1.WX_SENSOR_TYPE,
            Awos1.STATION_FREQUENCY,
            Awos1.SECOND_STATION_FREQUENCY,
            Awos1.STATION_PHONE_NUMBER,
            Wxs.STATION_NAME,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE,
            Wxs.STATION_ELEVATOIN_METER,
            Wxs.STATION_LATITUDE_DEGREES,
            Wxs.STATION_LONGITUDE_DEGREES,
            LocationColumns.DISTANCE,
            LocationColumns.BEARING
    };

    public NearbyWxCursor( SQLiteDatabase db, Location location, int radius ) {
        super( sColumns );

        double declination = GeoUtils.getMagneticDeclination( location );

        // Get the bounding box first to do a quick query as a first cut
        double[] box = GeoUtils.getBoundingBoxRadians( location, radius );
        double radLatMin = box[ 0 ];
        double radLatMax = box[ 1 ];
        double radLonMin = box[ 2 ];
        double radLonMax = box[ 3 ];

        // Check if 180th Meridian lies within the bounding Box
        boolean isCrossingMeridian180 = ( radLonMin > radLonMax );

        String selection = "("
                +"x."+Wxs.STATION_LATITUDE_DEGREES+">=? AND "
                +"x."+Wxs.STATION_LATITUDE_DEGREES+"<=?"
                +") AND (x."+Wxs.STATION_LONGITUDE_DEGREES+">=? "
                +(isCrossingMeridian180? "OR " : "AND ")
                +"x."+Wxs.STATION_LONGITUDE_DEGREES+"<=?)";
        String[] selectionArgs = {
                String.valueOf( Math.toDegrees( radLatMin ) ),
                String.valueOf( Math.toDegrees( radLatMax ) ),
                String.valueOf( Math.toDegrees( radLonMin ) ),
                String.valueOf( Math.toDegrees( radLonMax ) )
                };

        Cursor c = WxCursorHelper.query( db, selection, selectionArgs, null, null, null, null );

        if ( c.moveToFirst() ) {
            AwosData[] awosList = new AwosData[ c.getCount() ];
            do {
                AwosData awos = new AwosData( c, location, declination );
                awosList[ c.getPosition() ] = awos;
            } while ( c.moveToNext() );

            // Sort the airport list by distance from current location
            Arrays.sort( awosList );

            for ( AwosData awos : awosList ) {
                if ( awos.STATUS == null || awos.STATUS.equals( "N" ) ) {
                    continue;
                }
                if ( awos.DISTANCE > radius ) {
                    continue;
                }
                MatrixCursor.RowBuilder row = newRow();
                row.add( getPosition() )
                    .add( awos.ICAO_CODE )
                    .add( awos.SENSOR_IDENT )
                    .add( awos.SENSOR_TYPE )
                    .add( awos.FREQUENCY )
                    .add( awos.FREQUENCY2 )
                    .add( awos.PHONE )
                    .add( awos.NAME )
                    .add( awos.CITY )
                    .add( awos.STATE )
                    .add( awos.ELEVATION )
                    .add( awos.LATITUDE )
                    .add( awos.LONGITUDE )
                    .add( awos.DISTANCE )
                    .add( awos.BEARING );
            }
        }
        c.close();
    }

    private final class AwosData implements Comparable<AwosData> {

        public String ICAO_CODE;
        public String NAME;
        public String STATUS;
        public String SENSOR_IDENT;
        public String SENSOR_TYPE;
        public String FREQUENCY;
        public String FREQUENCY2;
        public String PHONE;
        public String CITY;
        public String STATE;
        public int ELEVATION;
        public double LATITUDE;
        public double LONGITUDE;
        public double DISTANCE;
        public double BEARING;

        public AwosData( Cursor c, Location location, double declination ) {
            ICAO_CODE = c.getString( c.getColumnIndex( Wxs.STATION_ID ) );
            NAME = c.getString( c.getColumnIndex( Wxs.STATION_NAME ) );
            STATUS = c.getString( c.getColumnIndex( Awos1.COMMISSIONING_STATUS ) );
            SENSOR_IDENT = c.getString( c.getColumnIndex( Awos1.WX_SENSOR_IDENT ) );
            SENSOR_TYPE = c.getString( c.getColumnIndex( Awos1.WX_SENSOR_TYPE ) );
            FREQUENCY = c.getString( c.getColumnIndex( Awos1.STATION_FREQUENCY ) );
            FREQUENCY2 = c.getString( c.getColumnIndex( Awos1.SECOND_STATION_FREQUENCY ) );
            PHONE = c.getString( c.getColumnIndex( Awos1.STATION_PHONE_NUMBER ) );
            CITY = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
            STATE = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
            ELEVATION = c.getInt( c.getColumnIndex( Wxs.STATION_ELEVATOIN_METER ) );
            LATITUDE = c.getDouble( c.getColumnIndex( Wxs.STATION_LATITUDE_DEGREES ) );
            LONGITUDE = c.getDouble( c.getColumnIndex( Wxs.STATION_LONGITUDE_DEGREES ) );

            if ( SENSOR_TYPE == null || SENSOR_TYPE.length() == 0 ) {
                SENSOR_TYPE = "ASOS/AWOS";
            }

            // Now calculate the distance to this wx station
            float[] results = new float[ 2 ];
            Location.distanceBetween( location.getLatitude(), location.getLongitude(),
                    LATITUDE, LONGITUDE, results );
            DISTANCE = results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE;
            BEARING = GeoUtils.applyDeclination( results[ 1 ], declination );
        }

        @Override
        public int compareTo( AwosData another ) {
            if ( this.DISTANCE > another.DISTANCE ) {
                return 1;
            } else if ( this.DISTANCE < another.DISTANCE ) {
                return -1;
            }
            return 0;
        }
    }

}
