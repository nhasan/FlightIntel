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

package com.nadmm.airports.dtpp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Cycle;
import com.nadmm.airports.DatabaseManager.Dtpp;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.TimeUtils;

public class DtppActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.dtpp_detail_view ) );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        DtppTask task = new DtppTask();
        task.execute( siteNumber );
    }

    private final class DtppTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] result = new Cursor[ 11 ];

            Cursor apt = getAirportDetails( siteNumber );
            result[ 0 ] = apt;

            String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );

            db = mDbManager.getDatabase( DatabaseManager.DB_DTPP );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Cycle.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { "*" },
                    null, null, null, null, null, null );
            result[ 1 ] = c;

            builder = new SQLiteQueryBuilder();
            builder.setTables( Dtpp.TABLE_NAME );
            c = builder.query( db, new String[] { Dtpp.TPP_VOLUME },
                    Dtpp.FAA_CODE+"=?",
                    new String[] { faaCode }, Dtpp.TPP_VOLUME, null, null, null );
            result[ 2 ] = c;

            int index = 3;
            for ( String chartCode : new String[] { "APD", "MIN", "STAR", "IAP",
                    "DP", "DPO", "LAH", "HOT" } ) {
                builder = new SQLiteQueryBuilder();
                builder.setTables( Dtpp.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        Dtpp.FAA_CODE+"=? AND "+Dtpp.CHART_CODE+"=?",
                        new String[] { faaCode, chartCode }, null, null, null, null );
                result[ index++ ] = c;
            }

            return result;
        }

        @Override
        protected void onResult( Cursor[] result ) {
            Cursor apt = result[ 0 ];

            setActionBarTitle( apt );
            showAirportTitle( apt );
            showAPD( result );

            setContentShown( true );
        }

    }

    protected void showAPD( Cursor[] result ) {
        LinearLayout topLayout = (LinearLayout) findViewById( R.id.dtpp_detail_layout );

        Cursor cycle = result[ 1 ];
        cycle.moveToFirst();
        String tppCycle = cycle.getString( cycle.getColumnIndex( Cycle.TPP_CYCLE ) );
        String from = cycle.getString( cycle.getColumnIndex( Cycle.FROM_DATE ) );
        String to = cycle.getString( cycle.getColumnIndex( Cycle.TO_DATE ) );

        SimpleDateFormat df = new SimpleDateFormat( "HHmm'Z' MM/dd/yy" );
        df.setTimeZone( java.util.TimeZone.getTimeZone( "UTC" ) );
        Date fromDate = null;
        Date toDate = null;
        try {
            fromDate = df.parse( from );
            toDate = df.parse( to );
        } catch ( ParseException e1 ) {
        }

        boolean expired = false;
        Date now = new Date();
        if ( now.getTime() > toDate.getTime() ) {
            expired = true;
        }

        LinearLayout item = (LinearLayout) inflate( R.layout.grouped_detail_item );
        TextView tv = (TextView) item.findViewById( R.id.group_name );
        LinearLayout layout = (LinearLayout) item.findViewById( R.id.group_details );
        if ( !expired ) {
            tv.setText( String.format( "Chart Cycle %s", tppCycle ) );
        } else {
            tv.setText( String.format( "Chart Cycle %s (Expired)", tppCycle ) );
        }

        Cursor dtpp = result[ 2 ];
        if ( dtpp.moveToFirst() ) {
            String tppVolume = dtpp.getString( 0 );
            addRow( layout, "Volume", tppVolume );
            addSeparator( layout );
        }

        addRow( layout, "Valid", TimeUtils.formatDateRangeUTC( this,
                fromDate.getTime(), toDate.getTime() ) );

        topLayout.addView( item, new LinearLayout.LayoutParams( LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT ) );

        int index = 3;
        while ( index < result.length ) {
            showCharts( topLayout, result[ index ] );
            ++index;
        }
    }

    protected void showCharts( LinearLayout layout, Cursor c ) {
        if ( c.moveToFirst() )
        {
            String chartCode = c.getString( c.getColumnIndex( Dtpp.CHART_CODE ) );
            LinearLayout item = (LinearLayout) inflate( R.layout.grouped_detail_item );
            TextView tv = (TextView) item.findViewById( R.id.group_name );
            LinearLayout grpLayout = (LinearLayout) item.findViewById( R.id.group_details );
            tv.setText( DataUtils.decodeChartCode( chartCode ) );
            do {
                addChartRow( grpLayout, c, false );
            } while ( c.moveToNext() );
            layout.addView( item, new LinearLayout.LayoutParams( LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT ) );
        }
    }

    protected View addChartRow( LinearLayout layout, Cursor c, boolean available ) {
        String chartName = c.getString( c.getColumnIndex( Dtpp.CHART_NAME ) );
        String userAction = c.getString( c.getColumnIndex( Dtpp.USER_ACTION ) );
        if ( layout.getChildCount() > 0 ) {
            addSeparator( layout );
        }
        View view = addRow( layout, chartName, DataUtils.decodeUserAction( userAction ) );
        if ( !userAction.equals( "D" ) ) {
            showChartAvailability( view, available );
        }
        return view;
    }

    protected void showChartAvailability( View view, boolean available ) {
        TextView tv = (TextView) view.findViewById( R.id.item_label );
        if ( available ) {
            tv.setCompoundDrawablesWithIntrinsicBounds( R.drawable.btn_check_on_holo_light,
                    0, 0, 0 );
        } else {
            tv.setCompoundDrawablesWithIntrinsicBounds( R.drawable.btn_check_off_holo_light,
                    0, 0, 0 );
        }
    }

}
