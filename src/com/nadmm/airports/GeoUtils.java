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

        return new double[] { radLatMin, radLatMax, radLonMin, radLonMax };
    }

    public static String getCardinalDirection( float bearing ) {
        if ( bearing >= 348.76 || bearing <= 11.25 ) {
            return "N";
        } else if ( bearing >= 11.26 && bearing <= 33.75 ) {
            return "NNE";
        } else if ( bearing >= 33.76 && bearing <= 56.25 ) {
            return "NE";
        } else if ( bearing >= 56.26 && bearing <= 78.75 ) {
            return "ENE";
        } else if ( bearing >= 78.76 && bearing <= 101.25 ) {
            return "E";
        } else if ( bearing >= 101.26 && bearing <= 123.75 ) {
            return "ESE";
        } else if ( bearing >= 123.76 && bearing <= 146.25 ) {
            return "SE";
        } else if ( bearing >= 146.26 && bearing <= 168.75 ) {
            return "SSE";
        } else if ( bearing >= 168.76 && bearing <= 191.25 ) {
            return "S";
        } else if ( bearing >= 191.26 && bearing <= 213.75 ) {
            return "SSW";
        } else if ( bearing >= 213.76 && bearing <= 236.25 ) {
            return "SW";
        } else if ( bearing >= 236.26 && bearing <= 258.75 ) {
            return "WSW";
        } else if ( bearing >= 258.76 && bearing <= 281.25 ) {
            return "W";
        } else if ( bearing >= 281.26 && bearing <= 303.75 ) {
            return "WNW";
        } else if ( bearing >= 303.76 && bearing <= 326.25 ) {
            return "NW";
        } else if ( bearing >= 326.26 && bearing <= 348.75 ) {
            return "NNW";
        }

        // Just to satisfy the compiler
        return "???";
    }

}
