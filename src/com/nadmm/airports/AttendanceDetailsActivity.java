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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Attendance;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;

public class AttendanceDetailsActivity extends ActivityBase {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        AirportAttendanceTask task = new AirportAttendanceTask();
        task.execute( siteNumber );
    }

    private final class AirportAttendanceTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 3 ];
            
            cursors[ 0 ] = mDbManager.getAirportDetails( siteNumber );

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Attendance.TABLE_NAME );
            cursors[ 1 ] = builder.query( db,
                    new String[] { Attendance.ATTENDANCE_SCHEDULE },
                    Runways.SITE_NUMBER+"=? ", new String[] { siteNumber }, 
                    null, null, Attendance.SEQUENCE_NUMBER, null );

            builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            cursors[ 2 ] = builder.query( db,
                    new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=? "
                    +"AND substr("+Remarks.REMARK_NAME+", 1, 3)='A17'",
                    new String[] { siteNumber }, null, null, null, null );

            return cursors;
        }

        @Override
        protected void onPostExecute( Cursor[] result ) {
            View view = mInflater.inflate( R.layout.attendance_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.attendance_top_layout );

            // Title
            Cursor apt = result[ 0 ];
            showAirportTitle( mMainLayout, apt );

            showAttendanceDetails( result );
            showAttendanceRemarks( result );

            // Cleanup cursors
            for ( Cursor c : result ) {
                if ( c != null ) {
                    c.close();
                }
            }
        }

    }

    protected void showAttendanceDetails( Cursor[] result ) {
        Cursor att = result[ 1 ];
        if ( att.moveToFirst() ) {
            LinearLayout layout = (LinearLayout) mMainLayout.findViewById(
                    R.id.attendance_content_layout );
            do {
                String schedule = att.getString(
                        att.getColumnIndex( Attendance.ATTENDANCE_SCHEDULE ) );
                String[] parts = schedule.split( "/" );
                if ( parts.length == 3 ) {
                    addSpacing( layout );
                    TableLayout table = (TableLayout) mInflater.inflate(
                            R.layout.attendance_detail_item, null );
                    addRow( table, "Months", parts[ 0 ] );
                    addSeparator( table );
                    addRow( table, "Days", parts[ 1 ] );
                    addSeparator( table );
                    addRow( table, "Hours", parts[ 2 ] );
                    layout.addView( table, 1, new LinearLayout.LayoutParams(
                            LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
                }
            } while ( att.moveToNext() );
        }
    }

    protected void showAttendanceRemarks( Cursor[] result ) {
        Cursor rmk = result[ 2 ];
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById(
                R.id.attendance_remark_layout );
        if ( rmk.moveToFirst() ) {
            layout.setVisibility( View.VISIBLE );
            do {
                String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                addRemarkRow( layout, remark );
            } while ( rmk.moveToNext() );            
        }
    }

    protected void addRow( TableLayout table, String label, String value ) {
        TableRow row = (TableRow) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tv = new TextView( this );
        tv.setText( label );
        tv.setSingleLine();
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 4, 2, 2, 2 );
        row.addView( tv, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        tv = new TextView( this );
        tv.setText( value );
        tv.setSingleLine();
        tv.setGravity( Gravity.RIGHT );
        tv.setPadding( 2, 2, 4, 2 );
        row.addView( tv, new TableRow.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1f ) );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
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

    protected void addBulletedRow( LinearLayout layout, String remark ) {
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 10, 4, 2, 4 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 4, 4, 12, 4 );
        tv.setText( remark );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( TableLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, new LayoutParams( LayoutParams.FILL_PARENT, 1 ) );
    }

    protected void addSpacing( LinearLayout layout ) {
        View separator = new View( this );
        layout.addView( separator, 1, new LayoutParams( LayoutParams.FILL_PARENT, 10 ) );
    }

}
