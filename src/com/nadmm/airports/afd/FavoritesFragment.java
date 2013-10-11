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

import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nadmm.airports.R;
import com.nadmm.airports.utils.TabsAdapter;
import com.nadmm.airports.wx.FavoriteWxFragment;

public final class FavoritesFragment extends AfdFragmentBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.fragment_pager_layout, container, false );
        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );

        TabsAdapter adapter = new TabsAdapter( getActivity(), getChildFragmentManager(), pager );
        adapter.addTab( "AIRPORTS", FavoriteAirportsFragment.class, null );
        adapter.addTab( "WEATHER", FavoriteWxFragment.class, null );

        PagerTabStrip tabs = (PagerTabStrip) findViewById( R.id.pager_tabs );
        tabs.setTabIndicatorColor( getResources().getColor( R.color.tab_indicator ) );

        if ( savedInstanceState != null ) {
            pager.setCurrentItem( savedInstanceState.getInt( "favtab" ) );
        }
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        if ( pager != null ) {
            outState.putInt( "favtab", pager.getCurrentItem() );
        }
    }

    @Override
    protected int getActivityId() {
        return ID_FAVORITES;
    }

}
