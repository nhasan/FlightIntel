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
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nadmm.airports.aeronav.ChartsDownloadActivity;
import com.nadmm.airports.afd.AfdMainActivity;
import com.nadmm.airports.clocks.ClocksActivity;
import com.nadmm.airports.e6b.E6bActivity;
import com.nadmm.airports.library.LibraryActivity;
import com.nadmm.airports.scratchpad.ScratchPadActivity;
import com.nadmm.airports.tfr.TfrListActivity;
import com.nadmm.airports.views.ScrimInsetsScrollView;
import com.nadmm.airports.wx.WxMainActivity;

import java.util.ArrayList;

@SuppressWarnings( "BooleanMethodIsAlwaysInverted" )
public class DrawerActivityBase extends ActivityBase {

    private Intent mIntent = null;
    private Handler mHandler;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ViewGroup mDrawerItemsListContainer;
    // list of navdrawer items that were actually added to the navdrawer, in order
    private ArrayList<Integer> mNavDrawerItems = new ArrayList<>();
    // views that correspond to each navdrawer item, null if not yet created
    private View[] mNavDrawerItemViews = null;

    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private int mNavMode;

    private static final int NAVDRAWER_LAUNCH_DELAY = 250;

    protected static final int NAVDRAWER_ITEM_AFD = 0;
    protected static final int NAVDRAWER_ITEM_WX = 1;
    protected static final int NAVDRAWER_ITEM_TFR = 2;
    protected static final int NAVDRAWER_ITEM_LIBRARY = 3;
    protected static final int NAVDRAWER_ITEM_CHARTS = 4;
    protected static final int NAVDRAWER_ITEM_SCRATCHPAD = 5;
    protected static final int NAVDRAWER_ITEM_CLOCKS = 6;
    protected static final int NAVDRAWER_ITEM_E6B = 7;
    protected static final int NAVDRAWER_ITEM_SETTINGS = 8;
    protected static final int NAVDRAWER_ITEM_INVALID = -1;
    protected static final int NAVDRAWER_ITEM_SEPARATOR = -2;
    protected static final int NAVDRAWER_ITEM_SEPARATOR_SPECIAL = -3;

    // titles for navdrawer items (indices must correspond to the above)
    private static final int[] NAVDRAWER_TITLE_RES_ID = new int[]{
            R.string.navdrawer_item_afd,
            R.string.navdrawer_item_wx,
            R.string.navdrawer_item_tfr,
            R.string.navdrawer_item_library,
            R.string.navdrawer_item_charts,
            R.string.navdrawer_item_scratchpad,
            R.string.navdrawer_item_clocks,
            R.string.navdrawer_item_e6b,
            R.string.navdrawer_item_settings
    };

