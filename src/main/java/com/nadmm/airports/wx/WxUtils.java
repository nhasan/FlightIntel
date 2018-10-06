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

package com.nadmm.airports.wx;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.nadmm.airports.R;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.UiUtils;

import java.util.ArrayList;

public class WxUtils {

    private static final String CATEGORY_VFR = "VFR";
    private static final String CATEGORY_MVFR = "MVFR";
    private static final String CATEGORY_IFR = "IFR";
    private static final String CATEGORY_LIFR = "LIFR";

    private WxUtils() {}

    static public int getFlightCategoryColor( String flightCategory ) {
        int color = 0;
        if ( flightCategory.equals( CATEGORY_VFR ) ) {
            color = Color.argb( 255, 0, 160, 32 );
        } else if ( flightCategory.equals( CATEGORY_MVFR ) ) {
            color = Color.argb( 255, 0, 144, 224 );
        } else if ( flightCategory.equals( CATEGORY_IFR ) ) {
            color = Color.argb( 255, 192, 32, 0 );
        } else if ( flightCategory.equals( CATEGORY_LIFR ) ) {
            color = Color.argb( 255, 200, 0, 160 );
        }
        return color;
    }

    static public String getFlightCategoryName( String flightCategory ) {
        String name;
        if ( flightCategory.equals( CATEGORY_MVFR ) ) {
            name = "Marginal VFR";
        } else if ( flightCategory.equals( CATEGORY_LIFR ) ) {
            name = "Low IFR";
        } else {
            name = flightCategory;
        }
        return name;
    }

    static public void showColorizedDrawable( TextView tv, String flightCategory, int resid ) {
        int color = getFlightCategoryColor( flightCategory );
        UiUtils.setTintedTextViewDrawable( tv, resid, color );
    }

    static public void setFlightCategoryDrawable( TextView tv, String flightCategory ) {
        Drawable d = UiUtils.getDrawableFromCache( flightCategory );
        if ( d == null ) {
            Resources    res = tv.getResources();
            if ( flightCategory.equals( CATEGORY_VFR ) ) {
                d = res.getDrawable( R.drawable.vfr );
            } else if ( flightCategory.equals( CATEGORY_MVFR ) ) {
                d = res.getDrawable( R.drawable.mvfr );
            } else if ( flightCategory.equals( CATEGORY_IFR ) ) {
                d = res.getDrawable( R.drawable.ifr );
            } else if ( flightCategory.equals( CATEGORY_LIFR ) ) {
                d = res.getDrawable( R.drawable.lifr );
            }
        }
        if ( d != null ) {
            UiUtils.putDrawableIntoCache( flightCategory, d );
            UiUtils.setTextViewDrawable( tv, d );
        }
    }

    static public void setColorizedCeilingDrawable( TextView tv, Metar metar ) {
        SkyCondition sky = metar.skyConditions.get( metar.skyConditions.size() - 1 );
        showColorizedDrawable( tv, metar.flightCategory, sky.getDrawable() );
    }

    static public Drawable getSkycoverDrawable( Context context, Metar metar ) {
        SkyCondition sky = metar.skyConditions.get( metar.skyConditions.size()-1 );
        int color = getFlightCategoryColor( metar.flightCategory );
        return UiUtils.getTintedDrawable( context, sky.getDrawable(), color );
    }

