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

package com.nadmm.airports.library;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.SlidingMenuFragment;
import com.nadmm.airports.DatabaseManager.BookCategories;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.TabsAdapter;

public class LibraryActivity extends ActivityBase {

    private TabsAdapter mTabsAdapter;
    private boolean mPending = false;
    private final Object mLock = new Object();
    private HashMap<String, BroadcastReceiver> mReceivers;
    private BroadcastReceiver mReceiver;
    private IntentFilter mFilter;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( createContentView( R.layout.fragment_pager_layout ) );

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

        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        mTabsAdapter = new TabsAdapter( this, getSupportFragmentManager(), pager );

        PagerTabStrip tabs = (PagerTabStrip) findViewById( R.id.pager_tabs );
        tabs.setTabIndicatorColor( getResources().getColor( R.color.tab_indicator ) );

        if ( savedInstanceState != null ) {
            pager.setCurrentItem( savedInstanceState.getInt( "wxtab" ) );
        }

        SQLiteDatabase db = getDatabase( DatabaseManager.DB_LIBRARY );
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( BookCategories.TABLE_NAME );
        Cursor c = builder.query( db, new String[] { "*" }, null, null, null, null,
                BookCategories._ID );
        if ( c.moveToFirst() ) {
            do {
                String code = c.getString( c.getColumnIndex( BookCategories.CATEGORY_CODE ) );
                String name = c.getString( c.getColumnIndex( BookCategories.CATEGORY_NAME ) );
                Bundle args = new Bundle();
                args.putString( BookCategories.CATEGORY_CODE, code );
                mTabsAdapter.addTab( name, LibraryFragment.class, args );
            } while ( c.moveToNext() );
        }
    }

    @Override
    protected void onStart() {
        setContentShown( true );
        super.onStart();
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.registerReceiver( mReceiver, mFilter );
        setSlidingMenuActivatedItem( SlidingMenuFragment.ITEM_ID_LIBRARY );

        super.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.unregisterReceiver( mReceiver );

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        outState.putInt( "wxtab", pager.getCurrentItem() );
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
