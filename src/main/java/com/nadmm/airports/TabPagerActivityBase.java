/*
 * FlightIntel for Pilots
 *
 * Copyright 2015-2017 Nadeem Hasan <nhasan@nadmm.com>
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

import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;

import com.nadmm.airports.utils.PagerAdapter;

public abstract class TabPagerActivityBase extends ActivityBase {

    private int mCurrentFragmentIndex = -1;
    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    private TabLayout mTabLayout;

    private static final String SAVED_TAB = "saved_tab";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_tab_pager );

        mViewPager = findViewById( R.id.view_pager );
        mPagerAdapter = new PagerAdapter( this, getSupportFragmentManager(), mViewPager );

        mViewPager.addOnPageChangeListener( new ViewPager.SimpleOnPageChangeListener() {

            public void onPageScrollStateChanged( int state ) {
                // Disable the swipe refresh while moving between pages
                if ( getCurrentFragment().isRefreshable() ) {
                    enableDisableSwipeRefresh( state == ViewPager.SCROLL_STATE_IDLE );
                }
            }
        } );

        mTabLayout = findViewById( R.id.sliding_tabs );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        mTabLayout.setupWithViewPager( mViewPager );
        mTabLayout.addOnTabSelectedListener( new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected( TabLayout.Tab tab ) {
                mCurrentFragmentIndex = tab.getPosition();
                mViewPager.setCurrentItem( mCurrentFragmentIndex );
                enableDisableSwipeRefresh( getCurrentFragment().isRefreshable() );
                showAppBar( true );
            }

            @Override
            public void onTabUnselected( TabLayout.Tab tab ) {
            }

            @Override
            public void onTabReselected( TabLayout.Tab tab ) {
                if ( mCurrentFragmentIndex != tab.getPosition() ) {
                    onTabSelected( tab );
                }
            }
        } );

        if ( savedInstanceState != null ) {
            mCurrentFragmentIndex = savedInstanceState.getInt( SAVED_TAB );
        } else {
            mCurrentFragmentIndex = getInitialTabIndex();
        }

        postRunnable( () -> {
            mTabLayout.getTabAt( mCurrentFragmentIndex ).select();
            enableDisableSwipeRefresh( getCurrentFragment().isRefreshable() );
        }, 0 );
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
        return getCurrentFragment().canSwipeRefreshChildScrollUp();
    }

    public void addTab( String label, Class<?> clss, Bundle args ) {
        mPagerAdapter.addTab( label, clss, args );
    }

    private FragmentBase getCurrentFragment() {
        return (FragmentBase) mPagerAdapter.getItem( mCurrentFragmentIndex );
    }

    protected int getInitialTabIndex() {
        return 0;
    }

}
