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

package com.nadmm.airports.afd;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Attendance;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;

public class AttendanceDetailsActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.attendance_detail_view ) );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        AirportAttendanceTask task = new AirportAttendanceTask();
        task.execute( siteNumber );
    }

    private final class AirportAttendanceTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 3 ];
            
            cursors[ 0 ] = getAirportDetails( siteNumber );

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Attendance.TABLE_NAME );
            cursors[ 1 ] = builder.query( db,
                    new String[] { Attendance.ATTENDANCE_SCHEDULE },
                    Attendance.SITE_NUMBER+"=?", new String[] { siteNumber }, 
                    null, null, Attendance.SEQUENCE_NUMBER, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            cursors[ 2 ] = builder.query( db,
                    new String[] { Remarks.REMARK_TEXT },
                    Attendance.SITE_NUMBER+"=? "
                    +"AND substr("+Remarks.REMARK_NAME+", 1, 3)='A17'",
                    new String[] { siteNumber }, null, null, null, null );

            return cursors;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            showDetails( result );
        }

    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        setActionBarTitle( apt );
        showAirportTitle( apt );
        showAttendanceDetails( result );
        showAttendanceRemarks( result );

        setContentShown( true );
    }

    protected void showAttendanceDetails( Cursor[] result ) {
        Cursor att = result[ 1 ];
        if ( att.moveToFirst() ) {
            LinearLayout layout = (LinearLayout) findViewById( R.id.attendance_content_layout );
            do {
                String schedule = att.getString(
                        att.getColumnIndex( Attendance.ATTENDANCE_SCHEDULE ) );
                String[] parts = schedule.split( "/" );
                addSpacing( layout );
                LinearLayout table = (LinearLayout) inflate( R.layout.attendance_detail_item );
                if ( parts.length == 3 ) {
                    addRow( table, "Months", parts[ 0 ] );
                    addSeparator( table );
                    addRow( table, "Days", parts[ 1 ] );
                    addSeparator( table );
                    addRow( table, "Hours", parts[ 2 ] );
                } else {
                    addRow( table, "Attendance", schedule );
                }
                layout.addView( table, 1, new LinearLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
            } while ( att.moveToNext() );
        }
    }

    protected void showAttendanceRemarks( Cursor[] result ) {
        Cursor rmk = result[ 2 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.attendance_remark_layout );
        if ( rmk.moveToFirst() ) {
            layout.setVisibility( View.VISIBLE );
            do {
                String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                addRemarkRow( layout, remark );
            } while ( rmk.moveToNext() );            
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

    protected void addSpacing( LinearLayout layout ) {
        View separator = new View( this );
        layout.addView( separator, 1, new LayoutParams( LayoutParams.MATCH_PARENT, 10 ) );
    }

}
