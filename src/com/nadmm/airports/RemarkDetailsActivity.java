/*
 * Airports for Android
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TableLayout.LayoutParams;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.DatabaseManager.Tower8;

public class RemarkDetailsActivity extends ActivityBase {

    private LinearLayout mMainLayout;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mInflater = getLayoutInflater();
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        AirportRemarksTask task = new AirportRemarksTask();
        task.execute( siteNumber );
    }

    private final class AirportRemarksTask extends AsyncTask<String, Void, Cursor[]> {

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
                    +"'A24', 'A70', 'A81', 'A81 1', 'A82')",
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
        protected void onPostExecute( Cursor[] result ) {
            Cursor rmk = result[ 1 ];
            if ( rmk.getCount() == 0 ) {
                Toast.makeText( RemarkDetailsActivity.this, "No remarks were found",
                        Toast.LENGTH_LONG ).show();
                RemarkDetailsActivity.this.finish();
            }

            View view = mInflater.inflate( R.layout.remarks_detail_view, null );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.remarks_top_layout );

            // Title
            Cursor apt = result[ 0 ];
            showAirportTitle( mMainLayout, apt );

            showRemarksDetails( result );
            showAirspaceDetails( result );
        }

    }

    protected void showRemarksDetails( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById(
                R.id.detail_remarks_layout );
        Cursor rmk = result[ 1 ];
        if ( rmk.moveToFirst() ) {
            do {
                String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                addRemarkRow( layout, remark );
            } while ( rmk.moveToNext() );
        }
    }

    protected void showAirspaceDetails( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) mMainLayout.findViewById(
                R.id.detail_remarks_layout );

        Cursor twr8 = result[ 2 ];
        if ( twr8.moveToFirst() ) {
            do {
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
                addRow( layout, remark );
            } while ( twr8.moveToNext() );
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

        addRow( layout, remark );
    }

    protected void addRow( LinearLayout layout, String remark ) {
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 10, 2, 2, 2 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 12, 2 );
        tv.setText( remark );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

}
