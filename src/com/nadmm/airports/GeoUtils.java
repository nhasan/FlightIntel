/*
 * Airports for Android
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports;

import android.location.Location;
import android.util.Log;

public class GeoUtils {

    public static final float METERS_PER_STATUTE_MILE = (float) 1609.344;
    public static final float METERS_PER_NAUTICAL_MILE = (float) 1852.0;
    // Earth's radius at major semi-axis in statute miles
    private static final double WGS84_a = (6378137.0/METERS_PER_STATUTE_MILE);
    // Earth's radius at minor semi-axis in statute miles
    private static final double WGS84_b = (6356752.3/METERS_PER_STATUTE_MILE);

    // Limits in radians
    private static final double MIN_LAT = -Math.PI/2;
    private static final double MAX_LAT = Math.PI/2;
    private static final double MIN_LON = -Math.PI;
    private static final double MAX_LON = Math.PI;

    public static double getEarthRadius( double radLat ) {
        // Earth radius at a given lattitude, according to the WGS-84 ellipsoid
        // http://en.wikipedia.org/wiki/Earth_radius
        double An = WGS84_a*WGS84_a * Math.cos( radLat );
        double Bn = WGS84_b*WGS84_b * Math.sin( radLat );
        double Ad = WGS84_a*Math.cos( radLat );
        double Bd = WGS84_b*Math.sin( radLat );
        return Math.sqrt( ( An*An + Bn*Bn )/( Ad*Ad + Bd*Bd ) );
    }

    public static double[] getBoundingBox( Location location, int r ) {
        double radLat = Math.toRadians( location.getLatitude() );
        double radLon = Math.toRadians( location.getLongitude() );

        // Calculate the radius of earth at this lattitude
        double R = getEarthRadius( radLat );
        // Calculate the angular distance
        double radDist = r/R;
        Log.i( "RADIUS", String.valueOf( R ) );
        Log.i( "NEARBY", String.valueOf( r ) );
        Log.i( "ANGLE", String.valueOf( radDist ) );
        double radLatMin = radLat-radDist;
        double radLatMax = radLat+radDist;

        double radLonMin, radLonMax;
        if ( radLatMin > MIN_LAT && radLatMax < MAX_LAT ) {
            double deltaLon = Math.asin( Math.sin( radDist )/Math.cos( radLat ) );
            radLonMin = radLon-deltaLon;
            if ( radLonMin < MIN_LON ) radLonMin += 2*Math.PI;
            radLonMax = radLon+deltaLon;
            if ( radLonMax > MAX_LON ) radLonMax -= 2*Math.PI;
        } else {
            // A pole is within the bounding box
            radLatMin = Math.max( radLatMin, MIN_LAT );
            radLatMax = Math.min( radLatMax, MAX_LAT );
            radLonMin = MIN_LON;
            radLonMax = MAX_LON;
        }

        Log.i( "LAT", String.valueOf( radLat ) );
        Log.i( "LON", String.valueOf( radLon ) );
        Log.i( "LATMIN", String.valueOf( radLatMin ) );
        Log.i( "LATMAX", String.valueOf( radLatMax ) );
        Log.i( "LONMIN", String.valueOf( radLonMin ) );
        Log.i( "LONMAX", String.valueOf( radLonMax ) );

        return new double[] { radLatMin, radLatMax, radLonMin, radLonMax };
    }

}
