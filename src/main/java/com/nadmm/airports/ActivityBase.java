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

import android.animation.ArgbEvaluator;
import android.animation.TypeEvaluator;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.aeronav.ChartsDownloadActivity;
import com.nadmm.airports.afd.AfdMainActivity;
import com.nadmm.airports.clocks.ClocksActivity;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.Catalog;
import com.nadmm.airports.data.DatabaseManager.Nav1;
import com.nadmm.airports.data.DatabaseManager.States;
import com.nadmm.airports.data.DownloadActivity;
import com.nadmm.airports.donate.DonateActivity;
import com.nadmm.airports.donate.DonateDatabase;
import com.nadmm.airports.e6b.E6bActivity;
import com.nadmm.airports.library.LibraryActivity;
import com.nadmm.airports.scratchpad.ScratchPadActivity;
import com.nadmm.airports.tfr.TfrListActivity;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.ExternalStorageActivity;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.views.MultiSwipeRefreshLayout;
import com.nadmm.airports.views.ObservableScrollView;
import com.nadmm.airports.views.ScrimInsetsScrollView;
import com.nadmm.airports.wx.WxMainActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;

public class ActivityBase extends AppCompatActivity implements
        MultiSwipeRefreshLayout.CanChildScrollUpCallback  {

    private DatabaseManager mDbManager;
    private LayoutInflater mInflater;
    private CursorAsyncTask mTask;

    private IntentFilter mFilter;
    private BroadcastReceiver mExternalStorageReceiver;

    private Toolbar mActionBarToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // variables that control the Action Bar auto hide behavior (aka "quick recall")
    private boolean mActionBarAutoHideEnabled = false;
    private int mActionBarAutoHideSensivity = 0;
    private int mActionBarAutoHideMinY = 0;
    private int mActionBarAutoHideSignal = 0;
    private boolean mActionBarShown = true;

    private Intent mIntent = null;
    private Handler mHandler;

    private int mThemedStatusBarColor;
    private int mNormalStatusBarColor;
    private int mProgressBarTopWhenActionBarShown;
    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ViewGroup mDrawerItemsListContainer;
    // list of navdrawer items that were actually added to the navdrawer, in order
    private ArrayList<Integer> mNavDrawerItems = new ArrayList<>();
    // views that correspond to each navdrawer item, null if not yet created
    private View[] mNavDrawerItemViews = null;

    // When set, these components will be shown/hidden in sync with the action bar
    // to implement the "quick recall" effect (the Action Bar and the header views disappear
    // when you scroll down a list, and reappear quickly when you scroll up).
    private ArrayList<View> mHideableHeaderViews = new ArrayList<>();

    private static final int HEADER_HIDE_ANIM_DURATION = 300;
    private static final int NAVDRAWER_LAUNCH_DELAY = 300;

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

    private FragmentManager.OnBackStackChangedListener mBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                @Override
                public void onBackStackChanged() {
                    updateDrawerToggle();
                }
            };

    public static final String FRAGMENT_TAG_EXTRA = "FRAGMENT_TAG_EXTRA";

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        mDbManager = DatabaseManager.instance( this );
        mInflater = getLayoutInflater();
        overridePendingTransition( R.anim.fade_in, R.anim.fade_out );

        mHandler = new Handler();

        mFilter = new IntentFilter();
        mFilter.addAction( Intent.ACTION_MEDIA_MOUNTED );
        mFilter.addAction( Intent.ACTION_MEDIA_SHARED );
        mFilter.addAction( Intent.ACTION_MEDIA_UNMOUNTED );
        mFilter.addAction( Intent.ACTION_MEDIA_REMOVED );
        mFilter.addDataScheme( "file" );

        mExternalStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive( Context context, Intent intent ) {
                externalStorageStatusChanged();
            }
        };

        if ( Application.sDonationDone == null ) {
            DonateDatabase db = new DonateDatabase( this );
            Cursor c = db.queryAllDonations();
            Application.sDonationDone = c.moveToFirst();
            db.close();
        }

        mThemedStatusBarColor = getResources().getColor( R.color.color_primary_dark );
        mNormalStatusBarColor = mThemedStatusBarColor;

        // Enable Google Analytics
        ( (Application) getApplication() ).getAnalyticsTracker();
    }

    @Override
    protected void onPause() {
        if ( mTask != null ) {
            mTask.cancel( true );
        }
        overridePendingTransition( R.anim.fade_in, R.anim.fade_out );
        unregisterReceiver( mExternalStorageReceiver );
        getSupportFragmentManager().removeOnBackStackChangedListener( mBackStackChangedListener );
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver( mExternalStorageReceiver, mFilter );

        // Whenever the fragment back stack changes, we may need to update the
        // action bar toggle: only top level screens show the hamburger-like icon, inner
        // screens - either Activities or fragments - show the "Up" icon instead.
        getSupportFragmentManager().addOnBackStackChangedListener( mBackStackChangedListener );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        setupNavDrawer();
        trySetupSwipeRefresh();
        updateSwipeRefreshProgressBarTop();

        enableDisableSwipeRefresh( false );
        enableActionBarAutoHide();

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
    public void onBackPressed() {
        // If the drawer is open, back will close it
        if ( mDrawerLayout != null && mDrawerLayout.isDrawerOpen( GravityCompat.START ) ) {
            mDrawerLayout.closeDrawers();
            return;
        }
        // Otherwise, it may return to the previous fragment stack
        FragmentManager fragmentManager = getSupportFragmentManager();
        if ( fragmentManager.getBackStackEntryCount() > 0 ) {
            fragmentManager.popBackStack();
        } else {
            // Lastly, it will rely on the system behavior for back
            super.onBackPressed();
        }
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
                onNavDrawerStateChanged( false, false );
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened( View drawerView ) {
                onNavDrawerStateChanged( true, false );
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerToggle.setToolbarNavigationClickListener( new View.OnClickListener() {

            @Override
            public void onClick( View v ) {
                onBackPressed();
            }
        } );
        mDrawerLayout.setDrawerListener( mDrawerToggle );

        createNavDrawerItems();

        updateDrawerToggle();
    }

    protected void updateDrawerToggle() {
        if ( mDrawerToggle == null ) {
            return;
        }
        boolean isRoot = getSupportFragmentManager().getBackStackEntryCount() == 0;
        mDrawerToggle.setDrawerIndicatorEnabled( isRoot );
        mDrawerLayout.setDrawerLockMode(
                isRoot ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED );
        ActionBar actionBar = getSupportActionBar();
        if ( actionBar != null ) {
            actionBar.setDisplayShowHomeEnabled( !isRoot );
            actionBar.setDisplayHomeAsUpEnabled( !isRoot );
            actionBar.setHomeButtonEnabled( !isRoot );
        }
        if ( isRoot ) {
            mDrawerToggle.syncState();
        }
    }

    private void createNavDrawerItems() {
        mNavDrawerItems.clear();
        mNavDrawerItems.add( NAVDRAWER_ITEM_AFD );
        mNavDrawerItems.add( NAVDRAWER_ITEM_WX );
        mNavDrawerItems.add( NAVDRAWER_ITEM_TFR );
        mNavDrawerItems.add( NAVDRAWER_ITEM_LIBRARY );
        mNavDrawerItems.add( NAVDRAWER_ITEM_CHARTS );
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

    private void onNavDrawerItemClicked( final int itemId ) {
        if ( itemId == getSelfNavDrawerItem() ) {
            mDrawerLayout.closeDrawer( GravityCompat.START );
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

        mDrawerLayout.closeDrawer( GravityCompat.START );
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

    private View makeNavDrawerItem( final int itemId, ViewGroup container ) {
        boolean selected = getSelfNavDrawerItem() == itemId;
        int layoutToInflate;
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
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen( GravityCompat.START );
    }

    public void setDrawerIndicatorEnabled( boolean enable ) {
        if ( mDrawerToggle != null ) {
            mDrawerToggle.setDrawerIndicatorEnabled( enable );
        }
    }

    protected Toolbar getActionBarToolbar() {
        if ( mActionBarToolbar == null ) {
            mActionBarToolbar = (Toolbar) findViewById( R.id.toolbar_actionbar );
            if ( mActionBarToolbar != null ) {
                setSupportActionBar( mActionBarToolbar );
                ActionBar actionBar = getSupportActionBar();
                if ( actionBar != null ) {
                    actionBar.setHomeButtonEnabled( true );
                    actionBar.setDisplayHomeAsUpEnabled( true );
                }
            }
        }
        return mActionBarToolbar;
    }

    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    private void trySetupSwipeRefresh() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById( R.id.swipe_refresh_layout );
        if ( mSwipeRefreshLayout != null ) {
            mSwipeRefreshLayout.setColorSchemeResources(
                    R.color.refresh_progress_1,
                    R.color.refresh_progress_2,
                    R.color.refresh_progress_3 );
            mSwipeRefreshLayout.setOnRefreshListener( new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    requestDataRefresh();
                }
            } );

            if ( mSwipeRefreshLayout instanceof MultiSwipeRefreshLayout ) {
                MultiSwipeRefreshLayout mswrl = (MultiSwipeRefreshLayout) mSwipeRefreshLayout;
                mswrl.setCanChildScrollUpCallback( this );
            }
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return false;
    }

    protected void setProgressBarTopWhenActionBarShown( int progressBarTopWhenActionBarShown ) {
        mProgressBarTopWhenActionBarShown = progressBarTopWhenActionBarShown;
        updateSwipeRefreshProgressBarTop();
    }

    private void updateSwipeRefreshProgressBarTop() {
        if ( mSwipeRefreshLayout == null ) {
            return;
        }

        int progressBarStartMargin = getResources().getDimensionPixelSize(
                R.dimen.swipe_refresh_progress_bar_start_margin );
        int progressBarEndMargin = getResources().getDimensionPixelSize(
                R.dimen.swipe_refresh_progress_bar_end_margin );
        int top = mActionBarShown ? mProgressBarTopWhenActionBarShown : 0;
        mSwipeRefreshLayout.setProgressViewOffset( false,
                top + progressBarStartMargin, top + progressBarEndMargin );
    }

    public void setRefreshing( boolean refreshing ) {
        if ( mSwipeRefreshLayout != null ) {
            mSwipeRefreshLayout.setRefreshing( refreshing );
        }
    }

    public boolean isRefreshing() {
        return mSwipeRefreshLayout.isRefreshing();
    }

    public void enableDisableSwipeRefresh( boolean enable ) {
        if ( mSwipeRefreshLayout != null ) {
            mSwipeRefreshLayout.setEnabled( enable );
        }
    }

    protected void requestDataRefresh() {
    }

    public void onFragmentStarted( FragmentBase fragment ) {
        // Action bar may be hidden when this fragment was attached so make sure it is visible
        autoShowOrHideActionBar( true );
        fragment.registerActionbarAutoHideView();

        updateContentTopClearance( fragment );
    }

    private void updateContentTopClearance( FragmentBase fragment ) {
        int topClearance = UiUtils.calculateActionBarSize( this );
        if ( findViewById( R.id.sliding_tabs ) != null ) {
            int tabbarClearance = getResources().getDimensionPixelSize( R.dimen.tabbar_height );
            topClearance += tabbarClearance;
        }
        fragment.setContentTopClearance( topClearance );
        setProgressBarTopWhenActionBarShown( topClearance );
    }

    public void registerActionBarAutoHideListView( final ListView listView ) {
        listView.setOnScrollListener( new AbsListView.OnScrollListener() {
            final int ITEMS_THRESHOLD = 2;
            int lastFvi = 0;

            @Override
            public void onScrollStateChanged( AbsListView view, int scrollState ) {
            }

            @Override
            public void onScroll( AbsListView view, int firstVisibleItem, int visibleItemCount,
                                  int totalItemCount ) {
                onMainContentScrolled( firstVisibleItem <= ITEMS_THRESHOLD ? 0 : Integer.MAX_VALUE,
                        lastFvi - firstVisibleItem > 0 ? Integer.MIN_VALUE :
                                lastFvi == firstVisibleItem ? 0 : Integer.MAX_VALUE );
                lastFvi = firstVisibleItem;
            }
        } );
    }

    public void registerActionBarAutoHideScrollView( final ObservableScrollView scrollView ) {
        scrollView.setCallbacks( new ObservableScrollView.Callbacks() {
            final int ITEM_SIZE = UiUtils.convertDpToPx( ActivityBase.this, 56 );
            final int ITEMS_THRESHOLD = 2;
            int lastFvi = 0;

            @Override
            public void onScrollChanged( int l, int t, int oldl, int oldt ) {
                int firstVisibleItem = t / ITEM_SIZE;
                onMainContentScrolled( firstVisibleItem <= ITEMS_THRESHOLD ? 0 : Integer.MAX_VALUE,
                        lastFvi - firstVisibleItem > 0 ? Integer.MIN_VALUE :
                                lastFvi == firstVisibleItem ? 0 : Integer.MAX_VALUE );
                lastFvi = firstVisibleItem;
            }
        } );
    }

    protected void registerHideableHeaderView( View hideableHeaderView ) {
        registerHideableHeaderView( hideableHeaderView, hideableHeaderView.getBottom() );
    }

    protected void registerHideableHeaderView( View hideableHeaderView, int offset ) {
        if ( !mHideableHeaderViews.contains( hideableHeaderView ) ) {
            hideableHeaderView.setTag( R.id.AUTOHIDE_OFFSET, offset );
            mHideableHeaderViews.add( hideableHeaderView );
        }
    }

    protected void autoShowOrHideActionBar( boolean show ) {
        if ( show == mActionBarShown ) {
            return;
        }

        mActionBarShown = show;
        onActionBarAutoShowOrHide( show );
    }

    /**
     * Initializes the Action Bar auto-hide (aka Quick Recall) effect.
     */
    private void initActionBarAutoHide() {
        mActionBarAutoHideEnabled = true;
        mActionBarAutoHideMinY = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_min_y);
        mActionBarAutoHideSensivity = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_sensivity);
    }

    protected void enableActionBarAutoHide() {
        enableActionBarAutoHide( 0 );
    }
    protected void enableActionBarAutoHide( int extraOffset ) {
        View header = findViewById( R.id.headerbar );
        if ( header != null ) {
            registerHideableHeaderView( header, UiUtils.calculateActionBarSize( this )+extraOffset );
            initActionBarAutoHide();
        }
    }

    /**
     * Indicates that the main content has scrolled (for the purposes of showing/hiding
     * the action bar for the "action bar auto hide" effect). currentY and deltaY may be exact
     * (if the underlying view supports it) or may be approximate indications:
     * deltaY may be INT_MAX to mean "scrolled forward indeterminately" and INT_MIN to mean
     * "scrolled backward indeterminately".  currentY may be 0 to mean "somewhere close to the
     * start of the list" and INT_MAX to mean "we don't know, but not at the start of the list"
     */
    private void onMainContentScrolled(int currentY, int deltaY) {
        if (deltaY > mActionBarAutoHideSensivity) {
            deltaY = mActionBarAutoHideSensivity;
        } else if (deltaY < -mActionBarAutoHideSensivity) {
            deltaY = -mActionBarAutoHideSensivity;
        }

        if (Math.signum(deltaY) * Math.signum(mActionBarAutoHideSignal) < 0) {
            // deltaY is a motion opposite to the accumulated signal, so reset signal
            mActionBarAutoHideSignal = deltaY;
        } else {
            // add to accumulated signal
            mActionBarAutoHideSignal += deltaY;
        }

        boolean shouldShow = currentY < mActionBarAutoHideMinY ||
                (mActionBarAutoHideSignal <= -mActionBarAutoHideSensivity );
        autoShowOrHideActionBar( shouldShow );
    }

    protected void resetActionBarAutoHide() {
        mActionBarAutoHideSignal = 0;
    }

    protected void onActionBarAutoShowOrHide( boolean shown ) {
        for ( View view : mHideableHeaderViews ) {
            if ( shown ) {
                view.animate()
                        .translationY( 0 )
                        .setDuration( HEADER_HIDE_ANIM_DURATION )
                        .setInterpolator( new DecelerateInterpolator() );
            } else {
                int offset = (int)view.getTag( R.id.AUTOHIDE_OFFSET );
                view.animate()
                        .translationY( -offset )
                        .setDuration( HEADER_HIDE_ANIM_DURATION )
                        .setInterpolator( new DecelerateInterpolator() );
            }
        }
    }

    // Subclasses can override this for custom behavior
    protected void onNavDrawerStateChanged( boolean isOpen, boolean isAnimating ) {
        if ( mActionBarAutoHideEnabled && isOpen ) {
            autoShowOrHideActionBar( true );
        }
    }

    protected CursorAsyncTask setBackgroundTask( CursorAsyncTask task ) {
        mTask = task;
        return mTask;
    }

    protected void externalStorageStatusChanged() {
        if ( !SystemUtils.isExternalStorageAvailable() ) {
            Intent intent = new Intent( this, ExternalStorageActivity.class );
            startActivity( intent );
        }
    }

    public DatabaseManager getDbManager() {
        return mDbManager;
    }

    protected View createContentView( int id ) {
        View view = inflate( id );
        return createContentView( view );
    }

    public View createContentView( View view ) {
        FrameLayout root = new FrameLayout( this );
        int white = getResources().getColor( android.R.color.white );
        root.setBackgroundColor( white );
        root.setDrawingCacheBackgroundColor( white );
        root.setLayoutParams( new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );

        LinearLayout pframe = new LinearLayout( this );
        pframe.setId( R.id.INTERNAL_PROGRESS_CONTAINER_ID );
        pframe.setGravity( Gravity.CENTER );

        ProgressBar progress = new ProgressBar( this, null, android.R.attr.progressBarStyleLarge );
        pframe.addView( progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
        root.addView( pframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );

        FrameLayout lframe = new FrameLayout( this );
        lframe.setId( R.id.INTERNAL_FRAGMENT_CONTAINER_ID );
        lframe.setVisibility( View.GONE );

        lframe.addView( view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );
        root.addView( lframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );

        return root;
    }

    public void setContentShown( boolean shown ) {
        View root = findViewById( android.R.id.content );
        setContentShown( root, shown, true );
    }

    public void setContentShown( View view, boolean shown ) {
        setContentShown( view, shown, true );
    }

    public void setContentShownNoAnimation( boolean shown ) {
        View root = findViewById( android.R.id.content );
        setContentShown( root, shown, false );
    }

    public void setContentShownNoAnimation( View view, boolean shown ) {
        setContentShown( view, shown, false );
    }

    protected void setContentShown( View view, boolean shown, boolean animation ) {
        View progress = view.findViewById( R.id.INTERNAL_PROGRESS_CONTAINER_ID );
        View content = view.findViewById( R.id.INTERNAL_FRAGMENT_CONTAINER_ID );

        if ( progress == null || content == null ) {
            return;
        }

        if ( shown ) {
            if ( animation ) {
                progress.startAnimation( AnimationUtils.loadAnimation( this, R.anim.fade_out ) );
                content.startAnimation( AnimationUtils.loadAnimation( this, R.anim.fade_in ) );
            }
            progress.setVisibility( View.GONE );
            content.setVisibility( View.VISIBLE );
        } else {
            if ( animation ) {
                progress.startAnimation( AnimationUtils.loadAnimation( this, R.anim.fade_in ) );
                content.startAnimation( AnimationUtils.loadAnimation( this, R.anim.fade_out ) );
            }
            progress.setVisibility( View.VISIBLE );
            content.setVisibility( View.GONE );
        }
    }

    protected void setContentMsg( String msg ) {
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL );
        tv.setPadding( 12, 8, 12, 8 );
        tv.setText( msg );
        setContentView( createContentView( tv ) );
    }

    protected Fragment replaceFragment( Class<?> clss, Bundle args, boolean addToStack ) {
        return replaceFragment( clss, args, R.id.fragment_container, addToStack );
    }

    protected Fragment replaceFragment( Class<?> clss, Bundle args ) {
        return replaceFragment( clss, args, R.id.fragment_container );
    }

    protected Fragment replaceFragment( Class<?> clss, Bundle args, int id  ) {
        return replaceFragment( clss, args, id, true );
    }

    protected Fragment replaceFragment( Class<?> clss, Bundle args, int id, boolean addToStack  ) {
        String tag = clss.getSimpleName();
        if ( args != null && args.containsKey( FRAGMENT_TAG_EXTRA ) ) {
            String extra = args.getString( FRAGMENT_TAG_EXTRA );
            if ( extra != null ) {
                tag = tag.concat( extra );
            }
        }
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentByTag( tag );
        if ( f == null ) {
            f = Fragment.instantiate( this, clss.getName(), args );
        }
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace( id, f, tag );
        if ( addToStack ) {
            ft.addToBackStack( tag );
        }
        ft.commit();
        return f;
    }

    protected Fragment addFragment( Class<?> clss, Bundle args ) {
        return addFragment( clss, args, R.id.fragment_container );
    }

    protected Fragment addFragment( Class<?> clss, Bundle args, int id ) {
        String tag = clss.getSimpleName();
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentByTag( tag );
        if ( f == null ) {
            f = Fragment.instantiate( this, clss.getName(), args );
            FragmentTransaction ft = fm.beginTransaction();
            ft.add( id, f, tag );
            ft.commit();
        }
        return f;
    }

    protected Fragment getFragment( Class<?> clss ) {
        String tag = clss.getSimpleName();
        FragmentManager fm = getSupportFragmentManager();
        return fm.findFragmentByTag( tag );
    }

    protected View inflate( int resId ) {
        return mInflater.inflate( resId, null, false );
    }

    protected View inflate( int resId, ViewGroup root ) {
        return mInflater.inflate( resId, root, false );
    }

    public Cursor getAirportDetails( String siteNumber ) {
        SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Airports.TABLE_NAME + " a LEFT OUTER JOIN " + States.TABLE_NAME + " s"
                + " ON a." + Airports.ASSOC_STATE + "=s." + States.STATE_CODE );
        Cursor c = builder.query( db, new String[]{ "*" }, Airports.SITE_NUMBER + "=?",
                new String[]{ siteNumber }, null, null, null, null );
        if ( !c.moveToFirst() ) {
            return null;
        }

        return c;
    }

    public SQLiteDatabase getDatabase( String type ) {
        SQLiteDatabase db = mDbManager.getDatabase( type );
        if ( db == null ) {
            Intent intent = checkData();
            if ( intent != null ) {
                startActivity( intent );
                finish();
            }
        }
        return db;
    }

    protected Intent checkData() {
        if ( !SystemUtils.isExternalStorageAvailable() ) {
            return new Intent( this, ExternalStorageActivity.class );
        }

        Cursor c = mDbManager.getCurrentFromCatalog();

        String msg = null;
        HashSet<String> installed = new HashSet<>();

        // Check if we have any expired data. If yes, then redirect to download activity
        if ( c.moveToFirst() ) {
            do {
                String type = c.getString( c.getColumnIndex( Catalog.TYPE ) );
                installed.add( type );

                int age = c.getInt( c.getColumnIndex( "age" ) );
                if ( age <= 0 ) {
                    msg = "One or more data items have expired";
                    break;
                }

                // Try to make sure we can open the databases
                SQLiteDatabase db = mDbManager.getDatabase( type );
                if ( db == null ) {
                    msg = "Database is corrupted. Please delete and re-install";
                    break;
                }
            } while ( c.moveToNext() );
        }
        c.close();

        if ( installed.size() < 4 ) {
            msg = "Please download the required database";
        }

        Intent intent = null;
        if ( msg != null ) {
            intent = new Intent( this, DownloadActivity.class );
            intent.putExtra( "MSG", msg );
        }

        return intent;
    }

    public void showAirportTitle( Cursor c ) {
        View root = findViewById( R.id.airport_title_layout );
        TextView tv = (TextView) root.findViewById( R.id.facility_name );
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null || code.length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
        }
        String tower = c.getString( c.getColumnIndex( Airports.TOWER_ON_SITE ) );
        int color = tower.equals( "Y" )? Color.rgb( 48, 96, 144 ) : Color.rgb( 128, 72, 92 );
        tv.setTextColor( color );
        String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String type = DataUtils.decodeLandingFaclityType( siteNumber );
        tv.setText( String.format( "%s %s", name, type ) );
        tv = (TextView) root.findViewById( R.id.facility_id );
        tv.setTextColor( color );
        tv.setText( code );
        tv = (TextView) root.findViewById( R.id.facility_info );
        String city = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
        String state = c.getString( c.getColumnIndex( States.STATE_NAME ) );
        if ( state == null ) {
            state = c.getString( c.getColumnIndex( Airports.ASSOC_COUNTY ) );
        }
        tv.setText( String.format( "%s, %s", city, state ) );
        tv = (TextView) root.findViewById( R.id.facility_info2 );
        int distance = c.getInt( c.getColumnIndex( Airports.DISTANCE_FROM_CITY_NM ) );
        String dir = c.getString( c.getColumnIndex( Airports.DIRECTION_FROM_CITY ) );
        String status = c.getString( c.getColumnIndex( Airports.STATUS_CODE ) );
        tv.setText( String.format( "%s, %d miles %s of city center",
                DataUtils.decodeStatus( status ), distance, dir ) );
        tv = (TextView) root.findViewById( R.id.facility_info3 );
        float elev_msl = c.getFloat( c.getColumnIndex( Airports.ELEVATION_MSL ) );
        int tpa_agl = c.getInt( c.getColumnIndex( Airports.PATTERN_ALTITUDE_AGL ) );
        String est = "";
        if ( tpa_agl == 0 ) {
            tpa_agl = 1000;
            est = " (est.)";
        }
        tv.setText( String.format( "%s MSL elevation - %s MSL TPA %s",
                FormatUtils.formatFeet( elev_msl ),
                FormatUtils.formatFeet( elev_msl + tpa_agl ), est ) );

        String s = c.getString( c.getColumnIndex( Airports.EFFECTIVE_DATE ) );
        GregorianCalendar endDate = new GregorianCalendar(
                        Integer.valueOf( s.substring( 6 ) ),
                        Integer.valueOf( s.substring( 3, 5 ) ),
                        Integer.valueOf( s.substring( 0, 2 ) ) ) ;
        // Calculate end date of the 56-day cycle
        endDate.add( GregorianCalendar.DAY_OF_MONTH, 56 );
        Calendar now = Calendar.getInstance();
        if ( now.after( endDate ) ) {
            // Show the expired warning
            tv = (TextView) root.findViewById( R.id.expired_label );
            tv.setVisibility( View.VISIBLE );
        }

        CheckBox cb = (CheckBox) root.findViewById( R.id.airport_star );
        cb.setChecked( mDbManager.isFavoriteAirport( siteNumber ) );
        cb.setTag( siteNumber );
        cb.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                CheckBox cb = (CheckBox) v;
                String siteNumber = (String) cb.getTag();
                if ( cb.isChecked() ) {
                    mDbManager.addToFavoriteAirports( siteNumber );
                    Toast.makeText( ActivityBase.this, "Added to favorites list",
                            Toast.LENGTH_LONG ).show();
                } else {
                    mDbManager.removeFromFavoriteAirports( siteNumber );
                    Toast.makeText( ActivityBase.this, "Removed from favorites list",
                            Toast.LENGTH_LONG ).show();
                }
            }

        } );

        ImageView iv = (ImageView) root.findViewById( R.id.airport_map );
        String lat = c.getString( c.getColumnIndex( Airports.REF_LATTITUDE_DEGREES ) );
        String lon = c.getString( c.getColumnIndex( Airports.REF_LONGITUDE_DEGREES ) );
        if ( lat.length() > 0 && lon.length() > 0 ) {
            iv.setTag( "geo:"+lat+","+lon+"?z=16" );
            iv.setOnClickListener( new OnClickListener() {

                @Override
                public void onClick( View v ) {
                    String tag = (String) v.getTag();
                    Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( tag ) );
                    startActivity( intent );
                }

            } );
        } else {
            iv.setVisibility( View.GONE );
        }
    }

    protected void showNavaidTitle( Cursor c ) {
        View root = findViewById( R.id.navaid_title_layout );
        String id = c.getString( c.getColumnIndex( Nav1.NAVAID_ID ) );
        String name = c.getString( c.getColumnIndex( Nav1.NAVAID_NAME ) );
        String type = c.getString( c.getColumnIndex( Nav1.NAVAID_TYPE ) );
        TextView tv = (TextView) root.findViewById( R.id.navaid_name );
        tv.setText( String.format( "%s - %s %s", id, name, type ) );
        String city = c.getString( c.getColumnIndex( Nav1.ASSOC_CITY ) );
        String state = c.getString( c.getColumnIndex( States.STATE_NAME ) );
        tv = (TextView) root.findViewById( R.id.navaid_info );
        tv.setText( String.format( "%s, %s", city, state ) );
        String use = c.getString( c.getColumnIndex( Nav1.PUBLIC_USE ) );
        Float elev_msl = c.getFloat( c.getColumnIndex( Nav1.ELEVATION_MSL ) );
        tv = (TextView) root.findViewById( R.id.navaid_info2 );
        tv.setText( String.format( "%s, %s elevation",
                use.equals( "Y" )? "Public use" : "Private use",
                FormatUtils.formatFeetMsl( elev_msl ) ) );
        tv = (TextView) root.findViewById( R.id.navaid_morse1 );
        tv.setText( DataUtils.getMorseCode( id.substring( 0, 1 ) ) );
        if ( id.length() > 1 ) {
            tv = (TextView) root.findViewById( R.id.navaid_morse2 );
            tv.setText( DataUtils.getMorseCode( id.substring( 1, 2 ) ) );
        }
        if ( id.length() > 2 ) {
            tv = (TextView) root.findViewById( R.id.navaid_morse3 );
            tv.setText( DataUtils.getMorseCode( id.substring( 2, 3 ) ) );
        }
    }

    public boolean postRunnable( Runnable r, long delayMillis ) {
        return mHandler.postDelayed( r, delayMillis );
    }

    @TargetApi(11)
    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.mainmenu, menu );

        MenuItem settingsItem = menu.findItem( R.id.menu_settings );
        settingsItem.setVisible( getSelfNavDrawerItem() == NAVDRAWER_ITEM_INVALID );

        MenuItem searchItem = menu.findItem( R.id.menu_search );
        SearchView searchView = (SearchView) MenuItemCompat.getActionView( searchItem );
        SearchManager searchManager = (SearchManager) getSystemService( Context.SEARCH_SERVICE );
        searchView.setSearchableInfo( searchManager.getSearchableInfo( getComponentName() ) );
        searchView.setIconifiedByDefault( false );

        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        if ( mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected( item ) ) {
            return true;
        }

        switch ( item.getItemId() ) {
        case android.R.id.home:
            onBackPressed();
            return true;
        case R.id.menu_search:
            return true;
        case R.id.menu_download:
            Intent download = new Intent( this, DownloadActivity.class );
            startActivity( download );
            return true;
        case R.id.menu_donate:
            Intent donate = new Intent( this, DonateActivity.class );
            startActivity( donate );
            return true;
        case R.id.menu_settings:
            Intent intent = new Intent( this, PreferencesActivity.class );
            startActivity( intent );
            return true;
        case R.id.menu_about:
            Intent about = new Intent( this, AboutActivity.class );
            startActivity( about );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

    public void setActionBarTitle( Cursor c ) {
        ActionBar actionBar = getSupportActionBar();
        if ( actionBar != null ) {
            String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            String type = DataUtils.decodeLandingFaclityType( siteNumber );
            String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
            String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
            if ( code == null || code.length() == 0 ) {
                code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
            }
            actionBar.setTitle( String.format( "%s - %s %s", code, name, type ) );
        }
    }

    protected void setActionBarTitle( Cursor c, String subtitle ) {
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null  || code.length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
        }
        String title = code;
        Boolean isScreenWide = getResources().getBoolean( R.bool.IsScreenWide );
        if ( isScreenWide ) {
            String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            String type = DataUtils.decodeLandingFaclityType( siteNumber );
            String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
            title = String.format( "%s - %s %s", code, name, type );
        }

        setActionBarTitle( title, subtitle );
    }

    protected void setActionBarTitle( String title ) {
        setActionBarTitle( title, getTitle().toString() );
    }

    protected void setActionBarTitle( String title, String subtitle ) {
        ActionBar actionBar = getSupportActionBar();
        if ( actionBar != null ) {
            actionBar.setTitle( title );
            actionBar.setSubtitle( subtitle );
        }
    }

    protected void setActionBarSubtitle( String subtitle ) {
        ActionBar actionBar = getSupportActionBar();
        if ( actionBar != null ) {
            actionBar.setSubtitle( subtitle );
        }
    }

    protected void showFaddsEffectiveDate( Cursor c ) {
        TextView tv = (TextView) findViewById( R.id.effective_date );
        if ( tv != null ) {
            tv.setText( "Effective date: "
                    + c.getString( c.getColumnIndex( Airports.EFFECTIVE_DATE ) ) );
        }
    }

}
