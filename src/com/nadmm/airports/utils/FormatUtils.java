/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

import java.text.DecimalFormat;

import com.nadmm.airports.wx.WxUtils;

public class FormatUtils {

    private static DecimalFormat sFeetFormat;
    private static DecimalFormat sFeetFormatMsl;
    private static DecimalFormat sFeetFormatAgl;
    private static DecimalFormat sNumberFormat;
    private static DecimalFormat sFreqFormat;
    private static DecimalFormat sVisFormat;

    static {
        sFeetFormat = new DecimalFormat();
        sFeetFormat.applyPattern( "#,##0.# ft" );
        sFeetFormatMsl = new DecimalFormat();
        sFeetFormatMsl.applyPattern( "#,##0.# ft MSL" );
        sFeetFormatAgl = new DecimalFormat();
        sFeetFormatAgl.applyPattern( "#,##0.# ft AGL" );
        sNumberFormat = new DecimalFormat();
        sNumberFormat.applyPattern( "#,##0.##" );
        sFreqFormat = new DecimalFormat();
        sFreqFormat.applyPattern( "##0.000" );
        sVisFormat = new DecimalFormat();
        sVisFormat.applyPattern( "#0.## SM" );
    }

    public static String formatFeet( float value ) {
        return sFeetFormat.format( value );
    }

    public static String formatFeetMsl( float value ) {
        return sFeetFormatMsl.format( value );
    }

    public static String formatFeetRangeMsl( int base, int top ) {
        if ( base < Integer.MAX_VALUE && top < Integer.MAX_VALUE ) {
            return String.format( "%s to %s ft MSL",
                    sNumberFormat.format( base ), sNumberFormat.format( top ) );
        } else if ( base < Integer.MAX_VALUE ) {
            return String.format( "%s ft MSL and above", sNumberFormat.format( base ) );
        } else if ( top < Integer.MAX_VALUE ) {
            return String.format( "%s ft MSL and below", sNumberFormat.format( top ) );
        } else {
            return "";
        }
    }

    public static String formatFeetAgl( float value ) {
        return sFeetFormatAgl.format( value );
    }

    public static String formatFeetRangeAgl( int base, int top ) {
        if ( base < Integer.MAX_VALUE && top < Integer.MAX_VALUE ) {
            return String.format( "%s to %s ft AGL",
                    sNumberFormat.format( base ), sNumberFormat.format( top ) );
        } else if ( base < Integer.MAX_VALUE ) {
            return String.format( "%s ft AGL and above", sNumberFormat.format( top ) );
        } else if ( top < Integer.MAX_VALUE ) {
            return String.format( "%s ft AGL and below", sNumberFormat.format( top ) );
        } else {
            return "";
        }
    }

    public static String formatNumber( float value ) {
        return sNumberFormat.format( value );
    }

    public static String formatFreq( float value ) {
        return sFreqFormat.format( value );
    }

    public static String formatVisibility( float value ) {
        return sVisFormat.format( value );
    }

    public static String formatTemperature( float temp ) {
        return String.format( "%.1f\u00B0C (%.0f\u00B0F)",
                temp, WxUtils.celsiusToFahrenheit( temp ) );
    }

    public static String formatAltimeter( float altimeterHg ) {
        float altimeterMb = WxUtils.hgToMillibar( altimeterHg );
        return String.format( "%.2f\" Hg (%s mb)",
                altimeterHg, FormatUtils.formatNumber( altimeterMb ) );
    }

}