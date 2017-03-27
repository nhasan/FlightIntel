/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2017 Nadeem Hasan <nhasan@nadmm.com>
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

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.nadmm.airports.aeronav.ChartsDownloadActivity;
import com.nadmm.airports.afd.AfdMainActivity;
import com.nadmm.airports.clocks.ClocksActivity;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.Nav1;
import com.nadmm.airports.data.DatabaseManager.States;
import com.nadmm.airports.data.DownloadActivity;
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
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;
import com.nadmm.airports.views.MultiSwipeRefreshLayout;
import com.nadmm.airports.wx.WxMainActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class ActivityBase extends AppCompatActivity implements
        MultiSwipeRefreshLayout.CanChildScrollUpCallback  {

    private DatabaseManager mDbManager;
    private LayoutInflater mInflater;
    private CursorAsyncTask mTask;

    private IntentFilter mFilter;
    private BroadcastReceiver mExternalStorageReceiver;

    private Toolbar mActionBarToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AppBarLayout mAppBar;

    private Handler mHandler;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mNavigationView;

    private FirebaseAnalytics mFirebaseAnalytics;
    private SharedPreferences mPreferences;

    private static final int NAVDRAWER_LAUNCH_DELAY = 250;
    protected static final int NAVDRAWER_ITEM_INVALID = -1;
    protected static final String EXTRA_MSG = "MSG";

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

        Intent intent = getIntent();
        if ( intent.hasExtra( EXTRA_MSG ) ) {
            String msg = intent.getStringExtra( EXTRA_MSG );
            Toast.makeText( this, msg, Toast.LENGTH_LONG ).show();
        }

        mPreferences = PreferenceManager.getDefaultSharedPreferences( this );
        mFirebaseAnalytics = FirebaseAnalytics.getInstance( this );
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

        if ( mNavigationView != null ) {
            mNavigationView.setCheckedItem( getSelfNavDrawerItem() );
        }

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
        enableDisableSwipeRefresh( false );

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
        int selfItem = getSelfNavDrawerItem();

        mDrawerLayout = (DrawerLayout) findViewById( R.id.drawer_layout );
        if ( mDrawerLayout == null ) {
            return;
        }
        mDrawerLayout.setStatusBarBackgroundColor(
                ContextCompat.getColor( this, R.color.color_primary_dark ) );

        mNavigationView = (NavigationView) mDrawerLayout.findViewById( R.id.navdrawer );
        if ( selfItem == NAVDRAWER_ITEM_INVALID ) {
            // do not show a nav drawer
            if ( mNavigationView != null ) {
                ((ViewGroup) mNavigationView.getParent()).removeView( mNavigationView );
            }
            mDrawerLayout = null;
            return;
        }

        mDrawerToggle = new ActionBarDrawerToggle( this, mDrawerLayout,
                getActionBarToolbar(), R.string.drawer_open, R.string.drawer_close ) {

            public void onDrawerClosed( View view ) {
                supportInvalidateOptionsMenu();
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
        mDrawerToggle.setDrawerSlideAnimationEnabled( false );
        mDrawerLayout.addDrawerListener( mDrawerToggle );
        updateDrawerToggle();

        // Initialize navigation drawer
        mNavigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected( MenuItem item ) {
                    item.setChecked( true );
                    final int id = item.getItemId();

                    if ( id != getSelfNavDrawerItem() ) {
                        // Launch the target Activity after a short delay to allow the drawer close
                        // animation to finish without stutter
                        mHandler.postDelayed( new Runnable() {
                            @Override
                            public void run() {
                                goToNavDrawerItem( id );
                            }
                        }, NAVDRAWER_LAUNCH_DELAY );
                    }

                    mDrawerLayout.closeDrawer( GravityCompat.START );
                    return false;
                }
            } );
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

    private void goToNavDrawerItem( int id ) {
        Intent intent;
        switch ( id ) {
            case R.id.navdrawer_afd:
                intent = new Intent( this, AfdMainActivity.class );
                startActivity( intent );
                finish();
                break;
            case R.id.navdrawer_wx:
                intent = new Intent( this, WxMainActivity.class );
                startActivity( intent );
                finish();
                break;
            case R.id.navdrawer_tfr:
                intent = new Intent( this, TfrListActivity.class );
                startActivity( intent );
                finish();
                break;
            case R.id.navdrawer_library:
                intent = new Intent( this, LibraryActivity.class );
                startActivity( intent );
                finish();
                break;
            case R.id.navdrawer_scratchpad:
                intent = new Intent( this, ScratchPadActivity.class );
                startActivity( intent );
                finish();
                break;
            case R.id.navdrawer_clocks:
                intent = new Intent( this, ClocksActivity.class );
                startActivity( intent );
                finish();
                break;
            case R.id.navdrawer_e6b:
                intent = new Intent( this, E6bActivity.class );
                startActivity( intent );
                finish();
                break;
            case R.id.navdrawer_charts:
                intent = new Intent( this, ChartsDownloadActivity.class );
                startActivity( intent );
                finish();
                break;
            case R.id.navdrawer_download:
                Intent download = new Intent( this, DownloadActivity.class );
                startActivity( download );
                break;
            case R.id.navdrawer_about:
                Intent about = new Intent( this, AboutActivity.class );
                startActivity( about );
                break;
            case R.id.navdrawer_settings:
                intent = new Intent( this, PreferencesActivity.class );
                startActivity( intent );
                break;
        }
    }

    public void setDrawerIndicatorEnabled( boolean enable ) {
        if ( mDrawerToggle != null ) {
            mDrawerToggle.setDrawerIndicatorEnabled( enable );
        }
    }

    protected Toolbar getActionBarToolbar() {
        if ( mActionBarToolbar == null ) {
            mAppBar = (AppBarLayout) findViewById( R.id.appbar );
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

    public void setRefreshing( boolean refreshing ) {
        if ( mSwipeRefreshLayout != null ) {
            mSwipeRefreshLayout.setRefreshing( refreshing );
        }
    }

    public boolean isRefreshing() {
        return mSwipeRefreshLayout != null && mSwipeRefreshLayout.isRefreshing();
    }

    public void enableDisableSwipeRefresh( boolean enable ) {
        if ( mSwipeRefreshLayout != null ) {
            mSwipeRefreshLayout.setEnabled( enable );
        }
    }

    public AppBarLayout getAppBar() {
        return mAppBar;
    }

    protected void requestDataRefresh() {
    }

    protected void showAppBar( boolean show ) {
        if ( mAppBar != null ) {
            mAppBar.setExpanded( show, true );
        }
    }

    public void onFragmentStarted( FragmentBase fragment ) {
        showAppBar( true );
    }

    // Subclasses can override this for custom behavior
    protected void onNavDrawerStateChanged( boolean isOpen, boolean isAnimating ) {
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
        int white = ContextCompat.getColor( this, android.R.color.white );
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
        tv.setPadding( dpToPx( 12 ), dpToPx( 8 ), dpToPx( 12 ), dpToPx( 8 ) );
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
            c.close();
            c = null;
        }

        return c;
    }

    public SQLiteDatabase getDatabase( String type ) {
        SQLiteDatabase db = mDbManager.getDatabase( type );
        if ( db == null ) {
            Intent intent = new Intent( this, DownloadActivity.class );
            intent.putExtra( "MSG", "Database is corrupted. Please delete and re-install" );
            startActivity( intent );
            finish();
        }
        return db;
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
        tv.setText( String.format( Locale.US, "%s %s", name, type ) );
        tv = (TextView) root.findViewById( R.id.facility_id );
        tv.setTextColor( color );
        tv.setText( code );
        tv = (TextView) root.findViewById( R.id.facility_info );
        String city = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
        String state = c.getString( c.getColumnIndex( States.STATE_NAME ) );
        if ( state == null ) {
            state = c.getString( c.getColumnIndex( Airports.ASSOC_COUNTY ) );
        }
        tv.setText( String.format( Locale.US, "%s, %s", city, state ) );
        tv = (TextView) root.findViewById( R.id.facility_info2 );
        int distance = c.getInt( c.getColumnIndex( Airports.DISTANCE_FROM_CITY_NM ) );
        String dir = c.getString( c.getColumnIndex( Airports.DIRECTION_FROM_CITY ) );
        String status = c.getString( c.getColumnIndex( Airports.STATUS_CODE ) );
        tv.setText( String.format( Locale.US, "%s, %d miles %s of city center",
                DataUtils.decodeStatus( status ), distance, dir ) );
        tv = (TextView) root.findViewById( R.id.facility_info3 );
        float elev_msl = c.getFloat( c.getColumnIndex( Airports.ELEVATION_MSL ) );
        int tpa_agl = c.getInt( c.getColumnIndex( Airports.PATTERN_ALTITUDE_AGL ) );
        String est = "";
        if ( tpa_agl == 0 ) {
            tpa_agl = 1000;
            est = " (est.)";
        }
        tv.setText( String.format( Locale.US, "%s MSL elev. - %s MSL TPA %s",
                FormatUtils.formatFeet( elev_msl ),
                FormatUtils.formatFeet( elev_msl + tpa_agl ), est ) );

        String s = c.getString( c.getColumnIndex( Airports.EFFECTIVE_DATE ) );
        GregorianCalendar endDate = new GregorianCalendar(
                        Integer.valueOf( s.substring( 6 ) ),
                        Integer.valueOf( s.substring( 3, 5 ) ),
                        Integer.valueOf( s.substring( 0, 2 ) ) );
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
        tv.setText( String.format( Locale.US, "%s - %s %s", id, name, type ) );
        String city = c.getString( c.getColumnIndex( Nav1.ASSOC_CITY ) );
        String state = c.getString( c.getColumnIndex( States.STATE_NAME ) );
        tv = (TextView) root.findViewById( R.id.navaid_info );
        tv.setText( String.format( Locale.US, "%s, %s", city, state ) );
        String use = c.getString( c.getColumnIndex( Nav1.PUBLIC_USE ) );
        Float elev_msl = c.getFloat( c.getColumnIndex( Nav1.ELEVATION_MSL ) );
        tv = (TextView) root.findViewById( R.id.navaid_info2 );
        tv.setText( String.format( Locale.US, "%s, %s elevation",
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

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.mainmenu, menu );

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
            actionBar.setTitle( String.format( Locale.US, "%s - %s %s", code, name, type ) );
        }
    }

    protected int dpToPx( float dp ) {
        return UiUtils.convertDpToPx( this, dp );
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
            title = String.format( Locale.US, "%s - %s %s", code, name, type );
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
            String s = c.getString( c.getColumnIndex( Airports.EFFECTIVE_DATE ) );
            Date date = TimeUtils.parseFaaDate( s );
            if ( date != null ) {
                Calendar start = Calendar.getInstance();
                start.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
                start.setTime( date );
                start.add( Calendar.MINUTE, 9 * 60 + 1 );
                Calendar end = Calendar.getInstance();
                end.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
                end.setTime( date );
                end.add( Calendar.DATE, 28 );
                end.add( Calendar.MINUTE, 9 * 60 + 1 );
                s = TimeUtils.formatDateRange( this, start, end );
                tv.setText( s );
            }
        }
    }

    public String getPrefHomeAirport() {
        return mPreferences.getString( PreferencesActivity.KEY_HOME_AIRPORT, "" );
    }

    public boolean getPrefUseGps() {
        return mPreferences.getBoolean( PreferencesActivity.KEY_LOCATION_USE_GPS, false );
    }

    public int getPrefNearbyRadius()
    {
        return Integer.valueOf(
                mPreferences.getString( PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "30" ) );
    }

    public boolean getPrefShowExtraRunwayData(){
        return mPreferences.getBoolean( PreferencesActivity.KEY_SHOW_EXTRA_RUNWAY_DATA, false );
    }

    public boolean getPrefShowGpsNotam(){
        return mPreferences.getBoolean( PreferencesActivity.KEY_SHOW_GPS_NOTAMS, false );
    }

    public boolean getPrefAutoDownoadOnMeteredNetwork() {
        return mPreferences.getBoolean( PreferencesActivity.KEY_AUTO_DOWNLOAD_ON_3G, false );
    }

    public boolean getPrefDisclaimerAgreed() {
        return mPreferences.getBoolean( PreferencesActivity.KEY_DISCLAIMER_AGREED, false );
    }

    public boolean getPrefShowLocalTime() {
        return mPreferences.getBoolean( PreferencesActivity.KEY_SHOW_LOCAL_TIME, false );
    }

    public String getPrefHomeScreen() {
        return mPreferences.getString( PreferencesActivity.KEY_HOME_SCREEN, "A/FD" );
    }

    public boolean getPrefAlwaysShowNearby() {
        return mPreferences.getBoolean( PreferencesActivity.KEY_ALWAYS_SHOW_NEARBY, false );
    }

    public boolean getPrefAnalyticsOptout() {
        return  mPreferences.getBoolean( PreferencesActivity.KEY_ANALYTICS_OPTOUT, false );
    }

    public void logAnalyticsEvent( String event, Bundle parameters ) {
        if ( !getPrefAnalyticsOptout() ) {
            mFirebaseAnalytics.logEvent( event, parameters );
        }
    }

    public void faLogSelectContent( String type, String id ) {
        Bundle bundle = new Bundle();
        bundle.putString( FirebaseAnalytics.Param.CONTENT_TYPE, type );
        bundle.putString( FirebaseAnalytics.Param.ITEM_ID, id );
        logAnalyticsEvent( FirebaseAnalytics.Event.SELECT_CONTENT, bundle );
    }

    public void faLogViewItemList( String categ ) {
        Bundle bundle = new Bundle();
        bundle.putString( FirebaseAnalytics.Param.ITEM_CATEGORY, categ );
        logAnalyticsEvent( FirebaseAnalytics.Event.VIEW_ITEM_LIST, bundle );
    }

    public void faLogViewItem( String categ, String id ) {
        faLogViewItem( categ, id, null );
    }

    public void faLogViewItem( String categ, String id, String name ) {
        Bundle bundle = new Bundle();
        bundle.putString( FirebaseAnalytics.Param.ITEM_CATEGORY, categ );
        bundle.putString( FirebaseAnalytics.Param.ITEM_ID, id );
        if ( name != null ) {
            bundle.putString( FirebaseAnalytics.Param.ITEM_NAME, name );
        }
        logAnalyticsEvent( FirebaseAnalytics.Event.VIEW_ITEM, bundle );

        faLogSelectContent( categ, id );
    }

}
