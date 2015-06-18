/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.afd;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.views.SlidingTabLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class AfdMainActivity extends ActivityBase {

    private final String[] mTabTitles = new String[] {
            "Favorites",
            "Nearby",
            "Browse"
    };

    private final Class<?>[] mClasses = new Class<?>[] {
            FavoriteAirportsFragment.class,
            NearbyAirportsFragment.class,
            BrowseAirportsFragment.class
    };

    private final int ID_FAVORITES = 0;
    private final int ID_NEARBY = 1;
    private final int ID_BROWSE = 2;

    private HashMap<String, Fragment> mAfdFragments = new HashMap<>();
    private int mCurrentFragmentIndex = -1;

    private ViewPager mViewPager;
    private AfdViewPagerAdapter mViewPagerAdapter;
    private SlidingTabLayout mSlidingTabLayout;

    private static final String AFD_SAVED_TAB = "afdtab";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_tab_pager );

        initFragments( savedInstanceState );

        setActionBarTitle( "Airports", null );

        mViewPager = (ViewPager) findViewById( R.id.view_pager );
        mViewPagerAdapter = new AfdViewPagerAdapter( getSupportFragmentManager() );
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
            mCurrentFragmentIndex = savedInstanceState.getInt( AFD_SAVED_TAB );
        } else {
            mCurrentFragmentIndex = getInitialFragmentId();
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

        outState.putInt( AFD_SAVED_TAB, mViewPager.getCurrentItem() );
    }

    @Override
    public void onFragmentStarted( FragmentBase fragment ) {
        registerActionBarAutoHideListView( ((ListFragmentBase)fragment).getListView() );
        updateContentTopClearance( fragment );
    }

    private void updateContentTopClearance( FragmentBase fragment ) {
        int actionbarClearance = UiUtils.calculateActionBarSize( this );
        int tabbarClearance = getResources().getDimensionPixelSize( R.dimen.tabbar_height );
        fragment.setContentTopClearance( actionbarClearance + tabbarClearance );
    }

    private void initFragments( Bundle savedInstanceState ) {
        if ( savedInstanceState != null ) {
            FragmentManager fm = getSupportFragmentManager();
            // Activity was recreated, check if our fragments survived
            for ( Class<?> clss : mClasses ) {
                // Restore the fragments from state saved earlier
                Fragment fragment = fm.getFragment( savedInstanceState, clss.getName() );
                if ( fragment != null ) {
                    mAfdFragments.put( clss.getName(), fragment );
                }
            }
        }

        Bundle args = getIntent().getExtras();
        for ( Class<?> clss : mClasses ) {
            if ( !mAfdFragments.containsKey( clss.getName() ) ) {
                Fragment fragment = Fragment.instantiate( this, clss.getName(), args );
                mAfdFragments.put( clss.getName(), fragment );
            }
        }
    }

    private ListFragmentBase getCurrentFragment() {
        return getFragmentAtPosition( mCurrentFragmentIndex );
    }

    private ListFragmentBase getFragmentAtPosition( int position ) {
        return (ListFragmentBase) mAfdFragments.get( mClasses[ position ].getName() );
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_AFD;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return getCurrentFragment().canSwipeRefreshChildScrollUp();
    }

    @Override
    protected void requestDataRefresh() {
        getCurrentFragment().requestDataRefresh();
    }

    protected int getInitialFragmentId() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this );
        boolean showNearby = prefs.getBoolean( PreferencesActivity.KEY_ALWAYS_SHOW_NEARBY, false );
        ArrayList<String> fav = getDbManager().getAptFavorites();
        if ( !showNearby && fav.size() > 0 ) {
            return ID_FAVORITES;
        } else {
            return ID_NEARBY;
        }
    }

    private class AfdViewPagerAdapter extends FragmentPagerAdapter {

        public AfdViewPagerAdapter( FragmentManager fm ) {
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
