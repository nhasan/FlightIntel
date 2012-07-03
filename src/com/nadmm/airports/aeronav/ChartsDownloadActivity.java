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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Dtpp;
import com.nadmm.airports.DatabaseManager.DtppCycle;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.afd.AirportsCursorHelper;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

public class ChartsDownloadActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_activity_layout );

        Bundle args = getIntent().getExtras();
        addFragment( ChartsDownloadFragment.class, args );
    }

    public static class ChartsDownloadFragment extends FragmentBase {

        private ArrayList<String> mFavorites = new ArrayList<String>();
        private String mTppCycle;
        private String mTppVolume;
        private Cursor mCursor;
        private IntentFilter mFilter;
        private BroadcastReceiver mReceiver;
        private View mDownloadRow;
        private ProgressBar mProgressBar;
        private OnClickListener mOnClickListener;
        private boolean mExpired;
        private boolean mGoodNetwork;

        @Override
        public void onCreate( Bundle savedInstanceState ) {
            mFilter = new IntentFilter();
            mFilter.addAction( DtppService.ACTION_GET_CHARTS );

            mReceiver = new BroadcastReceiver() {
                
                @Override
                public void onReceive( Context context, Intent intent ) {
                    onChartDownload( context, intent );
                }
            };

            mOnClickListener = new OnClickListener() {
                
                @Override
                public void onClick( View v ) {
                    if ( mDownloadRow == null ) {
                        mDownloadRow = v;
                        String tppVolume = (String) v.getTag();
                        VolumeDownloadTask task = new VolumeDownloadTask();
                        task.execute( tppVolume );
                    }
                }
            };

            super.onCreate( savedInstanceState );
        }

        @Override
        public void onResume() {
            getActivity().registerReceiver( mReceiver, mFilter );

            super.onResume();
        }

        @Override
        public void onPause() {
            getActivity().unregisterReceiver( mReceiver );
            if ( mCursor != null ) {
                mCursor.close();
                mCursor = null;
            }

            super.onPause();
        }

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            View v = inflate( R.layout.charts_download_view );
            return createContentView( v );
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            setBackgroundTask( new ChartsDownloadTask() ).execute();

            super.onActivityCreated( savedInstanceState );
        }

        private class ChartsDownloadTask extends CursorAsyncTask {

            @Override
            protected Cursor[] doInBackground( String... params ) {
                DatabaseManager dbManager = getDbManager();
                ArrayList<String> siteNumbers = dbManager.getAptFavorites();

                String selection = "";
                for (String siteNumer : siteNumbers ) {
                    if ( selection.length() > 0 ) {
                        selection += ", ";
                    }
                    selection += "'"+siteNumer+"'";
                };

                SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
                selection = "a."+Airports.SITE_NUMBER+" in ("+selection+")";
                Cursor c = AirportsCursorHelper.query( db, selection, 
                        null, null, null, Airports.FACILITY_NAME, null );
                if ( !c.moveToFirst() ) {
                    return null;
                }

                do {
                    String faaCode = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
                    mFavorites.add( faaCode );
                } while ( c.moveToNext() );

                selection = "";
                for (String faaCode : mFavorites ) {
                    if ( selection.length() > 0 ) {
                        selection += ", ";
                    }
                    selection += "'"+faaCode+"'";
                };

                Cursor[] result = new Cursor[ 3 ];

                db = getDatabase( DatabaseManager.DB_DTPP );

                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables( DtppCycle.TABLE_NAME );
                c = builder.query( db, new String[] { "*" },
                        null, null, null, null, null, null );
                result[ 0 ] = c;

                builder = new SQLiteQueryBuilder();
                builder.setTables( Dtpp.TABLE_NAME );
                c = builder.query( db, new String[] { "*" }, Dtpp.FAA_CODE+" in ("+selection+")",
                        null, null, null, null, null );
                result[ 1 ] = c;

                builder = new SQLiteQueryBuilder();
                builder.setTables( Dtpp.TABLE_NAME );
                c = builder.query( db, new String[] { Dtpp.TPP_VOLUME, 
                        "count(DISTINCT "+Dtpp.PDF_NAME+") AS total" },
                        Dtpp.USER_ACTION+"!='D'",
                        null, Dtpp.TPP_VOLUME, null, null, null );
                result[ 2 ] = c;

                return result;
            }

            @Override
            protected boolean onResult( Cursor[] result ) {
                showDownloadInfo( result );
                return true;
            }
        }

        private void showDownloadInfo( Cursor[] result ) {
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
            SimpleDateFormat df = new SimpleDateFormat( "HHmm'Z' MM/dd/yy" );
            df.setTimeZone( java.util.TimeZone.getTimeZone( "UTC" ) );
            Date endDate = null;
            try {
                endDate = df.parse( expiry );
            } catch ( ParseException e ) {
            }

            // Determine if chart cycle has expired
            Date now = new Date();
            if ( now.getTime() > endDate.getTime() ) {
                mExpired = true;
            }

            TextView tv = (TextView) findViewById( R.id.charts_cycle_expiry );
            if ( mExpired ) {
                tv.setText( "This chart cycle has expired on "
                        +TimeUtils.formatDateTime( getActivity(), endDate.getTime() ) );
            } else {
                tv.setText( "This chart cycle expires on "
                        +TimeUtils.formatDateTime( getActivity(), endDate.getTime() ) );
            }

            tv = (TextView) findViewById( R.id.charts_download_msg );
            tv.setText( "Each chart volume is about 150-250MB in size and may take 15-30 mins"
                    +" to download. Charts are stored on the external SD card storage."
                    +" Long press to delete all the charts for a volume."
                    +" Press 'Back' button to stop the running download."
                    +" Download will not start if you are connected to a metered network.");

            tv = (TextView) findViewById( R.id.charts_download_warning );
            if ( !NetworkUtils.isNetworkAvailable( getActivity() ) ) {
                tv.setText( "No connected to the internet" );
                mGoodNetwork = false;
            } else if ( NetworkUtils.isConnectedToMeteredNetwork( getActivity() ) ) {
                tv.setText( "Connected to a metered network" );
                mGoodNetwork = false;
            } else {
                tv.setText( "Connected to an unmetered network" );
                tv.setCompoundDrawablesWithIntrinsicBounds( R.drawable.check, 0, 0, 0 );
                tv.setCompoundDrawablePadding( UiUtils.convertDpToPx( getActivity(), 2 ) );
                mGoodNetwork = true;
            }

            LinearLayout layout = (LinearLayout) findViewById( R.id.vol_chart_details );
            c = result[ 2 ];
            if ( c.moveToFirst() ) {
                do {
                    String tppVolume = c.getString( c.getColumnIndex( Dtpp.TPP_VOLUME ) );
                    int total = c.getInt( c.getColumnIndex( "total" ) );
                    int avail = DtppService.getChartsCount( mTppCycle, tppVolume );
                    int resid = UiUtils.getRowSelectorForCursor( c );
                    addTppVolumeRow( layout, tppVolume, avail, total, resid );
                } while ( c.moveToNext() );
            }

            setFragmentContentShown( true );
        }

        private class VolumeDownloadTask extends CursorAsyncTask {

            @Override
            protected Cursor[] doInBackground( String... params ) {
                mTppVolume = params[ 0 ];
                SQLiteDatabase db = getDatabase( DatabaseManager.DB_DTPP );

                Cursor[] result = new Cursor[ 1 ];

                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables( Dtpp.TABLE_NAME );
                Cursor c = builder.query( db,
                        new String[] { Dtpp.PDF_NAME },
                        Dtpp.TPP_VOLUME+"=? AND "+Dtpp.USER_ACTION+"!=?",
                        new String[] { mTppVolume, "D" },
                        Dtpp.PDF_NAME+","+Dtpp.TPP_VOLUME,
                        null, null );
                result[ 0 ] = c;

                return result;
            }

            @Override
            protected boolean onResult( Cursor[] result ) {
                mCursor = result[ 0 ];
                mCursor.moveToFirst();
                mProgressBar = (ProgressBar) mDownloadRow.findViewById( R.id.progress );
                mProgressBar.setMax( mCursor.getCount() );
                Log.d( "TOTAL", String.valueOf( mCursor.getCount() ) );
                getNextChart();
                return false;
            }
        }

        protected void onChartDownload( Context context, Intent intent ) {
            if ( mCursor != null ) {
                mProgressBar.setProgress( mCursor.getPosition() );
                if ( mCursor.moveToNext() ) {
                    getNextChart();
                } else {
                    int avail = DtppService.getChartsCount( mTppCycle, mTppVolume );
                    int total = mCursor.getCount();
                    showStatus( mDownloadRow, avail, total );
                    mProgressBar.setVisibility( View.GONE );
                    mDownloadRow = null;
                    mTppVolume = null;
                    mCursor.close();
                    mCursor = null;
                    return;
                }
            }
        }

        protected void getNextChart() {
            mProgressBar.setVisibility( View.VISIBLE );
            String pdfName = mCursor.getString( mCursor.getColumnIndex( Dtpp.PDF_NAME ) );
            ArrayList<String> pdfNames = new ArrayList<String>();
            pdfNames.add( pdfName );
            Intent service = new Intent( getActivity(), DtppService.class );
            service.setAction( DtppService.ACTION_GET_CHARTS );
            service.putExtra( DtppService.CYCLE_NAME, mTppCycle );
            service.putExtra( DtppService.TPP_VOLUME, mTppVolume );
            service.putExtra( DtppService.PDF_NAMES, pdfNames );
            getActivity().startService( service );
        }

        protected View addTppVolumeRow( LinearLayout layout, String tppVolume,
                int avail, int total, int resid ) {
            if ( layout.getChildCount() > 0 ) {
                addSeparator( layout );
            }

            RelativeLayout row = (RelativeLayout) inflate( R.layout.list_item_with_progressbar );

            TextView tv = (TextView) row.findViewById( R.id.item_label );
            tv.setText( tppVolume );
            tv = (TextView) row.findViewById( R.id.item_value );
            tv.setText( String.format( "%d charts", total ) );

            if ( mGoodNetwork && !mExpired && avail < total ) {
                row.setOnClickListener( mOnClickListener );
                row.setBackgroundResource( resid );
                row.setTag( tppVolume );
            }

            showStatus( row, avail, total );

            layout.addView( row, new RelativeLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );

            return row;
        }

        protected void showStatus( View view, int avail, int total ) {
            TextView tv = (TextView) view.findViewById( R.id.item_label );
            if ( avail == total ) {
                tv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.btn_check_on_holo_light, 0, 0, 0 );
            } else {
                tv.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.btn_check_off_holo_light, 0, 0, 0 );
            }
        }

    }

}
