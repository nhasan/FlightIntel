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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

public class BatchDownloadActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_activity_layout );

        Bundle args = getIntent().getExtras();
        addFragment( BatchDownloadFragment.class, args );
    }

    public static class BatchDownloadFragment extends FragmentBase {

        private ArrayList<String> mFavorites = new ArrayList<String>();
        private String mDtppCycle;
        private ArrayList<String> mDtppPdfNames = new ArrayList<String>();

        @Override
        public View onCreateView( LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState ) {
            View v = inflate( R.layout.batch_download_view );
            return createContentView( v );
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            setBackgroundTask( new BatchDownloadTask() ).execute();

            super.onActivityCreated( savedInstanceState );
        }

        private class BatchDownloadTask extends CursorAsyncTask {

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
                        "count(DISTINCT "+Dtpp.PDF_NAME+") AS total" }, null,
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
                mDtppCycle = c.getString( c.getColumnIndex( DtppCycle.TPP_CYCLE ) );
            } else {
                UiUtils.showToast( getActivity(), "d-TPP database is not installed" );
                getActivity().finish();
                return;
            }

            getSupportActionBar().setSubtitle( String.format( "AeroNav Cycle %s", mDtppCycle ) );

            String expiry = c.getString( c.getColumnIndex( DtppCycle.TO_DATE ) );
            TextView tv = (TextView) findViewById( R.id.charts_cycle_expiry );
            SimpleDateFormat df = new SimpleDateFormat( "HHmm'Z' MM/dd/yy" );
            df.setTimeZone( java.util.TimeZone.getTimeZone( "UTC" ) );
            try {
                Date dt = df.parse( expiry );
                tv.setText( "This cycle expires on "
                        +TimeUtils.formatDateTime( getActivity(), dt.getTime() ) );
            } catch ( ParseException e ) {
            }

            LinearLayout layout = (LinearLayout) findViewById( R.id.fav_chart_details );
            if ( !mFavorites.isEmpty() ) {
                c = result[ 1 ];
                int numCharts = c.getCount();
                addRow( layout, String.format( "%d Favorite airports", mFavorites.size() ),
                        String.format( "%d charts", numCharts ) );
                if ( numCharts > 0 ) {
                    // Make clickable
                }
            } else {
                addRow( layout, "No favorite airports have been selected yet" );
            }

            layout = (LinearLayout) findViewById( R.id.vol_chart_details );
            c = result[ 2 ];
            if ( c.moveToFirst() ) {
                do {
                    String volume = c.getString( c.getColumnIndex( Dtpp.TPP_VOLUME ) );
                    int count = c.getInt( c.getColumnIndex( "total" ) );
                    View row = addRow( layout, volume, String.format( "%d charts", count ) );
                    row.setTag( volume );
                } while ( c.moveToNext() );
            }

            setFragmentContentShown( true );
        }

    }

}
