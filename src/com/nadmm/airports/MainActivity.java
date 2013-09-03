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

package com.nadmm.airports;

import java.util.ArrayList;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.nadmm.airports.afd.FavoritesFragment;
import com.nadmm.airports.afd.NearbyFragment;
import com.nadmm.airports.views.DrawerListView;

public class MainActivity extends ActivityBase implements ListView.OnItemClickListener {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerListView mDrawerList;
    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private CharSequence mDrawerTitle;

    private long mPrevId = -1;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );

        mTitle = mDrawerTitle = getTitle();

        mDrawerList = (DrawerListView) findViewById( R.id.left_drawer );
        mDrawerList.setOnItemClickListener( this );

        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawer_layout );
        mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed( View view ) {
                getSupportActionBar().setTitle( mTitle );
                getSupportActionBar().setSubtitle( mSubtitle );
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened( View drawerView ) {
                mTitle = getSupportActionBar().getTitle();
                mSubtitle = getSupportActionBar().getSubtitle();
                getSupportActionBar().setTitle( mDrawerTitle );
                getSupportActionBar().setSubtitle( null );
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener( mDrawerToggle );
        setFragment();
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
        mDrawerList.setItemChecked( DrawerListView.ITEM_ID_AFD, true );
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged( newConfig );
        mDrawerToggle.onConfigurationChanged( newConfig );
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen( mDrawerList );
        menu.findItem( R.id.menu_refresh ).setVisible( !drawerOpen );
        return super.onPrepareOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if ( mDrawerToggle.onOptionsItemSelected( item ) ) {
          return true;
        }

        return super.onOptionsItemSelected( item );
    }

    @Override
    public void setTitle( CharSequence title ) {
        mTitle = title;
        getSupportActionBar().setTitle( mTitle );
    }

    protected void setFragment() {
        Class<?> clss = getHomeFragmentClass();
        addFragment( clss, null );
    }

    public void setDrawerIndicatorEnabled( boolean enable ) {
        if ( mDrawerToggle != null ) {
            mDrawerToggle.setDrawerIndicatorEnabled( enable );
        }
    }

    protected void updateContent( int position ) {
        // Create a new fragment and specify the planet to show based on position
        long id = mDrawerList.getAdapter().getItemId( position );
        if ( mPrevId != id ) {
            Class<?> clss = null;
            if ( id == DrawerListView.ITEM_ID_AFD ) {
                clss = getHomeFragmentClass();
            } else if ( id == DrawerListView.ITEM_ID_TFR ) {
            } else if ( id == DrawerListView.ITEM_ID_LIBRARY ) {
            } else if ( id == DrawerListView.ITEM_ID_SCRATCHPAD ) {
            } else if ( id == DrawerListView.ITEM_ID_CHARTS ) {
            } else if ( id == DrawerListView.ITEM_ID_CLOCKS ) {
            } else if ( id == DrawerListView.ITEM_ID_E6B ) {
            }

            if ( clss != null ) {
                replaceFragment( clss, null );
            }
            mPrevId = id;
            mDrawerList.setItemChecked( position, true );
        }

        mDrawerLayout.closeDrawer( mDrawerList );
    }

    protected Class<?> getHomeFragmentClass() {
        ArrayList<String> fav = getDbManager().getAptFavorites();
        Class<?> clss = null;
        if ( fav.size() > 0 ) {
            clss = FavoritesFragment.class;
        } else {
            clss = NearbyFragment.class;
        }
        return clss;
    }

    @Override
    public void onItemClick( AdapterView<?> arg0, View arg1, int position, long id ) {
        updateContent( position );
    }

}
