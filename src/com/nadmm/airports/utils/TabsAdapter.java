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

package com.nadmm.airports.utils;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

public class TabsAdapter extends FragmentPagerAdapter  {

    private final Context mContext;
    private final ViewPager mViewPager;
    private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

    public final class TabInfo {
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

    public TabsAdapter( Context context, FragmentManager fm, ViewPager pager ) {
        super( fm );
        mContext = context;
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
    public CharSequence getPageTitle( int position ) {
        return mTabs.get( position ).label;
    }

}
