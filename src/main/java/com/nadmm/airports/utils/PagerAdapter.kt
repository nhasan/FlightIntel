/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.ViewGroup;

import java.util.ArrayList;

public class PagerAdapter extends FragmentPagerAdapter  {

    private Context mContext;
    private ArrayList<TabInfo> mTabs = new ArrayList<>();

    public final class TabInfo {
        private Class<?> clss;
        private Bundle args;
        private String label;
        private Fragment fragment;

        TabInfo( String _label, Class<?> _class, Bundle _args ) {
            clss = _class;
            args = _args;
            label = _label;
            fragment = null;
        }
    }

    public PagerAdapter( Context context, FragmentManager fm, ViewPager pager ) {
        super( fm );
        mContext = context;
        pager.setAdapter( this );
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
    public Object instantiateItem( ViewGroup container, int position ) {
        TabInfo info = mTabs.get( position );
        info.fragment = (Fragment) super.instantiateItem( container, position );
        return info.fragment;
    }

    @Override
    public Fragment getItem( int position ) {
        if ( position < 0 || position >= getCount() ) {
            throw new IllegalArgumentException();
        }

        TabInfo info = mTabs.get( position );
        if ( info.fragment != null ) {
            return info.fragment;
        }
        return Fragment.instantiate( mContext, info.clss.getName(), info.args );
    }

    @Override
    public CharSequence getPageTitle( int position ) {
        if ( position < 0 || position >= getCount() ) {
            throw new IllegalArgumentException();
        }

        return mTabs.get( position ).label;
    }

    public String[] getPageTitles() {
        // Build the data model for the spinner adapter
        String[] titles = new String[ getCount() ];
        int pos = 0;
        while ( pos < getCount() ) {
            titles[ pos ] = getPageTitle( pos ).toString();
            ++pos;
        }
        return titles;
    }

}
