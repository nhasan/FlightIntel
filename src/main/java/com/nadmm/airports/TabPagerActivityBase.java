/*
 * FlightIntel for Pilots
 *
 * Copyright 2015 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import com.nadmm.airports.utils.PagerAdapter;

import java.util.HashMap;

public class TabPagerActivityBase extends ActivityBase {

    private HashMap<String, Fragment> mWxFragments = new HashMap<>();
    private int mCurrentFragmentIndex = -1;
    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    private TabLayout mTabLayout;

    private static final String SAVED_TAB = "saved_tab";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_tab_pager );

        mViewPager = (ViewPager) findViewById( R.id.view_pager );
        mPagerAdapter = new PagerAdapter( this, getSupportFragmentManager(), mViewPager );

        mViewPager.addOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener() {

            public void onPageScrollStateChanged( int state ) {
                // Disable the swipe refresh while moving between pages
                if ( getCurrentFragment().isRefreshable() ) {
                    enableDisableSwipeRefresh( state == ViewPager.SCROLL_STATE_IDLE );
                }
            }
        } );

        Resources res = getResources();
        mTabLayout = (TabLayout) findViewById( R.id.sliding_tabs );

        mTabLayout.setOnTabSelectedListener( new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected( TabLayout.Tab tab ) {
                mCurrentFragmentIndex = tab.getPosition();
                enableDisableSwipeRefresh( getCurrentFragment().isRefreshable() );
                // Show the actionbar when a new page is selected
                resetActionBarAutoHide();
                autoShowOrHideActionBar( true );
            }

            @Override
            public void onTabUnselected( TabLayout.Tab tab ) {
            }

            @Override
            public void onTabReselected( TabLayout.Tab tab ) {
            }
        } );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        mTabLayout.setupWithViewPager( mViewPager );

        if ( savedInstanceState != null ) {
            mCurrentFragmentIndex = savedInstanceState.getInt( SAVED_TAB );
        } else {
            mCurrentFragmentIndex = getInitialTabIndex();
        }
        mViewPager.setCurrentItem( mCurrentFragmentIndex );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );

        outState.putInt( SAVED_TAB, mViewPager.getCurrentItem() );
    }

    @Override
    protected void requestDataRefresh() {
        FragmentBase fragment = getCurrentFragment();
        if ( fragment.isRefreshable() ) {
            fragment.requestDataRefresh();
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        FragmentBase fragment = getCurrentFragment();
        return fragment != null && fragment.canSwipeRefreshChildScrollUp();
    }

    public void addTab( String label, Class<?> clss, Bundle args ) {
        mPagerAdapter.addTab( label, clss, args );
    }

    private FragmentBase getCurrentFragment() {
        return getFragmentAtPosition( mCurrentFragmentIndex );
    }

    protected FragmentBase getFragmentAtPosition( int position ) {
        return (FragmentBase) mPagerAdapter.getItem( position );
    }

    protected int getInitialTabIndex() {
        return 0;
    }

}
