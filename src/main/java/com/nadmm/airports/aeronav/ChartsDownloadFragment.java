/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2018 Nadeem Hasan <nhasan@nadmm.com>
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

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nadmm.airports.Application;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Dtpp;
import com.nadmm.airports.data.DatabaseManager.DtppCycle;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class ChartsDownloadFragment extends FragmentBase {

    private String mTppCycle;
    private String mTppVolume;
    private Cursor mCursor;
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    private View mSelectedRow;
    private ProgressBar mProgressBar;
    private OnClickListener mOnClickListener;
    private boolean mExpired;
    private boolean mIsOk;
    private boolean mStop;
    private HashMap<String, View> mVolumeRowMap = new HashMap<>();

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mFilter = new IntentFilter();
        mFilter.addAction( AeroNavService.ACTION_GET_CHARTS );
        mFilter.addAction( AeroNavService.ACTION_CHECK_CHARTS );
        mFilter.addAction( AeroNavService.ACTION_COUNT_CHARTS );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                String action = intent.getAction();
                if ( action != null ) {
                    switch ( action ) {
                        case AeroNavService.ACTION_GET_CHARTS:
                            onChartDownload();
                            break;
                        case AeroNavService.ACTION_CHECK_CHARTS:
                            onChartDelete();
                            break;
                        case AeroNavService.ACTION_COUNT_CHARTS:
                            onChartCount( intent );
                            break;
                    }
                }
            }
        };

        mOnClickListener = v -> {
            if ( mSelectedRow == null ) {
                mStop = false;
                int total = (Integer) v.getTag( R.id.DTPP_CHART_TOTAL );
                int avail = (Integer) v.getTag( R.id.DTPP_CHART_AVAIL );
                if ( avail < total ) {
                    if ( mIsOk && !mExpired ) {
                        confirmStartDownload( v );
                    } else {
                        UiUtils.showToast( getActivity(), "Cannot start download" );
                    }
                } else {
                    confirmChartDelete( v );
                }
            } else if ( v == mSelectedRow ) {
                confirmStopDownload();
            }
        };
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        View v = inflate( R.layout.charts_download_view );
        return createContentView( v );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        ChartsDownloadTask task = new ChartsDownloadTask( this );
        setBackgroundTask( task );
        task.execute();
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
        finishOperation();

        super.onPause();
    }

    private void confirmStartDownload( final View v ) {
        int total = (Integer) v.getTag( R.id.DTPP_CHART_TOTAL );
        int avail = (Integer) v.getTag( R.id.DTPP_CHART_AVAIL );
        String tppVolume = (String) v.getTag( R.id.DTPP_VOLUME_NAME );
        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        builder.setTitle( "Start Download" );
        builder.setMessage( String.format( Locale.US,
                "Do you want to download %d charts for %s volume?",
                total-avail, tppVolume ) );
        builder.setPositiveButton( "Yes", ( dialog, which ) -> startChartDownload( v ) );
        builder.setNegativeButton( "No", null );
        builder.show();
    }

    private void startChartDownload( View v ) {
        mSelectedRow = v;
        String tppVolume = (String) mSelectedRow.getTag( R.id.DTPP_VOLUME_NAME );
        VolumeDownloadTask task = new VolumeDownloadTask( this );
        task.execute( tppVolume );
    }

    private void confirmChartDelete( final View v ) {
        int avail = (Integer) v.getTag( R.id.DTPP_CHART_AVAIL );
        String tppVolume = (String) v.getTag( R.id.DTPP_VOLUME_NAME );
        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        builder.setTitle( "Confirm Delete" );
        builder.setMessage( String.format( Locale.US,
                "Are you sure you want to delete all %d charts for %s volume?",
                avail, tppVolume ) );
        builder.setPositiveButton( android.R.string.yes,
                ( dialog, which ) -> startChartDelete( v ) );
        builder.setNegativeButton( android.R.string.no, null );
        builder.show();
    }

    private void startChartDelete( View v ) {
        mSelectedRow = v;
        String tppVolume = (String) mSelectedRow.getTag( R.id.DTPP_VOLUME_NAME );
        VolumeDeleteTask task = new VolumeDeleteTask( this );
        task.execute( tppVolume );
    }

    private void confirmStopDownload() {
        AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        builder.setTitle( "Stop Download" );
        builder.setMessage( "Do you want to stop the chart download?" );
        builder.setPositiveButton( "Yes", ( dialog, which ) -> mStop = true );
        builder.setNegativeButton( "No", null );
        builder.show();
    }

    private Cursor[] doQueryTotals() {
        Cursor[] result = new Cursor[ 2 ];
        SQLiteDatabase db = getDatabase( DatabaseManager.DB_DTPP );

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( DtppCycle.TABLE_NAME );
        Cursor c = builder.query( db, new String[] { "*" },
                null, null, null, null, null, null );
        result[ 0 ] = c;

        builder = new SQLiteQueryBuilder();
        builder.setTables( Dtpp.TABLE_NAME );
        c = builder.query( db, new String[] { Dtpp.TPP_VOLUME,
                        "count(DISTINCT "+Dtpp.PDF_NAME+") AS total" },
                Dtpp.USER_ACTION+"!='D'",
                null, Dtpp.TPP_VOLUME, null, null, null );
        result[ 1 ] = c;

        return result;
    }

    private static class ChartsDownloadTask extends CursorAsyncTask<ChartsDownloadFragment> {

        private ChartsDownloadTask( ChartsDownloadFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( ChartsDownloadFragment fragment, String... params ) {
            return fragment.doQueryTotals();
        }

        @Override
        protected boolean onResult( ChartsDownloadFragment fragment, Cursor[] result ) {
            // Add delay to allow navigation drawer to close without stutter
            new Handler().postDelayed( () -> fragment.showChartInfo( result ), 250 );
            // Do not close cursors now
            return false;
        }
    }

    @SuppressLint( "SetTextI18n" )
    private void showChartInfo( Cursor[] result ) {
        Cursor c = result[ 0 ];
        if ( c.moveToFirst() ) {
            mTppCycle = c.getString( c.getColumnIndex( DtppCycle.TPP_CYCLE ) );
        } else {
            UiUtils.showToast( getActivity(), "d-TPP database is not installed" );
            getActivity().finish();
            return;
        }

        getSupportActionBar().setSubtitle( String.format( "AeroNav Cycle %s", mTppCycle ) );

        String expiry = c.getString( c.getColumnIndex( DtppCycle.TO_DATE ) );
        SimpleDateFormat df = new SimpleDateFormat( "HHmm'Z' MM/dd/yy", Locale.US );
        df.setTimeZone( java.util.TimeZone.getTimeZone( "UTC" ) );
        Date endDate;
        try {
            endDate = df.parse( expiry );
        } catch ( ParseException ignored ) {
            endDate = new Date();
        }

        // Determine if chart cycle has expired
        Date now = new Date();
        if ( now.getTime() > endDate.getTime() ) {
            mExpired = true;
        }

        TextView tv = findViewById( R.id.charts_cycle_expiry );
        if ( mExpired ) {
            tv.setText( String.format( Locale.US, "This chart cycle has expired on %s",
                    TimeUtils.formatDateTime( getActivityBase(), endDate.getTime() ) ) );
        } else {
            tv.setText( String.format( Locale.US, "This chart cycle expires on %s",
                    TimeUtils.formatDateTime( getActivityBase(), endDate.getTime() ) ) );
        }

        tv = findViewById( R.id.charts_download_msg );
        tv.setText( "Each TPP volume is about 150-250MB in size and may take 15-30 mins"
                +" to download. The Instrument Procedure charts are in PDF format and"
                +" stored on the external SD card storage. These are not the sectional charts."
                +" Press 'Back' button to stop a running download.\n\n"
                +"All charts for a cycle are automatically deleted at the end of that cycle" );

        String msg;
        if ( !Application.sDonationDone ) {
            msg = "This function is only available after a donation";
            mIsOk = false;
        } else if ( mExpired ) {
            msg = "Chart cycle has expired";
            mIsOk = false;
        } else if ( !NetworkUtils.isNetworkAvailable( getActivity() ) ) {
            msg = "Not connected to the internet";
            mIsOk = false;
        } else if ( NetworkUtils.canDownloadData( getActivityBase() ) ) {
            msg = "Connected to an unmetered network";
            mIsOk = true;
        } else {
            msg = "Connected to a metered network";
            mIsOk = false;
        }

        tv = findViewById( R.id.charts_download_warning );
        tv.setText( msg );
        Drawable d = UiUtils.getDefaultTintedDrawable( getActivity(),
                mIsOk? R.drawable.ic_check : R.drawable.ic_highlight_remove );
        UiUtils.setTextViewDrawable( tv, d );

        LinearLayout layout = findViewById( R.id.vol_chart_details );
        c = result[ 1 ];
        if ( c.moveToFirst() ) {
            do {
                String tppVolume = c.getString( c.getColumnIndex( Dtpp.TPP_VOLUME ) );
                int total = c.getInt( c.getColumnIndex( "total" ) );
                addTppVolumeRow( layout, tppVolume, total );
            } while ( c.moveToNext() );
        }

        // Close all cursors here
        for ( Cursor cursor : result ) {
            cursor.close();
        }

        setFragmentContentShown( true );
    }

    private Cursor[] doQueryVolume( String volume ) {
        mTppVolume = volume;
        SQLiteDatabase db = getDatabase( DatabaseManager.DB_DTPP );

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Dtpp.TABLE_NAME );
        Cursor c = builder.query( db,
                new String[] { Dtpp.PDF_NAME },
                Dtpp.TPP_VOLUME+"=? AND "+Dtpp.USER_ACTION+"!=?",
                new String[] { mTppVolume, "D" },
                Dtpp.PDF_NAME+","+Dtpp.TPP_VOLUME,
                null, null );

        return new Cursor[] { c };
    }

    private static class VolumeDownloadTask extends CursorAsyncTask<ChartsDownloadFragment> {

        private VolumeDownloadTask( ChartsDownloadFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( ChartsDownloadFragment fragment, String... params ) {
            String volume = params[ 0 ];
            return fragment.doQueryVolume( volume );
        }

        @Override
        protected boolean onResult( ChartsDownloadFragment fragment, Cursor[] result ) {
            fragment.downloadCharts( result[ 0 ] );
            return false;
        }
    }

    private void onChartDownload() {
        if ( mCursor != null ) {
            mProgressBar.setProgress( mCursor.getPosition() );
            if ( !mStop && mCursor.moveToNext() ) {
                getNextChart();
            } else {
                getChartCount( mTppCycle, mTppVolume );
                finishOperation();
            }
        }
    }

    private void downloadCharts( Cursor c ) {
        mCursor = c;
        mCursor.moveToFirst();
        mProgressBar = mSelectedRow.findViewById( R.id.progress );
        mProgressBar.setMax( mCursor.getCount() );
        mProgressBar.setProgress( 0 );
        mProgressBar.setVisibility( View.VISIBLE );
        getNextChart();
    }

    private void getNextChart() {
        String pdfName = mCursor.getString( mCursor.getColumnIndex( Dtpp.PDF_NAME ) );
        ArrayList<String> pdfNames = new ArrayList<>();
        pdfNames.add( pdfName );
        Intent service = new Intent( getActivity(), DtppService.class );
        service.setAction( AeroNavService.ACTION_GET_CHARTS );
        service.putExtra( AeroNavService.CYCLE_NAME, mTppCycle );
        service.putExtra( AeroNavService.TPP_VOLUME, mTppVolume );
        service.putExtra( AeroNavService.PDF_NAMES, pdfNames );
        getActivity().startService( service );
    }

    private Cursor[] doQueryDelete( String volume ) {
        mTppVolume = volume;
        SQLiteDatabase db = getDatabase( DatabaseManager.DB_DTPP );

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Dtpp.TABLE_NAME );
        Cursor c = builder.query( db,
                new String[] { Dtpp.PDF_NAME },
                Dtpp.TPP_VOLUME+"=? AND "+Dtpp.USER_ACTION+"!=?",
                new String[] { mTppVolume, "D" },
                Dtpp.PDF_NAME+","+Dtpp.TPP_VOLUME,
                null, null );

        return new Cursor[] { c };
    }

    private static class VolumeDeleteTask extends CursorAsyncTask<ChartsDownloadFragment> {

        private VolumeDeleteTask( ChartsDownloadFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( ChartsDownloadFragment fragment, String... params ) {
            String volume = params[ 0 ];
            return fragment.doQueryDelete( volume );
        }

        @Override
        protected boolean onResult( ChartsDownloadFragment fragment, Cursor[] result ) {
            fragment.deleteCharts( result[ 0 ] );
            return false;
        }
    }

    private void onChartDelete() {
        if ( mCursor != null ) {
            mProgressBar.setProgress( mCursor.getPosition() );
            if ( !mStop && mCursor.moveToNext() ) {
                deleteNextChart();
            } else {
                getChartCount( mTppCycle, mTppVolume );
                finishOperation();
            }
        }
    }

    private void deleteCharts( Cursor c ) {
        mCursor = c;
        mCursor.moveToFirst();
        mProgressBar = mSelectedRow.findViewById( R.id.progress );
        mProgressBar.setMax( mCursor.getCount() );
        mProgressBar.setProgress( 0 );
        mProgressBar.setVisibility( View.VISIBLE );
        deleteNextChart();
    }

    private void deleteNextChart() {
        String pdfName = mCursor.getString( mCursor.getColumnIndex( Dtpp.PDF_NAME ) );
        ArrayList<String> pdfNames = new ArrayList<>();
        pdfNames.add( pdfName );
        Intent service = new Intent( getActivity(), DtppService.class );
        service.setAction( AeroNavService.ACTION_DELETE_CHARTS );
        service.putExtra( AeroNavService.CYCLE_NAME, mTppCycle );
        service.putExtra( AeroNavService.TPP_VOLUME, mTppVolume );
        service.putExtra( AeroNavService.PDF_NAMES, pdfNames );
        getActivity().startService( service );
    }

    private void finishOperation() {
        mTppVolume = null;
        mSelectedRow = null;
        if ( mProgressBar != null ) {
            mProgressBar.setVisibility( View.GONE );
            mProgressBar = null;
        }
        if ( mCursor != null ) {
            mCursor.close();
            mCursor = null;
        }
    }

    private void getChartCount( String tppCycle, String tppVolume ) {
        Intent service = new Intent( getActivity(), DtppService.class );
        service.setAction( AeroNavService.ACTION_COUNT_CHARTS );
        service.putExtra( AeroNavService.CYCLE_NAME, tppCycle );
        service.putExtra( AeroNavService.TPP_VOLUME, tppVolume );
        getActivity().startService( service );
    }

    private void addTppVolumeRow( LinearLayout layout, String tppVolume, int total ) {
        if ( layout.getChildCount() > 0 ) {
            addSeparator( layout );
        }

        RelativeLayout row = (RelativeLayout) inflate( R.layout.list_item_with_progressbar );

        TextView tv = row.findViewById( R.id.item_label );
        tv.setText( tppVolume );
        tv = row.findViewById( R.id.item_value );
        tv.setText( String.format( Locale.US, "%d charts", total ) );

        row.setTag( R.id.DTPP_VOLUME_NAME, tppVolume );
        row.setTag( R.id.DTPP_CHART_TOTAL, total );

        showStatus( row, 0, total );

        layout.addView( row, new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );

        mVolumeRowMap.put( tppVolume, row );
        getChartCount( mTppCycle, tppVolume );
    }

    private void onChartCount( Intent intent ) {
        String tppVolume = intent.getStringExtra( AeroNavService.TPP_VOLUME );
        int avail = intent.getIntExtra( AeroNavService.PDF_COUNT, 0 );
        View row = mVolumeRowMap.get( tppVolume );
        if ( row != null ) {
            row.setOnClickListener( mOnClickListener );
            int background = UiUtils.getSelectableItemBackgroundResource( getActivity() );
            row.setBackgroundResource( background );
            row.setTag( R.id.DTPP_CHART_AVAIL, avail );
            int total = (Integer) row.getTag( R.id.DTPP_CHART_TOTAL );
            showStatus( row, avail, total );
        }
    }

    private void showStatus( View row, int avail, int total ) {
        TextView tv = row.findViewById( R.id.item_label );
        if ( avail == total ) {
            UiUtils.setTextViewDrawable( tv, R.drawable.ic_check_box );
        } else {
            UiUtils.setTextViewDrawable( tv, R.drawable.ic_check_box_outline_blank );
        }
    }

}
