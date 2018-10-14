/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2016 Nadeem Hasan <nhasan@nadmm.com>
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
import android.location.Location;
import android.provider.BaseColumns;
import android.support.v4.util.Pair;

import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.LocationColumns;
import com.nadmm.airports.data.DatabaseManager.States;
import com.nadmm.airports.utils.DbUtils;
import com.nadmm.airports.utils.GeoUtils;

import java.util.Arrays;

public class NearbyAirportsCursor extends MatrixCursor {

    private static final String[] sColumns = new String[] {
            BaseColumns._ID,
            Airports.SITE_NUMBER,
            Airports.ICAO_CODE,
            Airports.FAA_CODE,
            Airports.FACILITY_NAME,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE,
            Airports.FUEL_TYPES,
            Airports.CTAF_FREQ,
            Airports.UNICOM_FREQS,
            Airports.ELEVATION_MSL,
            Airports.STATUS_CODE,
            Airports.FACILITY_USE,
            States.STATE_NAME,
            LocationColumns.DISTANCE,
            LocationColumns.BEARING
    };

    public NearbyAirportsCursor( SQLiteDatabase db, Location location, int radius,
                                 String extraSelection ) {
        super( sColumns );

        float declination = GeoUtils.getMagneticDeclination( location );

        Pair<String, String[]> selection = DbUtils.getBoundingBoxSelection(
                Airports.REF_LATTITUDE_DEGREES, Airports.REF_LONGITUDE_DEGREES, location, radius );
        String select = selection.first;
        if ( extraSelection != null ) {
            select = select.concat( extraSelection );
        }

        Cursor c = AirportsCursorHelper.query( db, select, selection.second,
                null, null, null, null );

        if ( c.moveToFirst() ) {
            AirportData[] airports = new AirportData[ c.getCount() ];
            do {
                AirportData airport = new AirportData();
                airport.setFromCursor( c, location, declination );
                airports[ c.getPosition() ] = airport;
            } while ( c.moveToNext() );

            // Sort the airport list by distance from current location
            Arrays.sort( airports );

            // Build a cursor out of the sorted airport list
            for ( AirportData airport : airports ) {
                if ( airport.DISTANCE >= 0 && airport.DISTANCE <= radius ) {
                    MatrixCursor.RowBuilder row = newRow();
                    row.add( getPosition() )
                        .add( airport.SITE_NUMBER )
                        .add( airport.ICAO_CODE)
                        .add( airport.FAA_CODE )
                        .add( airport.FACILITY_NAME )
                        .add( airport.ASSOC_CITY )
                        .add( airport.ASSOC_STATE )
                        .add( airport.FUEL_TYPES )
                        .add( airport.CTAF_FREQ )
                        .add( airport.UNICOM_FREQ )
                        .add( airport.ELEVATION_MSL )
                        .add( airport.STATUS_CODE )
                        .add( airport.FACILITY_USE )
                        .add( airport.STATE_NAME )
                        .add( airport.DISTANCE )
                        .add( airport.BEARING );
                }
            }
        }
        c.close();
    }

    // This data class allows us to sort the airport list based in distance
    private final class AirportData implements Comparable<AirportData> {

        public String SITE_NUMBER;
        public String ICAO_CODE;
        public String FAA_CODE;
        public String FACILITY_NAME;
        public String ASSOC_CITY;
        public String ASSOC_STATE;
        public String FUEL_TYPES;
        public String UNICOM_FREQ;
        public String CTAF_FREQ;
        public String ELEVATION_MSL;
        public String STATUS_CODE;
        public String FACILITY_USE;
        public String STATE_NAME;
        public double LATITUDE;
        public double LONGITUDE;
        public float DISTANCE;
        public float BEARING;

        private void setFromCursor( Cursor c, Location location, float declination ) {
            SITE_NUMBER = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            FACILITY_NAME = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
            ICAO_CODE = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
            FAA_CODE = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
            ASSOC_CITY = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
            ASSOC_STATE = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
            FUEL_TYPES = c.getString( c.getColumnIndex( Airports.FUEL_TYPES ) );
            ELEVATION_MSL = c.getString( c.getColumnIndex( Airports.ELEVATION_MSL ) );
            UNICOM_FREQ = c.getString( c.getColumnIndex( Airports.UNICOM_FREQS ) );
            CTAF_FREQ = c.getString( c.getColumnIndex( Airports.CTAF_FREQ ) );
            STATUS_CODE = c.getString( c.getColumnIndex( Airports.STATUS_CODE ) );
            FACILITY_USE = c.getString( c.getColumnIndex( Airports.FACILITY_USE ) );
            STATE_NAME = c.getString( c.getColumnIndex( States.STATE_NAME ) );
            LATITUDE = c.getDouble( c.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
            LONGITUDE = c.getDouble( c.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );

            // Now calculate the distance to this airport
            float[] results = new float[ 2 ];
            Location.distanceBetween( location.getLatitude(), location.getLongitude(),
                    LATITUDE, LONGITUDE, results );
            DISTANCE = results[ 0 ]/GeoUtils.METERS_PER_NAUTICAL_MILE;
            BEARING = ( results[ 1 ]+declination+360 )%360;
        }

        @Override
        public int compareTo( AirportData another ) {
            if ( this.DISTANCE > another.DISTANCE ) {
                return 1;
            } else if ( this.DISTANCE < another.DISTANCE ) {
                return -1;
            }
            return 0;
        }

    }

}
