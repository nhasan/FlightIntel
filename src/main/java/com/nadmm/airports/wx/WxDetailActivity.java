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
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.NavAdapter;
import com.nadmm.airports.utils.PagerAdapter;
import com.nadmm.airports.views.SlidingTabLayout;

public class WxDetailActivity extends ActivityBase {

    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    private SlidingTabLayout mSlidingTabLayout;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Intent intent = getIntent();
        Bundle args = intent.getExtras();

        setContentView( R.layout.activity_tab_pager );

        mViewPager = (ViewPager) findViewById( R.id.view_pager );

        mPagerAdapter = new PagerAdapter( this, getSupportFragmentManager(), mViewPager );
        mPagerAdapter.addTab( "METAR", MetarFragment.class, args );
        mPagerAdapter.addTab( "TAF", TafFragment.class, args );
        mPagerAdapter.addTab( "PIREP", PirepFragment.class, args );
        mPagerAdapter.addTab( "AIRMET/SIGMET", AirSigmetFragment.class, args );
        mPagerAdapter.addTab( "RADAR", RadarFragment.class, args );
        mPagerAdapter.addTab( "PROGNOSIS CHARTS", ProgChartFragment.class, args );
        mPagerAdapter.addTab( "WINDS/TEMPERATURE", WindFragment.class, args );
        mPagerAdapter.addTab( "WINDS ALOFT", WindsAloftFragment.class, args );
        mPagerAdapter.addTab( "SIG WX", SigWxFragment.class, args );
        mPagerAdapter.addTab( "CEILING & VISIBILIY", CvaFragment.class, args );
        mPagerAdapter.addTab( "ICING", IcingFragment.class, args );
        mPagerAdapter.addTab( "AREA FORECAST", AreaForecastFragment.class, args );

        Resources res = getResources();
        mSlidingTabLayout = (SlidingTabLayout) findViewById( R.id.sliding_tabs );
        mSlidingTabLayout.setCustomTabView( R.layout.tab_indicator, android.R.id.text1 );
        mSlidingTabLayout.setSelectedIndicatorColors( res.getColor( R.color.tab_selected_strip ) );
        mSlidingTabLayout.setDistributeEvenly( false );
        mSlidingTabLayout.setViewPager( mViewPager );

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
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        outState.putInt( "wxtab", mViewPager.getCurrentItem() );
    }

    @Override
    protected void setContentView() {
        setContentView( R.layout.fragment_pager_no_tab_layout );
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    private void setCurrentPage( int pos ) {
        // Workaround for race conditions in ViewPager
        // See: http://code.google.com/p/android/issues/detail?id=29472
        if ( mViewPager.getCurrentItem() != pos ) {
            mViewPager.setCurrentItem( pos );
        }
    }

}
