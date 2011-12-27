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

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.nadmm.airports.R;

public class WxUtils {

    static public final float StdSeaLevelTemp = 15;
    static public final float StdSeaLevelPressure = (float) 1013.25;

    static public int getFlightCategoryColor( String flightCategory ) {
        int color = 0;
        if ( flightCategory.equals( "VFR" ) ) {
            color = Color.argb( 208, 80, 176, 96 );
        } else if ( flightCategory.equals( "MVFR" ) ) {
            color = Color.argb( 208, 48, 128, 208 );
        } else if ( flightCategory.equals( "IFR" ) ) {
            color = Color.argb( 208, 208, 64, 48 );
        } else if ( flightCategory.equals( "LIFR" ) ) {
            color = Color.argb( 208, 208, 48, 128 );
        }
        return color;
    }

    static public void showWxFlightCategoryIcon( TextView tv, String flightCategory ) {
        if ( flightCategory != null ) {
            Resources res = tv.getResources();
            // Get a mutable copy of the drawable so each can be set to a different color
            Drawable d = res.getDrawable( R.drawable.circle ).mutate();
            int color = getFlightCategoryColor( flightCategory );
            d.setColorFilter( color, PorterDuff.Mode.SRC_ATOP );
            tv.setCompoundDrawablesWithIntrinsicBounds( d, null, null, null );
            tv.setCompoundDrawablePadding( 5 );
        }
    }

    static public Drawable colorizeFlightCategoryDrawable( String flightCategory,
            Resources res, int resid ) {
        // Get a mutable copy of the drawable so each can be set to a different color
        Drawable d = res.getDrawable( resid ).mutate();
        if ( flightCategory != null ) {
            int color = getFlightCategoryColor( flightCategory );
            d.setColorFilter( color, PorterDuff.Mode.SRC_IN );
        }

        return d;
    }

    static public float celsiusToFahrenheit( float degrees ) {
        return ( degrees*9/5 )+32;
    }

    static public float fahrenheitToCelsius( float degrees ) {
        return ( degrees-32 )*5/9;
    }

    static public float celsiusToKelvins( float degrees ) {
        return (float) (degrees+273.15);
    }

    static public float inchesHgToMillibar( float altimHg ) {
        return (float) (33.8639*altimHg);
    }

    static public float getRelativeHumidity( float temp, float dewPoint ) {
        float e = (float) ( 6.1094*Math.exp( (17.625*dewPoint)/(dewPoint+243.04) ) );
        float es = (float) ( 6.1094*Math.exp( (17.625*temp)/(temp+243.04) ) );
        return (e/es)*100;
    }
   
    static public int getDensityAltitudeFeet( float temp, float altimHg ) {
        float altimMb = (float) inchesHgToMillibar(altimHg);
        float p = altimMb/StdSeaLevelPressure;
        float t = (float)  celsiusToKelvins( temp )/celsiusToKelvins( StdSeaLevelTemp );
        return (int) ( 145442.156*( 1-Math.pow( p/t, 0.235 ) ) );
    }

}
