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

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
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
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

public class DtppActivity extends ActivityBase {

    private final String MIME_TYPE_PDF = "application/pdf";

    protected HashMap<String, View> mDtppMap = new HashMap<String, View>();
    protected ArrayList<String> mPendingCharts = new ArrayList<String>();
    protected String mTppCycle;
    protected BroadcastReceiver mReceiver;
    protected IntentFilter mFilter;
    protected String mFaaCode;
    protected String mTppVolume;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.dtpp_detail_view ) );

        mFilter = new IntentFilter();
        mFilter.addAction( DtppService.ACTION_CHECK_CHARTS );
        mFilter.addAction( DtppService.ACTION_GET_CHART );

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive( Context context, Intent intent ) {
                handleDtppBroadcast( intent );
            }
        };

        Button btnDownload = (Button) findViewById( R.id.btnDownload );
        btnDownload.setOnClickListener( new OnClickListener() {
            
            @Override
            public void onClick( View v ) {
                setRefreshItemVisible( true );
                startRefreshAnimation();
                getAptCharts();
            }
        } );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );
        DtppTask task = new DtppTask();
        task.execute( siteNumber );
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver( mReceiver, mFilter );
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver( mReceiver );
    }

    private final class DtppTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] result = new Cursor[ 11 ];

            Cursor apt = getAirportDetails( siteNumber );
            result[ 0 ] = apt;

            mFaaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );

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
                    new String[] { mFaaCode }, Dtpp.TPP_VOLUME, null, null, null );
            result[ 2 ] = c;

            c.moveToFirst();
            mTppVolume = c.getString( c.getColumnIndex( Dtpp.TPP_VOLUME ) );

            int index = 3;
            for ( String chartCode : new String[] { "APD", "MIN", "STAR", "IAP",
                    "DP", "DPO", "LAH", "HOT" } ) {
                builder = new SQLiteQueryBuilder();
                builder.setTables( Dtpp.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        Dtpp.FAA_CODE+"=? AND "+Dtpp.CHART_CODE+"=?",
                        new String[] { mFaaCode, chartCode }, null, null, null, null );
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
        mTppCycle = cycle.getString( cycle.getColumnIndex( Cycle.TPP_CYCLE ) );
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
            tv.setText( String.format( "Chart Cycle %s", mTppCycle ) );
        } else {
            tv.setText( String.format( "Chart Cycle %s (Expired)", mTppCycle ) );
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
        showOther( topLayout );

        // Check the chart availability
        for ( String pdfName : mDtppMap.keySet() ) {
            Log.d( "Checking", pdfName );
            mPendingCharts.add( pdfName );
        }
        checkTppCharts( mPendingCharts, false );
    }

    protected void showCharts( LinearLayout layout, Cursor c ) {
        if ( c.moveToFirst() ) {
            String chartCode = c.getString( c.getColumnIndex( Dtpp.CHART_CODE ) );
            LinearLayout item = (LinearLayout) inflate( R.layout.grouped_detail_item );
            TextView tv = (TextView) item.findViewById( R.id.group_name );
            LinearLayout grpLayout = (LinearLayout) item.findViewById( R.id.group_details );
            tv.setText( DataUtils.decodeChartCode( chartCode ) );
            do {
                String chartName = c.getString( c.getColumnIndex( Dtpp.CHART_NAME ) );
                String pdfName = c.getString( c.getColumnIndex( Dtpp.PDF_NAME ) );
                String userAction = c.getString( c.getColumnIndex( Dtpp.USER_ACTION ) );
                int resid = UiUtils.getRowSelectorForCursor( c );
                addChartRow( grpLayout, chartName, pdfName, userAction, resid );
            } while ( c.moveToNext() );
            layout.addView( item, new LinearLayout.LayoutParams( LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT ) );
        }
    }

    protected void showOther( LinearLayout layout ) {
        LinearLayout item = (LinearLayout) inflate( R.layout.grouped_detail_item );
        TextView tv = (TextView) item.findViewById( R.id.group_name );
        LinearLayout grpLayout = (LinearLayout) item.findViewById( R.id.group_details );
        tv.setText( "Other" );
        addChartRow( grpLayout, "Airport Diagram Legend", "legendAD.pdf", "",
                R.drawable.row_selector_top );
        addSeparator( grpLayout );
        addChartRow( grpLayout, "Legends & General Information", "frntmatter.pdf", "",
                R.drawable.row_selector_bottom );
        layout.addView( item, new LinearLayout.LayoutParams( LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT ) );
    }

    protected View addChartRow( LinearLayout layout, String chartName, final String pdfName,
            String userAction, int resid ) {
        if ( layout.getChildCount() > 0 ) {
            addSeparator( layout );
        }
        View row = addRow( layout, chartName, DataUtils.decodeUserAction( userAction ) );

        if ( !userAction.equals( "D" ) ) {
            row.setBackgroundResource( resid );
            row.setOnClickListener( new OnClickListener() {

                @Override
                public void onClick( View v ) {
                    String path = (String) v.getTag();
                    if ( path == null ) {
                        setRefreshItemVisible( true );
                        startRefreshAnimation();
                        getTppChart( pdfName );
                    } else {
                        startPDFIntent( path );
                    }
                }

            } );
            showChartAvailability( row, false );
            mDtppMap.put( pdfName, row );
        }

        return row;
    }

    protected void checkTppCharts( ArrayList<String> pdfNames, boolean download ) {
        Intent service = new Intent( this, DtppService.class );
        service.setAction( DtppService.ACTION_CHECK_CHARTS );
        service.putExtra( DtppService.TPP_CYCLE, mTppCycle );
        service.putExtra( DtppService.PDF_NAMES, pdfNames );
        service.putExtra( DtppService.DOWNLOAD_IF_MISSING, download );
        startService( service );
    }

    protected void getTppChart( String pdfName ) {
        mPendingCharts.add( pdfName );
        Intent service = new Intent( this, DtppService.class );
        service.setAction( DtppService.ACTION_GET_CHART );
        service.putExtra( DtppService.TPP_CYCLE, mTppCycle );
        service.putExtra( DtppService.PDF_NAME, pdfName );
        startService( service );
    }

    protected void getAptCharts() {
        for ( String pdfName : mDtppMap.keySet() ) {
            View v = mDtppMap.get( pdfName );
            if ( v.getTag() == null ) {
                Log.d( "Requesting", pdfName );
                mPendingCharts.add( pdfName );
            }
        }
        checkTppCharts( mPendingCharts, true );
    }

    protected void handleDtppBroadcast( Intent intent ) {
        String action = intent.getAction();
        String pdfName = intent.getStringExtra( DtppService.PDF_NAME );
        boolean result = intent.getBooleanExtra( DtppService.RESULT, false );

        Log.d( "Response", pdfName );
        if ( result ) {
            View view = mDtppMap.get( pdfName );
            showChartAvailability( view, true );
            // Save the PDF chart path for later use
            String path = intent.getStringExtra( DtppService.PDF_PATH );
            view.setTag( path );

            if ( action.equals( DtppService.ACTION_GET_CHART ) ) {
                startPDFIntent( path );
            }
        }

        mPendingCharts.remove( pdfName );
        if ( mPendingCharts.isEmpty() ) {
            Log.d( "Response", "ALL DONE" );
            // There are no pending requests, hide the spinner
            stopRefreshAnimation();
            setRefreshItemVisible( false );

            // Check if we have all the charts for this airport
            boolean all = true;
            for ( String key : mDtppMap.keySet() ) {
                // Check if PDF path is set for this chart
                if ( mDtppMap.get( key ).getTag() == null ) {
                    all = false;
                    break;
                }
            }
            if ( all ) {
                // All charts are available, hide the download button
                Button btnDownload = (Button) findViewById( R.id.btnDownload );
                btnDownload.setVisibility( View.GONE );
            }
        }
    }

    protected void startPDFIntent( String path ) {
        if ( SystemUtils.canDisplayMimeType( this, MIME_TYPE_PDF ) ) {
            Intent viewChart = new Intent( Intent.ACTION_VIEW );
            Uri pdf = Uri.fromFile( new File( path ) );
            viewChart.setDataAndType( pdf, MIME_TYPE_PDF );
            startActivity( viewChart );
        } else {
            UiUtils.showToast( this, "FlightIntel: No PDF viewer app was found." );
            Intent market = new Intent( Intent.ACTION_VIEW );
            Uri uri = Uri.parse( "market://details?id=org.ebookdroid" );
            market.setData( uri );
            startActivity( market );
        }
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
