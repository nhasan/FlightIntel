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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Dtpp;
import com.nadmm.airports.DatabaseManager.DtppCycle;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;

public class SubscribeDtppFragment extends FragmentBase {

    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        mFilter = new IntentFilter();
        mFilter.addAction( DtppService.ACTION_GET_CHARTS );
        mFilter.addAction( DtppService.ACTION_CHECK_CHARTS );
        mFilter.addAction( DtppService.ACTION_COUNT_CHARTS );

        mReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive( Context context, Intent intent ) {
                String action = intent.getAction();
                if ( action.equals( DtppService.ACTION_GET_CHARTS ) ) {
                } else if ( action.equals( DtppService.ACTION_CHECK_CHARTS ) ) {
                } else if ( action.equals( DtppService.ACTION_COUNT_CHARTS ) ) {
                }
            }
        };
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
        View v = inflate( R.layout.charts_download_view );
        return createContentView( v );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        setBackgroundTask( new VolumeListTask() ).execute();

        super.onActivityCreated( savedInstanceState );
    }

    private class VolumeListTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
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

        @Override
        protected boolean onResult( Cursor[] result ) {
            return true;
        }
    }

}
