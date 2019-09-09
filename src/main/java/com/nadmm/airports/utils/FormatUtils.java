/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2019 Nadeem Hasan <nhasan@nadmm.com>
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
import java.text.NumberFormat;
import java.util.Locale;

public class FormatUtils {

    private final static DecimalFormat sFeetFormat;
    private final static DecimalFormat sFeetFormatMsl;
    private final static DecimalFormat sFeetFormatAgl;
    private final static DecimalFormat sNumberFormat;
    private final static DecimalFormat sFreqFormat;
    private final static DecimalFormat sSMFormat;
    private final static DecimalFormat sNMFormat;
    private final static NumberFormat sDollarFormat;

    private FormatUtils() {}

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
        sSMFormat = new DecimalFormat();
        sSMFormat.applyPattern( "#0.## SM" );
        sNMFormat = new DecimalFormat();
        sNMFormat.applyPattern( "#0.# NM" );
        sDollarFormat = DecimalFormat.getCurrencyInstance();
    }

    public static String formatFeet( float value ) {
        return sFeetFormat.format( value );
    }

    public static String formatFeetMsl( float value ) {
        return sFeetFormatMsl.format( value );
    }

    public static String formatFeetRangeMsl( int base, int top ) {
        if ( base == 0 && top < Integer.MAX_VALUE ) {
            return String.format( "Surface to %s ft MSL", sNumberFormat.format( top ) );
        } else if ( base < Integer.MAX_VALUE && top < Integer.MAX_VALUE ) {
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
        if ( base == 0 && top < Integer.MAX_VALUE ) {
            return String.format( "Surface to %s ft AGL", sNumberFormat.format( top ) );
        } else if ( base < Integer.MAX_VALUE && top < Integer.MAX_VALUE ) {
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

    public static String formatStatuteMiles( float value ) {
        return sSMFormat.format( value );
    }

    public static String formatNauticalMiles( float value ) {
        return sNMFormat.format( value );
    }

    public static String formatTemperature( float tempC ) {
        return String.format( Locale.US, "%s (%s)",
                formatTemperatureC( tempC ), formatTemperatureF( tempC ) );
    }

    public static String formatTemperatureC( float tempC ) {
        return String.format( Locale.US, "%.1f\u00B0C", tempC );
    }

    public static String formatTemperatureF( float tempC ) {
        return String.format( Locale.US, "%.0f\u00B0F", WxUtils.celsiusToFahrenheit( tempC ) );
    }

    public static String formatAltimeter( float altimeterHg ) {
        float altimeterMb = WxUtils.hgToMillibar( altimeterHg );
        return String.format( Locale.US, "%.2f\" Hg (%s mb)",
                altimeterHg, FormatUtils.formatNumber( altimeterMb ) );
    }

    public static String formatAltimeterHg( float altimeterHg ) {
        return String.format( Locale.US, "%.2f\" Hg", altimeterHg );
    }

    public static String formatAltimeterMb( float altimeterHg ) {
        float altimeterMb = WxUtils.hgToMillibar( altimeterHg );
        return String.format( Locale.US, "%s mb", FormatUtils.formatNumber( altimeterMb ) );
    }

    public static String formatDegrees( float degrees ) {
        return String.format( Locale.US, "%.02f\u00B0", degrees );
    }

    public static String formatDegrees( int degrees ) {
        return String.format( Locale.US, "%03d\u00B0", degrees );
    }

    public static String formatCurrency( double amount ) {
        return sDollarFormat.format( amount );
    }

}
