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


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.TabsAdapter;
import com.viewpagerindicator.TabPageIndicator;

public class WxDetailActivity extends ActivityBase {

    private TabsAdapter mTabsAdapter;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        setContentView( R.layout.fragment_view_pager_layout );

        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        mTabsAdapter = new TabsAdapter( this, pager );
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        mTabsAdapter.addTab( "METAR", MetarFragment.class, args );
        mTabsAdapter.addTab( "TAF", TafFragment.class, args );
        mTabsAdapter.addTab( "PIREP", PirepFragment.class, args );
        mTabsAdapter.addTab( "AIRSIGMET", AirSigmetFragment.class, args );
        mTabsAdapter.addTab( "RADAR", RadarFragment.class, args );
        mTabsAdapter.addTab( "PROGCHART", ProgChartFragment.class, args );
        mTabsAdapter.addTab( "WIND", WindFragment.class, args );
        mTabsAdapter.addTab( "SIGWX", SigWxFragment.class, args );

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

}
