/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2013 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.library;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.BookCategories;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.TabsAdapter;

public class LibraryMainFragment extends FragmentBase {

    private TabsAdapter mTabsAdapter;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflate( R.layout.fragment_pager_layout, container );
        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        mTabsAdapter = new TabsAdapter( getActivity(), getChildFragmentManager(), pager );

        PagerTabStrip tabs = (PagerTabStrip) findViewById( R.id.pager_tabs );
        tabs.setTabIndicatorColor( getResources().getColor( R.color.tab_indicator ) );

        SQLiteDatabase db = getDatabase( DatabaseManager.DB_LIBRARY );
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( BookCategories.TABLE_NAME );
        Cursor c = builder.query( db, new String[] { "*" }, null, null, null, null,
                BookCategories._ID );
        if ( c.moveToFirst() ) {
            do {
                String code = c.getString( c.getColumnIndex( BookCategories.CATEGORY_CODE ) );
                String name = c.getString( c.getColumnIndex( BookCategories.CATEGORY_NAME ) );
                Bundle args = new Bundle();
                args.putString( BookCategories.CATEGORY_CODE, code );
                mTabsAdapter.addTab( name, LibraryPageFragment.class, args );
            } while ( c.moveToNext() );
        }

        if ( savedInstanceState != null ) {
            pager.setCurrentItem( savedInstanceState.getInt( "libtab" ) );
        }
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        outState.putInt( "libtab", pager.getCurrentItem() );
    }

}
