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

public class FormatUtils {

    private static DecimalFormat sFeetFormat;
    private static DecimalFormat sNumberFormat;

    static {
        sFeetFormat = new DecimalFormat();
        sFeetFormat.applyPattern( "#,##0.# ft" );
        sNumberFormat = new DecimalFormat();
        sNumberFormat.applyPattern( "#,##0.##" );
    }

    public static String formatFeet( float value ) {
        return sFeetFormat.format( value );
    }

    public static String formatNumber( float value ) {
        return sNumberFormat.format( value );
    }

}
