/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2013 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.library;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.nadmm.airports.DrawerActivity;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.views.DrawerListView;

public class LibraryActivity extends DrawerActivity {

    private boolean mPending = false;
    private final Object mLock = new Object();
    private HashMap<String, BroadcastReceiver> mReceivers;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mReceivers = new HashMap<String, BroadcastReceiver>();
        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                String category = intent.getStringExtra( LibraryService.CATEGORY );
                BroadcastReceiver receiver = mReceivers.get( category );
                if ( receiver != null ) {
                    receiver.onReceive( context, intent );
                }

                // Show the PDF here as the Fragment requesting it may be paused
                String action = intent.getAction();

                if ( action.equals( LibraryService.ACTION_GET_BOOK ) ) {
                    String path = intent.getStringExtra( LibraryService.PDF_PATH );
                    mPending = false;
                    if ( path != null ) {
                        SystemUtils.startPDFViewer( LibraryActivity.this, path );
                    }
                }
            }
        };
        mFilter = new IntentFilter();
        mFilter.setPriority( 10 );
        mFilter.addAction( LibraryService.ACTION_CHECK_BOOKS );
        mFilter.addAction( LibraryService.ACTION_GET_BOOK );
        mFilter.addAction( LibraryService.ACTION_DOWNLOAD_PROGRESS );

        Bundle args = getIntent().getExtras();
        addFragment( LibraryMainFragment.class, args );
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.registerReceiver( mReceiver, mFilter );
        setDrawerItemChecked( DrawerListView.ITEM_ID_LIBRARY );

        super.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.unregisterReceiver( mReceiver );

        super.onPause();
    }

    public void setPending( boolean pending ) {
        synchronized ( mLock ) {
            mPending = pending;
        }
    }

    public boolean isPending() {
        synchronized ( mLock ) {
            return mPending;
        }
    }

    public void registerReceiver( String category, BroadcastReceiver receiver ) {
        mReceivers.put( category, receiver );
    }

    public void unregisterReceiver( String category ) {
        mReceivers.remove( category );
    }

}
