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

package com.nadmm.airports.afd;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Attendance;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.SlidingMenuFragment;
import com.nadmm.airports.utils.CursorAsyncTask;

public class AttendanceActivity extends AfdActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Bundle args = getIntent().getExtras();
        addFragment( AttendanceFragment.class, args );
    }

    @Override
    protected void onResume() {
        setSlidingMenuActivatedItem( SlidingMenuFragment.ITEM_ID_AFD );

        super.onResume();
    }

    public static class AttendanceFragment extends FragmentBase {

        private final class AttendanceTask extends CursorAsyncTask {

            @Override
            protected Cursor[] doInBackground( String... params ) {
                String siteNumber = params[ 0 ];
                Cursor[] cursors = new Cursor[ 3 ];
                
                cursors[ 0 ] = getAirportDetails( siteNumber );

                SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
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
            protected boolean onResult( Cursor[] result ) {
                showDetails( result );
                return true;
            }

        }

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            View view = inflater.inflate( R.layout.attendance_detail_view, container, false );
            return view;
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            Bundle args = getArguments();
            String siteNumber = args.getString( Airports.SITE_NUMBER );
            setBackgroundTask( new AttendanceTask() ).execute( siteNumber );

            super.onActivityCreated( savedInstanceState );
        }

        protected void showDetails( Cursor[] result ) {
            Cursor apt = result[ 0 ];

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
                    LinearLayout item = (LinearLayout) inflate( R.layout.attendance_detail_item,
                            layout );
                    if ( parts.length == 3 ) {
                        addRow( item, "Months", parts[ 0 ] );
                        addRow( item, "Days", parts[ 1 ] );
                        addRow( item, "Hours", parts[ 2 ] );
                    } else {
                        addRow( item, "Attendance", schedule );
                    }
                    layout.addView( item );
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
                    addBulletedRow( layout, remark );
                } while ( rmk.moveToNext() );            
            }
        }
    }
}
