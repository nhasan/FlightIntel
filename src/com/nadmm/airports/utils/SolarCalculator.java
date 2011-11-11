/*
 * FlightIntel for Pilots
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

package com.nadmm.airports.utils;

import java.util.Calendar;
import java.util.TimeZone;

import android.location.Location;

public class SolarCalculator {

    public static final double ASTRONOMICAL = 108.0;
    public static final double NAUTICAL = 102.0;
    public static final double CIVIL = 96.0;
    public static final double OFFICIAL = 90.83333333;

    final private int MSEC_PER_HOUR = 60*60*1000;

    private Location mLocation;
    private TimeZone mTimeZone;

    public SolarCalculator( Location location, TimeZone timeZone ) {
        mLocation = location;
        mTimeZone = timeZone;
    }

    public Calendar getSunriseTime( double zenith, Calendar date ) {
        date.setTimeZone( mTimeZone );
        double longitudeHour = getSunriseLongitudeHour( date );
        double meanAnomaly = getMeanAnomaly( longitudeHour );
        double sunTrueLongitude = getSunTrueLongitude( meanAnomaly );
        double cosineSunLocalHour = getCosineSunLocalHour( sunTrueLongitude, zenith );

        if ( cosineSunLocalHour < -1.0 || cosineSunLocalHour > 1.0 ) {
            return null;
        }

        double sunLocalHour = getSunLocalHourForSunrise( cosineSunLocalHour );
        double localMeanTime = getLocalMeanTime( sunTrueLongitude, longitudeHour, sunLocalHour );
        double localTime = getLocalTime( localMeanTime, date );

        return getTimeAsCalendar( localTime, date );
    }

    public Calendar getSunsetTime( double zenith, Calendar date ) {
        date.setTimeZone( mTimeZone );
        double longitudeHour = getSunsetLongitudeHour( date );
        double meanAnomaly = getMeanAnomaly( longitudeHour );
        double sunTrueLongitude = getSunTrueLongitude( meanAnomaly );
        double cosineSunLocalHour = getCosineSunLocalHour( sunTrueLongitude, zenith );

        if ( cosineSunLocalHour < -1.0 || cosineSunLocalHour > 1.0 ) {
            return null;
        }

        double sunLocalHour = getSunLocalHourForSunset( cosineSunLocalHour );
        double localMeanTime = getLocalMeanTime( sunTrueLongitude, longitudeHour, sunLocalHour );
        double localTime = getLocalTime( localMeanTime, date );

        return getTimeAsCalendar( localTime, date );
    }

    private double getBaseLongitudeHour() {
        return mLocation.getLongitude()/15.0;
    }

    private double getSunriseLongitudeHour( Calendar date ) {
        return date.get( Calendar.DAY_OF_YEAR )+( ( 6-getBaseLongitudeHour() )/24 );
    }

    private double getSunsetLongitudeHour( Calendar date ) {
        return date.get( Calendar.DAY_OF_YEAR )+( ( 18-getBaseLongitudeHour() )/24 );
    }

    private double getMeanAnomaly( double longitudeHour ) {
        return ( longitudeHour*0.9856 )-3.289;
    }

    private double getSunTrueLongitude( double meanAnomaly ) {
        double sunTrueLongitude = meanAnomaly
                +( 1.916*Math.sin( Math.toRadians( meanAnomaly ) ) )
                +( 0.020*Math.sin( Math.toRadians( meanAnomaly*2 ) ) )
                +282.634;
        if ( sunTrueLongitude < 0 ) {
            sunTrueLongitude += 360;
        } else if ( sunTrueLongitude > 360 ) {
            sunTrueLongitude -= 360;
        }
        return sunTrueLongitude;
    }

    private double getRightAscention( double sunTrueLongitude ) {
        double tanL = Math.tan( Math.toRadians( sunTrueLongitude ) );
        double rightAscention = Math.toDegrees( Math.atan( tanL*0.91764 ) );
        if ( rightAscention < 0 ) {
            rightAscention += 360;
        } else if ( rightAscention > 360 ) {
            rightAscention -= 360;
        }
        double longitudeQuadrant = Math.floor( sunTrueLongitude/90 )*90;
        double rightAscensionQuadrant = Math.floor( rightAscention/90 )*90;
        return ( rightAscention+( longitudeQuadrant-rightAscensionQuadrant ) )/15;
    }

    private double getSineOfSunDeclination( double sunTrueLong ) {
        return Math.sin( Math.toRadians( sunTrueLong ) )*0.39782;
    }

    private double getCosineOfSunDeclination( double sineSunDeclination ) {
        return Math.cos( Math.asin( sineSunDeclination ) );
    }

    private double getCosineSunLocalHour( double sunTrueLong, double zenith ) {
        double sineSunDeclination = getSineOfSunDeclination( sunTrueLong );
        double cosineSunDeclination = getCosineOfSunDeclination( sineSunDeclination );
        double sineLatitude = Math.sin( Math.toRadians( mLocation.getLatitude() ) );
        double cosineLatitude = Math.cos( Math.toRadians( mLocation.getLatitude() ) );
        double dividend = Math.cos( Math.toRadians( zenith ) )
                -( sineSunDeclination*sineLatitude );
        double divisor = cosineSunDeclination*cosineLatitude;
        return dividend/divisor;
    }

    private double getSunLocalHourForSunrise( double cosineSunLocalHour ) {
        return ( 360-Math.toDegrees( Math.acos( cosineSunLocalHour ) ) )/15;
    }

    private double getSunLocalHourForSunset( double cosineSunLocalHour ) {
        return Math.toDegrees( Math.acos( cosineSunLocalHour ) )/15;
    }

    private double getLocalMeanTime( double sunTrueLongitude, double longitudeHour,
            double sunLocalHour ) {
        double rightAscention = getRightAscention( sunTrueLongitude );
        double localMeanTime = sunLocalHour+rightAscention-( longitudeHour*0.06571 )-6.622;
        if ( localMeanTime < 0 ) {
            localMeanTime += 24;
        } else if ( localMeanTime > 24 ) {
            localMeanTime -= 24;
        }
        return localMeanTime;
    }

    private double adjustForDST( double localMeanTime, Calendar date ) {
        double localTime = localMeanTime;
        if ( mTimeZone.inDaylightTime( date.getTime() ) ) {
            localTime += mTimeZone.getDSTSavings()/MSEC_PER_HOUR;
        }
        if ( localTime > 24 ) {
            localTime -= 24;
        }
        return localTime;
    }

    private double getUtcTime( double localMeanTime ) {
        double utcTime = localMeanTime-getBaseLongitudeHour();
        return utcTime;
    }

    private double getLocalTime( double localMeanTime, Calendar date ) {
        double utcTime = getUtcTime( localMeanTime );
        double utcOffset = date.get( Calendar.ZONE_OFFSET )/MSEC_PER_HOUR;
        double localTime = utcTime+utcOffset;
        return adjustForDST( localTime, date );        
    }

    private Calendar getTimeAsCalendar( double time, Calendar date ) {
        int hours = ( int) Math.floor( time );
        int minutes = (int ) ( ( time-hours )*60 );
        if ( minutes == 60 ) {
            minutes = 0;
            ++hours;
        }

        Calendar result = (Calendar) date.clone();
        result.set( Calendar.HOUR_OF_DAY, hours );
        result.set( Calendar.MINUTE, minutes );
        result.set( Calendar.SECOND, 0 );

        return result;
    }

}
