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

package com.nadmm.airports.aeronav;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Dtpp;
import com.nadmm.airports.DatabaseManager.DtppCycle;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

public class DtppActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.airport_activity_layout ) );

        Bundle args = getIntent().getExtras();
        addFragment( DtppFragment.class, args );
    }

    @Override
    public void onStart() {
      super.onStart();
      EasyTracker.getInstance( this ).activityStart( this );
    }

    @Override
    public void onStop() {
      super.onStop();
      EasyTracker.getInstance( this ).activityStop( this );
    }

    public static class DtppFragment extends FragmentBase {

        protected HashMap<String, View> mDtppRowMap = new HashMap<String, View>();
        protected ArrayList<String> mPendingCharts = new ArrayList<String>();
        protected String mTppCycle;
        protected String mFaaCode;
        protected String mTppVolume;
        protected boolean mExpired = false;
        protected OnClickListener mOnClickListener;
        protected IntentFilter mFilter;
        protected BroadcastReceiver mReceiver;

        private final class DtppTask extends CursorAsyncTask {

            @Override
            protected Cursor[] doInBackground( String... params ) {
                String siteNumber = params[ 0 ];
                int index = 0;

                SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
                Cursor[] result = new Cursor[ 11 ];

                Cursor apt = getAirportDetails( siteNumber );
                result[ index++ ] = apt;

                mFaaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );

                db = getDatabase( DatabaseManager.DB_DTPP );

                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables( DtppCycle.TABLE_NAME );
                Cursor c = builder.query( db, new String[] { "*" },
                        null, null, null, null, null, null );
                result[ index++ ] = c;

                builder = new SQLiteQueryBuilder();
                builder.setTables( Dtpp.TABLE_NAME );
                c = builder.query( db, new String[] { Dtpp.TPP_VOLUME },
                        Dtpp.FAA_CODE+"=?",
                        new String[] { mFaaCode }, Dtpp.TPP_VOLUME, null, null, null );
                result[ index++ ] = c;

                c.moveToFirst();
                mTppVolume = c.getString( c.getColumnIndex( Dtpp.TPP_VOLUME ) );

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
            protected boolean onResult( Cursor[] result ) {
                Cursor apt = result[ 0 ];
                showAirportTitle( apt );
                showDtppSummary( result );
                showDtppCharts( result );
                setContentShown( true );
                return true;
            }

        }

        @Override
        public void onCreate( Bundle savedInstanceState ) {

            mFilter = new IntentFilter();
            mFilter.addAction( DtppService.ACTION_CHECK_CHARTS );
            mFilter.addAction( DtppService.ACTION_GET_CHARTS );

            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive( Context context, Intent intent ) {
                    handleDtppBroadcast( intent );
                }
            };

            mOnClickListener = new OnClickListener() {
                @Override
                public void onClick( View v ) {
                    String path = (String) v.getTag( R.id.DTPP_PDF_PATH );
                    if ( path == null ) {
                        String pdfName = (String) v.getTag( R.id.DTPP_PDF_NAME );
                        getTppChart( pdfName );
                    } else {
                        startPdfViewer( path );
                    }
                }
            };

            super.onCreate( savedInstanceState );
        }

        @Override
        public void onResume() {
            LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
            bm.registerReceiver( mReceiver, mFilter );

            super.onResume();
        }

        @Override
        public void onPause() {
            LocalBroadcastManager bm = LocalBroadcastManager.getInstance( getActivity() );
            bm.unregisterReceiver( mReceiver );

            super.onPause();
        }

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            View view = inflater.inflate( R.layout.dtpp_detail_view, container, false );

            Button btnDownload = (Button) view.findViewById( R.id.btnDownload );
            btnDownload.setOnClickListener( new OnClickListener() {

                @Override
                public void onClick( View v ) {
                    getAptCharts();
                }
            } );

            Button btnDelete = (Button) view.findViewById( R.id.btnDelete );
            btnDelete.setOnClickListener( new OnClickListener() {

                @Override
                public void onClick( View v ) {
                    checkDelete();
                }
            } );

            return view;
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            Bundle args = getArguments();
            String siteNumber = args.getString( Airports.SITE_NUMBER );
            setBackgroundTask( new DtppTask() ).execute( siteNumber );

            super.onActivityCreated( savedInstanceState );
        }

        protected void showDtppSummary( Cursor[] result ) {
            LinearLayout topLayout = (LinearLayout) findViewById( R.id.dtpp_detail_layout );

            Cursor cycle = result[ 1 ];
            cycle.moveToFirst();
            mTppCycle = cycle.getString( cycle.getColumnIndex( DtppCycle.TPP_CYCLE ) );
            String from = cycle.getString( cycle.getColumnIndex( DtppCycle.FROM_DATE ) );
            String to = cycle.getString( cycle.getColumnIndex( DtppCycle.TO_DATE ) );

            // Parse chart cycle effective dates
            SimpleDateFormat df = new SimpleDateFormat( "HHmm'Z' MM/dd/yy", Locale.US );
            df.setTimeZone( java.util.TimeZone.getTimeZone( "UTC" ) );
            Date fromDate = null;
            Date toDate = null;
            try {
                fromDate = df.parse( from );
                toDate = df.parse( to );
            } catch ( ParseException e1 ) {
            }

            // Determine if chart cycle has expired
            Date now = new Date();
            if ( now.getTime() > toDate.getTime() ) {
                mExpired = true;
            }

            RelativeLayout item = (RelativeLayout) inflate( R.layout.grouped_detail_item );
            TextView tv = (TextView) item.findViewById( R.id.group_name );
            LinearLayout layout = (LinearLayout) item.findViewById( R.id.group_details );
            tv.setText( String.format( "Chart Cycle %s", mTppCycle ) );

            Cursor dtpp = result[ 2 ];
            dtpp.moveToFirst();
            String tppVolume = dtpp.getString( 0 );
            addRow( layout, "Volume", tppVolume );
            addRow( layout, "Valid", TimeUtils.formatDateRange( getActivity(),
                    fromDate.getTime(), toDate.getTime() ) );
            if ( mExpired ) {
                addRow( layout, "WARNING: This chart cycle has expired." );
            }

            topLayout.addView( item, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        }

        protected void showDtppCharts( Cursor[] result ) {
            LinearLayout topLayout = (LinearLayout) findViewById( R.id.dtpp_detail_layout );

            int index = 3;
            while ( index < result.length ) {
                showChartGroup( topLayout, result[ index ] );
                ++index;
            }
            showOtherCharts( topLayout );

            // Check the chart availability
            ArrayList<String> pdfNames = new ArrayList<String>();
            for ( String pdfName : mDtppRowMap.keySet() ) {
                pdfNames.add( pdfName );
            }
            checkTppCharts( pdfNames, false );
        }

        protected void showChartGroup( LinearLayout layout, Cursor c ) {
            if ( c.moveToFirst() ) {
                String chartCode = c.getString( c.getColumnIndex( Dtpp.CHART_CODE ) );
                RelativeLayout item = (RelativeLayout) inflate( R.layout.grouped_detail_item );
                TextView tv = (TextView) item.findViewById( R.id.group_name );
                tv.setText( DataUtils.decodeChartCode( chartCode ) );
                LinearLayout group = (LinearLayout) item.findViewById( R.id.group_details );
                do {
                    String chartName = c.getString( c.getColumnIndex( Dtpp.CHART_NAME ) );
                    String pdfName = c.getString( c.getColumnIndex( Dtpp.PDF_NAME ) );
                    String userAction = c.getString( c.getColumnIndex( Dtpp.USER_ACTION ) );
                    String faanfd18 = c.getString( c.getColumnIndex( Dtpp.FAANFD18_CODE ) );
                    addChartRow( group, chartCode, chartName, pdfName, userAction, faanfd18 );
                } while ( c.moveToNext() );
                setRowBackgroundResource( group );
                layout.addView( item, new LinearLayout.LayoutParams( LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT ) );
            }
        }

        protected void showOtherCharts( LinearLayout layout ) {
            RelativeLayout item = (RelativeLayout) inflate( R.layout.grouped_detail_item );
            TextView tv = (TextView) item.findViewById( R.id.group_name );
            LinearLayout group = (LinearLayout) item.findViewById( R.id.group_details );
            tv.setText( "Other" );
            addChartRow( group, "", "Airport Diagram Legend", "legendAD.pdf", "", "" );
            addChartRow( group, "", "Legends & General Information", "frntmatter.pdf", "", "" );
            setRowBackgroundResource( group );
            layout.addView( item, new LinearLayout.LayoutParams( LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT ) );
        }

        protected View addChartRow( LinearLayout layout, String chartCode, String chartName,
                String pdfName, String userAction, String faanfd18 ) {
            View row;
            if ( userAction.length() > 0 ) {
                row= addRow( layout, chartName, DataUtils.decodeUserAction( userAction ) );
            } else {
                row= addRow( layout, chartName, faanfd18 );
            }

            if ( !userAction.equals( "D" ) ) {
                row.setOnClickListener( mOnClickListener );
                row.setTag( R.id.DTPP_CHART_CODE, chartCode );
                row.setTag( R.id.DTPP_PDF_NAME, pdfName );
                showChartAvailability( row, false );
                mDtppRowMap.put( pdfName, row );
            }

            row.setTag( R.id.DTPP_USER_ACTION, userAction );
            return row;
        }

        protected void checkTppCharts( ArrayList<String> pdfNames, boolean download ) {
            setRefreshItemVisible( true );
            startRefreshAnimation();
            mPendingCharts = pdfNames;
            Intent service = makeServiceIntent( DtppService.ACTION_CHECK_CHARTS );
            service.putExtra( DtppService.DOWNLOAD_IF_MISSING, download );
            getActivity().startService( service );
        }

        protected void getTppChart( String pdfName ) {
            setRefreshItemVisible( true );
            startRefreshAnimation();
            mPendingCharts.add( pdfName );
            Intent service = makeServiceIntent( DtppService.ACTION_GET_CHARTS );
            getActivity().startService( service );
        }

        protected void deleteCharts() {
            for ( String pdfName : mDtppRowMap.keySet() ) {
                View v = mDtppRowMap.get( pdfName );
                String userAction = (String) v.getTag( R.id.DTPP_USER_ACTION );
                String chartCode = (String) v.getTag( R.id.DTPP_CHART_CODE );
                String path = (String) v.getTag( R.id.DTPP_PDF_PATH );
                if ( !userAction.equals( "D" ) && chartCode.length() > 0 && path != null ) {
                    mPendingCharts.add( pdfName );
                }
            }
            setRefreshItemVisible( true );
            startRefreshAnimation();
            Intent service = makeServiceIntent( DtppService.ACTION_DELETE_CHARTS );
            getActivity().startService( service );
        }

        protected Intent makeServiceIntent( String action ) {
            Intent service = new Intent( getActivity(), DtppService.class );
            service.setAction( action );
            service.putExtra( DtppService.CYCLE_NAME, mTppCycle );
            service.putExtra( DtppService.TPP_VOLUME, mTppVolume );
            service.putExtra( DtppService.PDF_NAMES, mPendingCharts );
            return service;
        }

        protected void getMissingCharts() {
            ArrayList<String> pdfNames = new ArrayList<String>();
            for ( String pdfName : mDtppRowMap.keySet() ) {
                View v = mDtppRowMap.get( pdfName );
                String userAction = (String) v.getTag( R.id.DTPP_USER_ACTION );
                String path = (String) v.getTag( R.id.DTPP_PDF_PATH );
                // Skip deleted and downloaded charts
                if ( !userAction.equals( "D" ) && path == null ) {
                    // This PDF is not available on the device
                    pdfNames.add( pdfName );
                }
            }
            UiUtils.showToast( getActivity(), 
                    String.format( "Downloading %d charts in the background", pdfNames.size() ) );
            checkTppCharts( pdfNames, true );
        }

        protected void handleDtppBroadcast( Intent intent ) {
            String action = intent.getAction();
            String pdfName = intent.getStringExtra( DtppService.PDF_NAME );
            String path = intent.getStringExtra( DtppService.PDF_PATH );

            View view = mDtppRowMap.get( pdfName );
            if ( view == null ) {
                // We got a broadcast for a chart for some other airport
                return;
            }

            if ( path != null ) {
                showChartAvailability( view, true );
                view.setTag( R.id.DTPP_PDF_PATH, path );
                if ( action.equals( DtppService.ACTION_GET_CHARTS ) ) {
                    startPdfViewer( path );
                }
            } else {
                showChartAvailability( view, false );
                view.setTag( R.id.DTPP_PDF_PATH, null );
            }

            mPendingCharts.remove( pdfName );
            if ( mPendingCharts.isEmpty() ) {
                // There are no pending requests, hide the spinner
                stopRefreshAnimation();
                setRefreshItemVisible( false );

                updateButtonState();
            }
        }

        protected void showChartAvailability( View view, boolean available ) {
            TextView tv = (TextView) view.findViewById( R.id.item_label );
            if ( available ) {
                tv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.btn_check_on_holo_light, 0, 0, 0 );
            } else {
                tv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.btn_check_off_holo_light, 0, 0, 0 );
            }
        }

        protected void updateButtonState() {
            // Check if we have all the charts for this airport
            boolean all = true;
            boolean none = true;
            for ( String key : mDtppRowMap.keySet() ) {
                View v = mDtppRowMap.get( key );
                String path = (String) v.getTag( R.id.DTPP_PDF_PATH );
                String chartCode = (String) v.getTag( R.id.DTPP_CHART_CODE );
                if ( chartCode.length() > 0 ) {
                    if ( path == null ) {
                        all = false;
                    } else {
                        none = false;
                    }
                }
            }

            Button btnDownload = (Button) findViewById( R.id.btnDownload );
            if ( all ) {
                btnDownload.setVisibility( View.GONE );
            } else {
                btnDownload.setVisibility( View.VISIBLE );
            }

            Button btnDelete = (Button) findViewById( R.id.btnDelete );
            if ( none ) {
                btnDelete.setVisibility( View.GONE );
            } else {
                btnDelete.setVisibility( View.VISIBLE );
            }
        }

        protected void getAptCharts() {
            NetworkUtils.checkNetworkAndDownload( getActivity(), new Runnable() {

                @Override
                public void run() {
                    getMissingCharts();
                }
            } );
        }

        protected void startPdfViewer( String path ) {
            if ( mExpired ) {
                UiUtils.showToast( getActivity(), "WARNING: This chart has expired!" );
            }
            SystemUtils.startPDFViewer( getActivity(), path );
        }

        protected void checkDelete() {
            AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
            builder.setMessage( "Delete all downloaded charts for this airport?" )
                   .setPositiveButton( "Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int id ) {
                            deleteCharts();
                        }
                   } )
                   .setNegativeButton( "No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick( DialogInterface dialog, int id ) {
                        }
                   } );
            AlertDialog alert = builder.create();
            alert.show();
        }

    }

}
