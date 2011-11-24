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

package com.nadmm.airports;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.Tower8;

public class RemarkDetailsActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        AirportRemarksTask task = new AirportRemarksTask();
        task.execute( siteNumber );
    }

    private final class AirportRemarksTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 3 ];

            Cursor apt = mDbManager.getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            cursors[ 1 ] = builder.query( db,
                    new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=? "
                    +"AND substr("+Remarks.REMARK_NAME+", 1, 2) not in ('A3', 'A4', 'A5', 'A6') "
                    +"AND substr("+Remarks.REMARK_NAME+", 1, 3) not in ('A23', 'A17')"
                    +"AND "+Remarks.REMARK_NAME
                    +" not in ('E147', 'A3', 'A11', 'A12', 'A13', 'A14', 'A15', 'A16', 'A17', "
                    +"'A24', 'A70', 'A75', 'A81', 'A81 1', 'A82')",
                    new String[] { siteNumber }, null, null, Remarks.REMARK_NAME, null );

            String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
            builder = new SQLiteQueryBuilder();
            builder.setTables( Tower8.TABLE_NAME );
            cursors[ 2 ] = builder.query( db, new String[] { "*" },
                    Tower8.FACILITY_ID+"=? ",
                    new String[] { faaCode }, null, null, null, null );

            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            setContentView( R.layout.remarks_detail_view );

            Cursor apt = result[ 0 ];
            showAirportTitle( apt );
            showRemarksDetails( result );
            showAirspaceDetails( result );
        }

    }

    protected void showRemarksDetails( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_remarks_layout );
        Cursor rmk = result[ 1 ];
        if ( rmk.moveToFirst() ) {
            do {
                String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                addRemarkRow( layout, remark );
            } while ( rmk.moveToNext() );
        }
    }

    protected void showAirspaceDetails( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_remarks_layout );

        Cursor twr8 = result[ 2 ];
        if ( twr8.moveToFirst() ) {
            String airspace = twr8.getString( twr8.getColumnIndex( Tower8.AIRSPACE_TYPES ) );
            String remark = "";
            if ( airspace.charAt( 0 ) == 'Y' ) {
                remark += "CLASS B";
            }
            if ( airspace.charAt( 1 ) == 'Y' ) {
                if ( remark.length() > 0 ) {
                    remark += ", ";
                }
                remark += "CLASS C";
            }
            if ( airspace.charAt( 2 ) == 'Y' ) {
                if ( remark.length() > 0 ) {
                    remark += ", ";
                }
                remark += "CLASS D";
            }
            if ( airspace.charAt( 3 ) == 'Y' ) {
                if ( remark.length() > 0 ) {
                    remark += ", ";
                }
                remark += "CLASS E";
            }
            remark = "AIRSPACE: "+remark;
            String hours = twr8.getString( twr8.getColumnIndex( Tower8.AIRSPACE_HOURS ) );
            if ( hours.length() > 0 ) {
                remark += " ("+hours+")";
            }
            addBulletedRow( layout, remark );
        }
    }

    protected void addRemarkRow( LinearLayout layout, String remark ) {
        int index = remark.indexOf( ' ' );
        if ( index != -1 ) {
            while ( remark.charAt( index ) == ' ' ) {
                ++index;
            }
            remark = remark.substring( index );
        }

        addBulletedRow( layout, remark );
    }

}
