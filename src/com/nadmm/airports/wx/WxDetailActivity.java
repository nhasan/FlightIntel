/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.wx;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.TabsAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class WxDetailActivity extends ActivityBase {

    private BroadcastReceiver mReceiver;
    TabsAdapter mTabsAdapter;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        setContentView( R.layout.fragment_view_pager_layout );

        mReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive( Context context, Intent intent ) {
                MetarFragment metar = (MetarFragment) mTabsAdapter.getItem( 0 );
                metar.onReceiveResult( intent );
            }

        };

        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );

        mTabsAdapter = new TabsAdapter( this, pager );
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        mTabsAdapter.addTab( "METAR", MetarFragment.class, args );

        TabPageIndicator tabIndicator = (TabPageIndicator) findViewById( R.id.page_titles );
        tabIndicator.setViewPager( pager );

        if ( savedInstanceState != null ) {
            pager.setCurrentItem( savedInstanceState.getInt( "wxtab" ) );
        }

        super.onCreate( savedInstanceState );
    }

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        outState.putInt( "wxtab", pager.getCurrentItem() );
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction( MetarService.ACTION_GET_METAR );
        registerReceiver( mReceiver, filter );
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver( mReceiver );
    }

}
