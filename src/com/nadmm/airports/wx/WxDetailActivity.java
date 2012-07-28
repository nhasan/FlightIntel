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


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.TabsAdapter;
import com.viewpagerindicator.UnderlinePageIndicator;

public class WxDetailActivity extends ActivityBase
                implements OnNavigationListener, ViewPager.OnPageChangeListener {

    private TabsAdapter mTabsAdapter;
    private ViewPager mPager;

    private static final String[] mLabels = new String[] {
        "METAR",
        "TAF",
        "Pilot Reports",
        "Airmets/Sigmets",
        "Radar",
        "Prognosis Charts",
        "Winds",
        "Significant Wx",
        "Ceiling & Visibility"
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        setContentView( R.layout.fragment_pager_vpi_layout );

        mPager = (ViewPager) findViewById( R.id.content_pager );

        mTabsAdapter = new TabsAdapter( this, mPager );
        Intent intent = getIntent();
        Bundle args = intent.getExtras();
        mTabsAdapter.addTab( mLabels[ 0 ], MetarFragment.class, args );
        mTabsAdapter.addTab( mLabels[ 1 ], TafFragment.class, args );
        mTabsAdapter.addTab( mLabels[ 2 ], PirepFragment.class, args );
        mTabsAdapter.addTab( mLabels[ 3 ], AirSigmetFragment.class, args );
        mTabsAdapter.addTab( mLabels[ 4 ], RadarFragment.class, args );
        mTabsAdapter.addTab( mLabels[ 5 ], ProgChartFragment.class, args );
        mTabsAdapter.addTab( mLabels[ 6 ], WindFragment.class, args );
        mTabsAdapter.addTab( mLabels[ 7 ], SigWxFragment.class, args );
        mTabsAdapter.addTab( mLabels[ 8 ], CvaFragment.class, args );

        Context context = getSupportActionBar().getThemedContext();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( context,
                R.layout.sherlock_spinner_item, mLabels );
        adapter.setDropDownViewResource( R.layout.sherlock_spinner_dropdown_item );
        getSupportActionBar().setNavigationMode( ActionBar.NAVIGATION_MODE_LIST );
        getSupportActionBar().setListNavigationCallbacks( adapter, this );

        UnderlinePageIndicator indicator = (UnderlinePageIndicator) findViewById( R.id.indicator );
        indicator.setViewPager( mPager );
        indicator.setSelectedColor( 0xFF33B5E5 );
        indicator.setBackgroundColor( 0xFFAAAAAA );
        indicator.setFadeDelay(3000);
        indicator.setFadeLength(1000);
        indicator.setOnPageChangeListener( this );

        if ( savedInstanceState != null ) {
            mPager.setCurrentItem( savedInstanceState.getInt( "wxtab" ) );
        }

        super.onCreate( savedInstanceState );
    }

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        outState.putInt( "wxtab", mPager.getCurrentItem() );
    }

    @Override
    public boolean onNavigationItemSelected( int itemPosition, long itemId ) {
        mPager.setCurrentItem( itemPosition );
        return false;
    }

    @Override
    public void onPageScrollStateChanged( int arg0 ) {
    }

    @Override
    public void onPageScrolled( int arg0, float arg1, int arg2 ) {
    }

    @Override
    public void onPageSelected( int arg0 ) {
        getSupportActionBar().setSelectedNavigationItem( arg0 );
    }

}
