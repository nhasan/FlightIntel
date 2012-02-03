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

package com.nadmm.airports;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.Menu;
import android.support.v4.view.ViewPager;

import com.nadmm.airports.wx.FavoriteWxFragment;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TitleProvider;

public class FavoritesActivity extends ActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_view_pager_layout );

        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );

        TabsAdapter adapter = new TabsAdapter( this, pager );
        adapter.addTab( "AIRPORTS", FavoriteAirportsFragment.class, null );
        adapter.addTab( "WEATHER", FavoriteWxFragment.class, null );

        TabPageIndicator tabIndicator = (TabPageIndicator) findViewById( R.id.page_titles );
        tabIndicator.setViewPager( pager );

        if ( savedInstanceState != null ) {
            pager.setCurrentItem( savedInstanceState.getInt( "favtab" ) );
        }
    }

    @Override
    protected void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        outState.putInt( "favtab", pager.getCurrentItem() );
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        menu.findItem( R.id.menu_favorites ).setEnabled( false );
        return super.onPrepareOptionsMenu( menu );
    }

    public class TabsAdapter extends FragmentPagerAdapter implements
            TitleProvider, ViewPager.OnPageChangeListener {

        private final Context mContext;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;
            private final String label;
            private Fragment fragment;

            TabInfo( String _label, Class<?> _class, Bundle _args ) {
                clss = _class;
                args = _args;
                label = _label;
                fragment = null;
            }
        }

        public TabsAdapter( FragmentActivity activity, ViewPager pager ) {
            super( activity.getSupportFragmentManager() );
            mContext = activity;
            mViewPager = pager;
            mViewPager.setAdapter( this );
        }

        public void addTab( String label, Class<?> clss, Bundle args ) {
            TabInfo info = new TabInfo( label, clss, args );
            mTabs.add( info );
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem( int position ) {
            TabInfo info = mTabs.get( position );
            if ( info.fragment == null ) {
                info.fragment = Fragment.instantiate( mContext, info.clss.getName(), info.args );
            }
            return info.fragment;
        }

        @Override
        public void onPageScrolled( int position, float positionOffset,
                int positionOffsetPixels ) {
        }

        @Override
        public void onPageSelected( int position ) {
            TabInfo info = mTabs.get( 1 );
            FavoriteWxFragment f = (FavoriteWxFragment) info.fragment;
            if ( info.fragment != null ) {
                if ( position == 1 ) {
                    f.startTask();
                } else {
                    f.stopTask();
                }
            }
        }

        @Override
        public void onPageScrollStateChanged( int state ) {
        }

        @Override
        public String getTitle( int position ) {
            TabInfo info = mTabs.get( position );
            return info.label;
        }

    }

}