    static public Drawable getWindBarbDrawable( Context context, Metar metar, float declination ) {
        Drawable d = null;
        if ( isWindAvailable( metar ) ) {
            int resid;
            if ( metar.windSpeedKnots >= 48 ) {
                resid = R.drawable.windbarb50;
            } else if ( metar.windSpeedKnots >= 43 ) {
                resid = R.drawable.windbarb45;
            } else if ( metar.windSpeedKnots >= 38 ) {
                resid = R.drawable.windbarb40;
            } else if ( metar.windSpeedKnots >= 33 ) {
                resid = R.drawable.windbarb35;
            } else if ( metar.windSpeedKnots >= 28 ) {
                resid = R.drawable.windbarb30;
            } else if ( metar.windSpeedKnots >= 23 ) {
                resid = R.drawable.windbarb25;
            } else if ( metar.windSpeedKnots >= 18 ) {
                resid = R.drawable.windbarb20;
            } else if ( metar.windSpeedKnots >= 13 ) {
                resid = R.drawable.windbarb15;
            } else if ( metar.windSpeedKnots >= 8 ) {
                resid = R.drawable.windbarb10;
            } else if ( metar.windSpeedKnots >= 3 ) {
                resid = R.drawable.windbarb5;
            } else {
                resid = R.drawable.windbarb0;
            }
            d = UiUtils.getRotatedDrawable( context, resid,
                    GeoUtils.applyDeclination( metar.windDirDegrees, declination ) );
        }
        return d;
    }

    static public Drawable getErrorDrawable( Context context ) {
        int resid = R.drawable.ic_error;
        String key = String.valueOf( resid );
        Drawable d = UiUtils.getDrawableFromCache( key );
        if ( d == null ) {
            Resources res = context.getResources();
            d = res.getDrawable( resid );
            UiUtils.putDrawableIntoCache( key, d );
        }
        return d;
    }

    static public void setColorizedWxDrawable( TextView tv, Metar metar, float declination ) {
        if ( metar != null ) {
            Context context = tv.getContext();
            if ( metar.isValid ) {
                Drawable d1 = getSkycoverDrawable( context, metar );
                Drawable d2 = getWindBarbDrawable( tv.getContext(), metar, declination );
                Drawable result = UiUtils.combineDrawables( context, d1, d2, 2 );
                UiUtils.setTextViewDrawable( tv, result );
            } else {
                Drawable d = getErrorDrawable( context );
                UiUtils.setTextViewDrawable( tv, d );
            }
        } else {
            UiUtils.setTextViewDrawable( tv, null );
        }
    }

    static public boolean isWindAvailable( Metar metar ) {
        return ( metar.windDirDegrees > 0 && metar.windDirDegrees < Integer.MAX_VALUE
        && metar.windSpeedKnots > 0 && metar.windSpeedKnots < Integer.MAX_VALUE );
    }

    static public float celsiusToFahrenheit( float tempCelsius ) {
        return ( tempCelsius*9/5 )+32;
    }

    static public float fahrenheitToCelsius( float tempFahrenheit ) {
        return ( tempFahrenheit-32 )*5/9;
    }

    static public float celsiusToKelvin( float tempCelsius ) {
        return (float) ( tempCelsius+273.15 );
    }

    static public float kelvinToCelsius( float tempKelvin ) {
        return (float) ( tempKelvin-273.15 );
    }

    static public float kelvinToRankine( float tempKelvin ) {
        return (float) ( celsiusToFahrenheit( kelvinToCelsius( tempKelvin ) )+459.69 );
    }

    static public float hgToMillibar( float altimHg ) {
        return (float) ( 33.8639*altimHg );
    }

    static public float getVirtualTemperature( float tempCelsius, float dewpointCelsius,
            float stationPressureHg ) {
        float tempKelvin = celsiusToKelvin( tempCelsius );
        float eMb = getVaporPressure( dewpointCelsius );
        float pMb = hgToMillibar( stationPressureHg );
        return (float) ( tempKelvin/( 1-( eMb/pMb )*( 1-0.622 ) ) );
    }

    static public float getVaporPressure( float tempCelsius ) {
        // Vapor pressure is in mb or hPa
        return (float) ( 6.1094*Math.exp( ( 17.625*tempCelsius )/( tempCelsius+243.04 ) ) );
    }

    static public float getStationPressure( float altimHg, float elevMeters ) {
        // Station pressure is in inHg
        return (float) ( altimHg*Math.pow( ( 288-0.0065*elevMeters )/288, 5.2561 ) );
    }

