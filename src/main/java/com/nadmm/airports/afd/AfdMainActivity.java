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
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;
import android.view.ViewGroup;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.views.SlidingTabLayout;

import java.util.ArrayList;
import java.util.HashMap;

public final class AfdMainActivity extends ActivityBase
        implements AirportListFragment.Listener {

    private final String[] mOptions = new String[] {
            "Favorites",
            "Nearby",
            "Browse"
    };

    private final Class<?>[] mClasses = new Class<?>[] {
            FavoriteAirportsFragment.class,
            NearbyAirportsFragment.class,
            BrowseStateFragment.class
    };

    private final int ID_FAVORITES = 0;
    private final int ID_NEARBY = 1;
    private final int ID_BROWSE = 2;

    private HashMap<String, ListFragmentBase> mAirportFragments = new HashMap<>();
    private int mSelectedFragment;

    ViewPager mViewPager = null;
    AfdViewPagerAdapter mViewPagerAdapter = null;
    SlidingTabLayout mSlidingTabLayout = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Resources res = getResources();

        setContentView( R.layout.activity_tab_pager );

        mViewPager = (ViewPager) findViewById( R.id.view_pager );
        mViewPagerAdapter = new AfdViewPagerAdapter( getSupportFragmentManager() );
        mViewPager.setAdapter( mViewPagerAdapter );

        mSlidingTabLayout = (SlidingTabLayout) findViewById( R.id.sliding_tabs );
        mSlidingTabLayout.setCustomTabView( R.layout.tab_indicator, android.R.id.text1 );
        mSlidingTabLayout.setSelectedIndicatorColors( res.getColor( R.color.tab_selected_strip ) );
        mSlidingTabLayout.setDistributeEvenly( true );
        mSlidingTabLayout.setViewPager( mViewPager );

        mSlidingTabLayout.setOnPageChangeListener( new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled( int position, float v, int i1 ) {
            }

            @Override
            public void onPageSelected( int position ) {
                mSelectedFragment = position;
                enableDisableSwipeRefresh( position == ID_NEARBY );
            }

            @Override
            public void onPageScrollStateChanged( int state ) {
                enableDisableSwipeRefresh( state == ViewPager.SCROLL_STATE_IDLE
                    && mSelectedFragment == ID_NEARBY );
            }
        } );
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate( savedInstanceState );
        int mSelectedFragment = getInitialFragmentId();
        mViewPager.setCurrentItem( mSelectedFragment );
        enableDisableSwipeRefresh( mSelectedFragment == ID_NEARBY );
        setProgressBarTopWhenActionBarShown( (int)
                TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 2,
                        getResources().getDisplayMetrics() ) );
    }

    @Override
    public void onFragmentViewCreated( AirportListFragment fragment ) {
    }

    @Override
    public void onFragmentAttached( AirportListFragment fragment ) {
    }

    @Override
    public void onFragmentDetached( AirportListFragment fragment) {
    }

    private class AfdViewPagerAdapter extends FragmentPagerAdapter {

        public AfdViewPagerAdapter( FragmentManager fm ) {
            super( fm );
        }

        @Override
        public Fragment getItem( int position ) {
            return Fragment.instantiate( AfdMainActivity.this,
                    mClasses[ position ].getName(), null );
        }

        @Override
        public Object instantiateItem( ViewGroup container, int position ) {
            ListFragmentBase fragment = (ListFragmentBase) super.instantiateItem(
                    container, position );
            mAirportFragments.put( fragment.getClass().getName(), fragment );
            return fragment;
        }

        @Override
        public int getCount() {
            return mOptions.length;
        }

        @Override
        public CharSequence getPageTitle( int position ) {
            return mOptions[ position ];
        }
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_AFD;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        String name = mClasses[ mSelectedFragment ].getName();
        ListFragmentBase fragment = mAirportFragments.get( name );
        return ViewCompat.canScrollVertically( fragment.getListView(), -1 );
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

}
