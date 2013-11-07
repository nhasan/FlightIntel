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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.nadmm.airports.aeronav.ChartsDownloadActivity;
import com.nadmm.airports.afd.AfdMainActivity;
import com.nadmm.airports.clocks.ClocksActivity;
import com.nadmm.airports.e6b.E6bActivity;
import com.nadmm.airports.library.LibraryActivity;
import com.nadmm.airports.scratchpad.ScratchPadActivity;
import com.nadmm.airports.tfr.TfrListActivity;
import com.nadmm.airports.views.DrawerListView;
import com.nadmm.airports.wx.WxMainActivity;

public class DrawerActivity extends ActivityBase implements ListView.OnItemClickListener {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerListView mDrawerList;
    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private Intent mIntent = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView();

        mDrawerList = (DrawerListView) findViewById( R.id.left_drawer );
        mDrawerList.setOnItemClickListener( this );

        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawer_layout );
        mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed( View view ) {
                if ( mIntent != null ) {
                    mIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
                    mIntent.setFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP );
                    startActivity( mIntent );
                    mIntent = null;
                } else {
                    getSupportActionBar().setTitle( mTitle );
                    getSupportActionBar().setSubtitle( mSubtitle );
                    supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened( View drawerView ) {
                mTitle = getSupportActionBar().getTitle();
                mSubtitle = getSupportActionBar().getSubtitle();
                getSupportActionBar().setTitle( R.string.app_name );
                getSupportActionBar().setSubtitle( null );
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener( mDrawerToggle );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
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

    protected void setContentView() {
        setContentView( R.layout.activity_main );
    }

    protected void setDrawerItemChecked( int position ) {
        mDrawerList.setItemChecked( position, true );
    }

    public void setDrawerIndicatorEnabled( boolean enable ) {
        if ( mDrawerToggle != null ) {
            mDrawerToggle.setDrawerIndicatorEnabled( enable );
        }
    }

    @Override
    public void onItemClick( AdapterView<?> arg0, View arg1, int position, long id ) {
        Intent intent = null;

        if ( id == DrawerListView.ITEM_ID_AFD ) {
            intent = new Intent( this, AfdMainActivity.class );
        } else if ( id == DrawerListView.ITEM_ID_WX ) {
            intent = new Intent( this, WxMainActivity.class );
        } else if ( id == DrawerListView.ITEM_ID_TFR ) {
            intent = new Intent( this, TfrListActivity.class );
        } else if ( id == DrawerListView.ITEM_ID_LIBRARY ) {
            intent = new Intent( this, LibraryActivity.class );
        } else if ( id == DrawerListView.ITEM_ID_SCRATCHPAD ) {
            intent = new Intent( this, ScratchPadActivity.class );
        } else if ( id == DrawerListView.ITEM_ID_CHARTS ) {
            intent = new Intent( this, ChartsDownloadActivity.class );
        } else if ( id == DrawerListView.ITEM_ID_CLOCKS ) {
            intent = new Intent( this, ClocksActivity.class );
        } else if ( id == DrawerListView.ITEM_ID_E6B ) {
            intent = new Intent( this, E6bActivity.class );
        }

        if ( intent != null ) {
            mIntent = intent;
        }
        mDrawerLayout.closeDrawer( mDrawerList );
    }

}