    static public float getRelativeHumidity( Metar metar ) {
        return getRelativeHumidity( metar.tempCelsius, metar.dewpointCelsius );
    }

    static public float getRelativeHumidity( float tempCelsius, float dewpointCelsius ) {
        float e = getVaporPressure( dewpointCelsius );
        float es = getVaporPressure( tempCelsius );
        return (e/es)*100;
    }

    static public long getPressureAltitude( Metar metar ) {
        return getPressureAltitude( metar.altimeterHg, metar.stationElevationMeters );
    }

    static public long getPressureAltitude( float altimHg, float elevMeters ) {
        float pMb = hgToMillibar( getStationPressure( altimHg, elevMeters ) );
        return Math.round( ( 1-Math.pow( pMb/1013.25, 0.190284 ) )*145366.45 );
    }

    static public long getDensityAltitude( Metar metar ) {
        return getDensityAltitude( metar.tempCelsius, metar.dewpointCelsius, metar.altimeterHg,
                metar.stationElevationMeters );
    }

    static public long getDensityAltitude( float tempCelsius, float dewpointCelsius,
            float altimHg, float elevMeters ) {
        float stationPressureHg = getStationPressure( altimHg, elevMeters );
        float tvKelvin = getVirtualTemperature( tempCelsius, dewpointCelsius, stationPressureHg );
        float tvRankine = kelvinToRankine( tvKelvin );
        return Math.round( 145366*( 1-Math.pow( 17.326*stationPressureHg/tvRankine, 0.235 ) ) );
    }

    static public long getHeadWindComponent( double windSpeed, double windDir, double d ) {
        return Math.round(windSpeed*Math.cos( Math.toRadians( windDir-d ) ) );
    }

    static public long getCrossWindComponent( double windSpeed, double windDir, double d ) {
        return Math.round( windSpeed*Math.sin( Math.toRadians( windDir-d ) ) );
    }

    static public SkyCondition getCeiling( ArrayList<SkyCondition> skyConditions ) {
        for ( SkyCondition sky : skyConditions ) {
            // Ceiling is defined as the lowest layer aloft reported as broken or overcast;
            // or the vertical visibility into an indefinite ceiling
            if ( sky.getSkyCover().equals( "BKN" )
                    || sky.getSkyCover().equals( "OVC" )
                    || sky.getSkyCover().equals( "OVX" ) ) {
                return sky;
            }
        }
        return SkyCondition.create( "NSC", 12000 );
    }

    static public String computeFlightCategory( ArrayList<SkyCondition> skyConditions,
            float visibilitySM ) {
        String flightCategory;
        SkyCondition sky = getCeiling( skyConditions );
        int ceiling = sky.getCloudBaseAGL();
        if ( ceiling < 500 || visibilitySM < 1.0 ) {
            flightCategory = "LIFR";
        } else if ( ceiling < 1000 || visibilitySM < 3.0 ) {
            flightCategory = "IFR";
        } else if ( ceiling <= 3000 || visibilitySM <= 5.0 ) {
            flightCategory = "MVFR";
        } else {
            flightCategory = "VFR";
        }

        return flightCategory;
    }

    public static String decodeTurbulenceIntensity( int intensity ) {
        switch ( intensity ) {
        case 1:
            return "Light";
        case 2:
        case 4:
            return "Occasional, Moderate";
        case 3:
        case 5:
            return "Frequent, Moderate";
        case 6:
        case 8:
            return "Occasional, Severe";
        case 7:
        case 9:
            return "Frequent, Severe";
        default:
            return "None";
        }
    }

    public static String decodeIcingIntensity( int icing ) {
        switch ( icing ) {
        case 1:
        case 2:
        case 3:
            return "Light icing";
        case 4:
        case 5:
        case 6:
            return "Moderate icing";
        case 7:
        case 8:
        case 9:
            return "Severe icing";
        default:
            return "No icing";
        }
    }

}
