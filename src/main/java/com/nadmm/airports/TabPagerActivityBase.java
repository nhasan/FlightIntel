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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.views.SlidingTabLayout;

import java.util.HashMap;
import java.util.List;

public abstract class TabPagerActivityBase extends ActivityBase {

    private HashMap<String, Fragment> mWxFragments = new HashMap<>();
    private int mCurrentFragmentIndex = -1;
    private ViewPager mViewPager = null;
    private ViewPagerAdapter mViewPagerAdapter = null;
    private SlidingTabLayout mSlidingTabLayout = null;

    private String[] mTabTitles;
    private Class<?>[] mClasses;

    private static final String SAVED_TAB = "saved_tab";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mTabTitles = getTabTitles();
        mClasses = getTabFragments();

        setContentView( R.layout.activity_tab_pager );

        initFragments( savedInstanceState );

        mViewPager = (ViewPager) findViewById( R.id.view_pager );
        mViewPagerAdapter = new ViewPagerAdapter( getSupportFragmentManager() );
        mViewPager.setAdapter( mViewPagerAdapter );

        Resources res = getResources();
        mSlidingTabLayout = (SlidingTabLayout) findViewById( R.id.sliding_tabs );
        mSlidingTabLayout.setCustomTabView( R.layout.tab_indicator, android.R.id.text1 );
        mSlidingTabLayout.setSelectedIndicatorColors( res.getColor( R.color.tab_selected_strip ) );
        mSlidingTabLayout.setDistributeEvenly( false );
        mSlidingTabLayout.setViewPager( mViewPager );

        mSlidingTabLayout.setOnPageChangeListener( new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled( int position, float v, int i1 ) {
            }

            @Override
            public void onPageSelected( int position ) {
                mCurrentFragmentIndex = position;
                enableDisableSwipeRefresh( getCurrentFragment().isRefreshable() );
                // Show the actionbar when a new page is selected
                resetActionBarAutoHide();
                autoShowOrHideActionBar( true );
            }

            @Override
            public void onPageScrollStateChanged( int state ) {
                enableDisableSwipeRefresh( state == ViewPager.SCROLL_STATE_IDLE
                        && getCurrentFragment().isRefreshable() );
            }
        } );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        enableActionBarAutoHide();
        registerHideableHeaderView( findViewById( R.id.headerbar ),
                UiUtils.calculateActionBarSize( this ) );

        if ( savedInstanceState != null ) {
            mCurrentFragmentIndex = savedInstanceState.getInt( SAVED_TAB );
        } else {
            mCurrentFragmentIndex = getInitialTabIndex();
        }
        mViewPager.setCurrentItem( mCurrentFragmentIndex );
        enableDisableSwipeRefresh( getCurrentFragment().isRefreshable() );
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );

        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> fragments = fm.getFragments();
        for ( Fragment fragment : fragments ) {
            // Save the fragments so we can restore them later
            fm.putFragment( outState, fragment.getClass().getName(), fragment );
        }

        outState.putInt( SAVED_TAB, mViewPager.getCurrentItem() );
    }

    @Override
    public void onFragmentStarted( FragmentBase fragment ) {
        updateContentTopClearance( fragment );
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

    private void updateContentTopClearance( FragmentBase fragment ) {
        int actionbarClearance = UiUtils.calculateActionBarSize( this );
        int tabbarClearance = getResources().getDimensionPixelSize( R.dimen.tabbar_height );
        fragment.setContentTopClearance( actionbarClearance + tabbarClearance );
        setProgressBarTopWhenActionBarShown( actionbarClearance + tabbarClearance );
    }

    private void initFragments( Bundle savedInstanceState ) {
        if ( savedInstanceState != null ) {
            FragmentManager fm = getSupportFragmentManager();
            // Activity was recreated, check if our fragments survived
            for ( Class<?> clss : mClasses ) {
                // Restore the fragments from state saved earlier
                Fragment fragment = fm.getFragment( savedInstanceState, clss.getName() );
                if ( fragment != null ) {
                    mWxFragments.put( clss.getName(), fragment );
                }
            }
        }

        Bundle args = getIntent().getExtras();
        for ( Class<?> clss : mClasses ) {
            if ( !mWxFragments.containsKey( clss.getName() ) ) {
                Fragment fragment = Fragment.instantiate( this, clss.getName(), args );
                mWxFragments.put( clss.getName(), fragment );
            }
        }
    }

    private FragmentBase getCurrentFragment() {
        return getFragmentAtPosition( mCurrentFragmentIndex );
    }

    protected FragmentBase getFragmentAtPosition( int position ) {
        return (FragmentBase) mWxFragments.get( mClasses[ position ].getName() );
    }

    protected abstract String[] getTabTitles();

    protected abstract Class<?>[] getTabFragments();

    protected abstract int getInitialTabIndex();

    private class ViewPagerAdapter extends FragmentPagerAdapter {

        public ViewPagerAdapter( FragmentManager fm ) {
            super( fm );
        }

        @Override
        public Fragment getItem( int position ) {
            return getFragmentAtPosition( position );
        }

        @Override
        public int getCount() {
            return mTabTitles.length;
        }

        @Override
        public CharSequence getPageTitle( int position ) {
            return mTabTitles[ position ];
        }

    }

}
