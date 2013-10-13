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
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.widget.ArrayAdapter;

import com.google.analytics.tracking.android.EasyTracker;
import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.TabsAdapter;

public class WxDetailActivity extends ActivityBase
                implements OnNavigationListener, ViewPager.OnPageChangeListener {

    private TabsAdapter mTabsAdapter;
    private ViewPager mViewPager;
    private ActionBar mActionBar;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_pager_no_tab_layout );

        Intent intent = getIntent();
        Bundle args = intent.getExtras();

        mViewPager = (ViewPager) findViewById( R.id.content_pager );
        mViewPager.setOnPageChangeListener( this );

        mTabsAdapter = new TabsAdapter( this, getSupportFragmentManager(), mViewPager );
        mTabsAdapter.addTab( "METAR", MetarFragment.class, args );
        mTabsAdapter.addTab( "TAF", TafFragment.class, args );
        mTabsAdapter.addTab( "PIREP", PirepFragment.class, args );
        mTabsAdapter.addTab( "AIRMET/SIGMET", AirSigmetFragment.class, args );
        mTabsAdapter.addTab( "RADAR", RadarFragment.class, args );
        mTabsAdapter.addTab( "PROGNOSIS CHARTS", ProgChartFragment.class, args );
        mTabsAdapter.addTab( "WINDS/TEMPERATURE", WindFragment.class, args );
        mTabsAdapter.addTab( "WINDS ALOFT", WindsAloftFragment.class, args );
        mTabsAdapter.addTab( "SIG WX", SigWxFragment.class, args );
        mTabsAdapter.addTab( "CEILING & VISIBILIY", CvaFragment.class, args );
        mTabsAdapter.addTab( "ICING", IcingFragment.class, args );
        mTabsAdapter.addTab( "AREA FORECAST", AreaForecastFragment.class, args );

        // Build the data model for the spinner adapter
        String[] titles = new String[ mTabsAdapter.getCount() ];
        int pos = 0;
        while ( pos < mTabsAdapter.getCount() ) {
            titles[ pos ] = mTabsAdapter.getPageTitle( pos ).toString();
            ++pos;
        }

        // Setup list navigation mode
        mActionBar = getSupportActionBar();
        mActionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_LIST );
        ArrayAdapter<String> adapter = new ArrayAdapter<String>( mActionBar.getThemedContext(),
                R.layout.spinner_dropdown_item, titles );
        adapter.setDropDownViewResource( R.layout.spinner_item );
        mActionBar.setListNavigationCallbacks( adapter, this );
        mActionBar.setDisplayShowTitleEnabled( false );

        if ( savedInstanceState != null ) {
            // Workaround for race conditions in ViewPager
            // See: http://code.google.com/p/android/issues/detail?id=29472
            final int lastPos = savedInstanceState.getInt( "wxtab" );
            mViewPager.post( new Runnable() {

                @Override
                public void run() {
                    setCurrentPage( lastPos );
                }
            } );
        }
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

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        outState.putInt( "wxtab", pager.getCurrentItem() );
    }

    @Override
    public boolean onNavigationItemSelected( int itemPosition, long itemId ) {
        mViewPager.setCurrentItem( itemPosition );
        return true;
    }

    @Override
    public void onPageScrollStateChanged( int arg0 ) {
    }

    @Override
    public void onPageScrolled( int arg0, float arg1, int arg2 ) {
    }

    @Override
    public void onPageSelected( int position ) {
        mActionBar.setSelectedNavigationItem( position );
    }

    private void setCurrentPage( int pos ) {
        // Workaround for race conditions in ViewPager
        // See: http://code.google.com/p/android/issues/detail?id=29472
        if ( mViewPager.getCurrentItem() != pos ) {
            mViewPager.setCurrentItem( pos );
        }
    }

}
