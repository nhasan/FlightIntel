/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.e6b;

import android.database.MatrixCursor;
import android.provider.BaseColumns;

import com.nadmm.airports.R;

public class E6bMenuCursor extends MatrixCursor {

    public static final String ITEM_ICON = "ITEM_ICON";
    public static final String ITEM_TITLE = "ITEM_TITLE";
    public static final String ITEM_SUMMARY = "ITEM_SUMMARY";

    private final static String[] sColumnNames = new String[]
            { BaseColumns._ID, ITEM_ICON, ITEM_TITLE, ITEM_SUMMARY };

    public E6bMenuCursor( long menuId ) {
        super( sColumnNames );

        if ( menuId == R.id.CATEGORY_MAIN ) {
            newRow().add( R.id.CATEGORY_TIME )
                .add( R.drawable.airport )
                .add( "Time" )
                .add( "UTC clock, local clock, stop watch" );
        } else if ( menuId == R.id.CATEGORY_TIME ) {
            newRow().add( R.id.TIME_CLOCKS )
                .add( R.drawable.airport )
                .add( "Clocks" )
                .add( "Display UTC clock, local clock" );
            newRow().add( R.id.TIME_STOPWATCH )
                .add( R.drawable.airport )
                .add( "Stop Watch" )
                .add( "Stop watch for timing legs and approaches" );
            newRow().add( R.id.TIME_COUNTDOWN )
                .add( R.drawable.airport )
                .add( "Countdown Timer" )
                .add( "Countdown timer for timing approaches" );
        }
    }

}
