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

package com.nadmm.airports;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

public class DrawerActivityBase extends ActivityBase implements ListView.OnItemClickListener {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerListView mNavDrawer;
    private Intent mIntent = null;

    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private int mNavMode;

    protected static final int NAVDRAWER_ITEM_AFD = 0;
    protected static final int NAVDRAWER_ITEM_WX = 1;
    protected static final int NAVDRAWER_ITEM_TFR = 2;
    protected static final int NAVDRAWER_ITEM_LIBRARY = 3;
    public static final int NAVDRAWER_ITEM_SCRATCHPAD = 4;
    public static final int NAVDRAWER_ITEM_CLOCKS = 5;
    public static final int NAVDRAWER_ITEM_E6B = 6;
    public static final int NAVDRAWER_ITEM_CHARTS = 7;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawer_layout );
        if ( mDrawerLayout == null ) {
            return;
        }
        mDrawerLayout.setStatusBarBackgroundColor(
                getResources().getColor( R.color.color_primary_dark ) );

        mNavDrawer = (DrawerListView) findViewById( R.id.left_drawer );
        mNavDrawer.setOnItemClickListener( this );

        if ( !showNavDrawer() ) {
            (( ViewGroup) mNavDrawer.getParent()).removeView( mNavDrawer );
            mDrawerLayout = null;
            return;
        }

        mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout,
                getActionBarToolbar(), R.string.drawer_open, R.string.drawer_close ) {

            public void onDrawerClosed( View view ) {
                supportInvalidateOptionsMenu();

                if ( mIntent != null ) {
                    mIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
                    mIntent.setFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP );
                    startActivity( mIntent );
                    mIntent = null;
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened( View drawerView ) {
                ActionBar actionBar = getSupportActionBar();
                mTitle = actionBar.getTitle();
                mSubtitle = actionBar.getSubtitle();
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener( mDrawerToggle );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if ( mDrawerToggle != null ) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged( Configuration newConfig ) {
        super.onConfigurationChanged( newConfig );
        if ( mDrawerToggle != null ) {
            mDrawerToggle.onConfigurationChanged( newConfig );
        }
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if ( mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected( item ) ) {
          return true;
        }

        return super.onOptionsItemSelected( item );
    }

    @Override
    public void setTitle( CharSequence title ) {
        mTitle = title;
        getSupportActionBar().setTitle( mTitle );
    }

    protected boolean showNavDrawer() {
        return true;
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen( mNavDrawer );
    }

    protected void setDrawerItemChecked( int position ) {
        if ( mDrawerLayout != null ) {
            mNavDrawer.setItemChecked( position, true );
        }
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

        mIntent = intent;

        if ( mDrawerLayout != null ) {
            mDrawerLayout.closeDrawer( mNavDrawer );
        }
    }

}
