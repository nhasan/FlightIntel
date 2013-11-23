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
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.BookCategories;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.NavAdapter;
import com.nadmm.airports.utils.TabsAdapter;

public class LibraryMainFragment extends FragmentBase
            implements ActionBar.OnNavigationListener, ViewPager.OnPageChangeListener {

    private TabsAdapter mTabsAdapter;
    private int mFragmentId = -1;
    private ViewPager mViewPager;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflate( R.layout.fragment_pager_no_tab_layout, container );
        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mViewPager = (ViewPager) findViewById( R.id.content_pager );
        mViewPager.setOnPageChangeListener( this );

        mTabsAdapter = new TabsAdapter( getActivity(), getChildFragmentManager(), mViewPager );

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

        // Build the data model for the spinner adapter
        String[] titles = new String[ mTabsAdapter.getCount() ];
        int pos = 0;
        while ( pos < mTabsAdapter.getCount() ) {
            titles[ pos ] = mTabsAdapter.getPageTitle( pos ).toString();
            ++pos;
        }

        // Setup list navigation mode
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_LIST );
        NavAdapter adapter = new NavAdapter( actionBar.getThemedContext(), "Library", titles );
        actionBar.setListNavigationCallbacks( adapter, this );
        actionBar.setDisplayShowTitleEnabled( false );

        if ( savedInstanceState != null ) {
            // Workaround for race conditions in ViewPager
            // See: http://code.google.com/p/android/issues/detail?id=29472
            final int lastPos = savedInstanceState.getInt( "libtab" );
            mViewPager.post( new Runnable() {

                @Override
                public void run() {
                    setCurrentPage( lastPos );
                }
            } );
        }
    }

    @Override
    public void onResume() {
        ActionBar actionBar = getSupportActionBar();
        if ( actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST ) {
            actionBar.setSelectedNavigationItem( mFragmentId );
        }

        super.onResume();
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );
        ViewPager pager = (ViewPager) findViewById( R.id.content_pager );
        outState.putInt( "libtab", pager.getCurrentItem() );
    }

    @Override
    public boolean onNavigationItemSelected( int itemPosition, long itemId ) {
        setCurrentPage( itemPosition );
        return true;
    }

    @Override
    public void onPageScrollStateChanged( int arg0 ) {
    }

    @Override
    public void onPageScrolled( int arg0, float arg1, int arg2 ) {
    }

    @Override
    public void onPageSelected( int position ) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSelectedNavigationItem( position );
    }

    private void setCurrentPage( int pos ) {
        // Workaround for race conditions in ViewPager
        // See: http://code.google.com/p/android/issues/detail?id=29472
        if ( mViewPager.getCurrentItem() != pos ) {
            mViewPager.setCurrentItem( pos );
        }
    }

}