    // icons for navdrawer items (indices must correspond to above array)
    private static final int[] NAVDRAWER_ICON_RES_ID = new int[]{
            R.drawable.ic_navdrawer_afd,
            R.drawable.ic_navdrawer_wx,
            R.drawable.ic_navdrawer_tfr,
            R.drawable.ic_navdrawer_library,
            R.drawable.ic_navdrawer_charts,
            R.drawable.ic_navdrawer_scratchpad,
            R.drawable.ic_navdrawer_clocks,
            R.drawable.ic_navdrawer_e6b,
            R.drawable.ic_navdrawer_settings
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mHandler = new Handler();
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        setupNavDrawer();
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if ( mDrawerToggle != null ) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void setContentView( int layoutResID ) {
        super.setContentView( layoutResID );
        getActionBarToolbar();
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

    private void setupNavDrawer() {
        // What nav drawer item should be selected?
        int selfItem = getSelfNavDrawerItem();

        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawer_layout );
        if ( mDrawerLayout == null ) {
            return;
        }
        mDrawerLayout.setStatusBarBackgroundColor(
                getResources().getColor( R.color.color_primary_dark ) );

        ScrimInsetsScrollView navDrawer = (ScrimInsetsScrollView)
                mDrawerLayout.findViewById( R.id.navdrawer );
        if ( selfItem == NAVDRAWER_ITEM_INVALID ) {
            // do not show a nav drawer
            if ( navDrawer != null ) {
                ((ViewGroup) navDrawer.getParent()).removeView( navDrawer );
            }
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

        createNavDrawerItems();
    }

    private void onNavDrawerItemClicked( final int itemId ) {
        if ( itemId == getSelfNavDrawerItem() ) {
            mDrawerLayout.closeDrawer( Gravity.START );
            return;
        }

        if ( isSpecialItem( itemId ) ) {
            goToNavDrawerItem( itemId );
        } else {
            // launch the target Activity after a short delay, to allow the close animation to play
            mHandler.postDelayed( new Runnable() {
                @Override
                public void run() {
                    goToNavDrawerItem( itemId );
                }
            }, NAVDRAWER_LAUNCH_DELAY );

            // change the active item on the list so the user can see the item changed
            setSelectedNavDrawerItem( itemId );
        }

        mDrawerLayout.closeDrawer( Gravity.START );
    }

    private void goToNavDrawerItem( int item ) {
        Intent intent;
        switch ( item ) {
            case NAVDRAWER_ITEM_AFD:
                intent = new Intent( this, AfdMainActivity.class );
                startActivity( intent );
                finish();
                break;
            case NAVDRAWER_ITEM_WX:
                intent = new Intent( this, WxMainActivity.class );
                startActivity( intent );
                finish();
                break;
            case NAVDRAWER_ITEM_TFR:
                intent = new Intent( this, TfrListActivity.class );
                startActivity( intent );
                finish();
                break;
            case NAVDRAWER_ITEM_LIBRARY:
                intent = new Intent( this, LibraryActivity.class );
                startActivity( intent );
                finish();
                break;
            case NAVDRAWER_ITEM_SCRATCHPAD:
                intent = new Intent( this, ScratchPadActivity.class );
                startActivity( intent );
                finish();
                break;
            case NAVDRAWER_ITEM_CLOCKS:
                intent = new Intent( this, ClocksActivity.class );
                startActivity( intent );
                finish();
                break;
            case NAVDRAWER_ITEM_E6B:
                intent = new Intent( this, E6bActivity.class );
                startActivity( intent );
                finish();
                break;
            case NAVDRAWER_ITEM_CHARTS:
                intent = new Intent( this, ChartsDownloadActivity.class );
                startActivity( intent );
                finish();
                break;
            case NAVDRAWER_ITEM_SETTINGS:
                intent = new Intent( this, PreferencesActivity.class );
                startActivity( intent );
                break;
        }
    }

    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    private void createNavDrawerItems() {
        mNavDrawerItems.clear();
        mNavDrawerItems.add( NAVDRAWER_ITEM_AFD );
        mNavDrawerItems.add( NAVDRAWER_ITEM_WX );
        mNavDrawerItems.add( NAVDRAWER_ITEM_TFR );
        mNavDrawerItems.add( NAVDRAWER_ITEM_SEPARATOR );
        mNavDrawerItems.add( NAVDRAWER_ITEM_LIBRARY );
        mNavDrawerItems.add( NAVDRAWER_ITEM_CHARTS );
        mNavDrawerItems.add( NAVDRAWER_ITEM_SEPARATOR );
        mNavDrawerItems.add( NAVDRAWER_ITEM_SCRATCHPAD );
        mNavDrawerItems.add( NAVDRAWER_ITEM_CLOCKS );
        mNavDrawerItems.add( NAVDRAWER_ITEM_E6B );
        mNavDrawerItems.add( NAVDRAWER_ITEM_SEPARATOR );
        mNavDrawerItems.add( NAVDRAWER_ITEM_SETTINGS );

        mDrawerItemsListContainer = (ViewGroup) findViewById( R.id.navdrawer_items_list );
        if ( mDrawerItemsListContainer == null ) {
            return;
        }

        mNavDrawerItemViews = new View[ mNavDrawerItems.size() ];
        mDrawerItemsListContainer.removeAllViews();
        int i = 0;
        for ( int itemId : mNavDrawerItems ) {
            mNavDrawerItemViews[ i ] = makeNavDrawerItem( itemId, mDrawerItemsListContainer );
            mDrawerItemsListContainer.addView( mNavDrawerItemViews[ i ] );
            ++i;
        }
    }

    private void setSelectedNavDrawerItem( int itemId ) {
        if ( mNavDrawerItemViews != null ) {
            for ( int i = 0; i < mNavDrawerItemViews.length; i++ ) {
                if ( i < mNavDrawerItems.size() ) {
                    int thisItemId = mNavDrawerItems.get( i );
                    formatNavDrawerItem( mNavDrawerItemViews[ i ],
                            thisItemId, itemId == thisItemId );
                }
            }
        }
    }

    private View makeNavDrawerItem( final int itemId, ViewGroup container ) {
        boolean selected = getSelfNavDrawerItem() == itemId;
        int layoutToInflate = 0;
        if ( itemId == NAVDRAWER_ITEM_SEPARATOR ) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else if ( itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL ) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else {
            layoutToInflate = R.layout.navdrawer_item;
        }
        View view = getLayoutInflater().inflate( layoutToInflate, container, false );

        if ( isSeparator( itemId ) ) {
            return view;
        }

        ImageView iconView = (ImageView) view.findViewById( R.id.item_icon );
        TextView titleView = (TextView) view.findViewById( R.id.item_title );
        int iconId = itemId >= 0 && itemId < NAVDRAWER_ICON_RES_ID.length ?
                NAVDRAWER_ICON_RES_ID[ itemId ] : 0;
        int titleId = itemId >= 0 && itemId < NAVDRAWER_TITLE_RES_ID.length ?
                NAVDRAWER_TITLE_RES_ID[ itemId ] : 0;

        // set icon and text
        iconView.setVisibility( iconId > 0 ? View.VISIBLE : View.GONE );
        if ( iconId > 0 ) {
            iconView.setImageResource( iconId );
        }
        titleView.setText( getString( titleId ) );

        formatNavDrawerItem( view, itemId, selected );

        view.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                onNavDrawerItemClicked( itemId );
            }
        } );

        return view;
    }

    private boolean isSpecialItem( int itemId ) {
        return itemId == NAVDRAWER_ITEM_SETTINGS;
    }

    private boolean isSeparator( int itemId ) {
        return itemId == NAVDRAWER_ITEM_SEPARATOR || itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL;
    }

    private void formatNavDrawerItem( View view, int itemId, boolean selected ) {
        if ( isSeparator( itemId ) ) {
            // not applicable
            return;
        }

        ImageView iconView = (ImageView) view.findViewById( R.id.item_icon );
        TextView titleView = (TextView) view.findViewById( R.id.item_title );

        if ( selected ) {
            view.setBackgroundResource( R.drawable.selected_navdrawer_item_background );
        }

        // configure its appearance according to whether or not it's selected
        titleView.setTextColor( selected ?
                getResources().getColor( R.color.navdrawer_text_color_selected ) :
                getResources().getColor( R.color.navdrawer_text_color ) );
        iconView.setColorFilter( selected ?
                getResources().getColor( R.color.navdrawer_icon_tint_selected ) :
                getResources().getColor( R.color.navdrawer_icon_tint ) );
    }

    public boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen( Gravity.START );
    }

    public void setDrawerIndicatorEnabled( boolean enable ) {
        if ( mDrawerToggle != null ) {
            mDrawerToggle.setDrawerIndicatorEnabled( enable );
        }
    }

}
