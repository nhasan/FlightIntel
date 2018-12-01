/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2018 Nadeem Hasan <nhasan@nadmm.com>
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

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.Attendance;
import com.nadmm.airports.data.DatabaseManager.Remarks;
import com.nadmm.airports.utils.CursorAsyncTask;

public final class AttendanceFragment extends FragmentBase {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.attendance_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setActionBarTitle( "Attendance", "" );

        String siteNumber = getArguments().getString( Airports.SITE_NUMBER );
        setBackgroundTask( new AttendanceTask( this ) ).execute( siteNumber );
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        showAirportTitle( apt );
        showAttendanceDetails( result );
        showAttendanceRemarks( result );

        setFragmentContentShown( true );
    }

    private void showAttendanceDetails( Cursor[] result ) {
        Cursor att = result[ 1 ];
        if ( att.moveToFirst() ) {
            LinearLayout layout = findViewById( R.id.attendance_content_layout );
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

    private void showAttendanceRemarks( Cursor[] result ) {
        Cursor rmk = result[ 2 ];
        LinearLayout layout = findViewById( R.id.attendance_remark_layout );
        if ( rmk.moveToFirst() ) {
            layout.setVisibility( View.VISIBLE );
            do {
                String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                addBulletedRow( layout, remark );
            } while ( rmk.moveToNext() );
        }
    }

    private Cursor[] doQuery( String siteNumber ) {
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

    private static class AttendanceTask extends CursorAsyncTask<AttendanceFragment> {

        private AttendanceTask( AttendanceFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( AttendanceFragment fragment, String... params ) {
            String siteNumber = params[ 0 ];
            return fragment.doQuery( siteNumber );
        }

        @Override
        protected boolean onResult( AttendanceFragment fragment, Cursor[] result ) {
            fragment.showDetails( result );
            return true;
        }

    }

}
