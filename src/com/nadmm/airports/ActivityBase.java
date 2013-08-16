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
import java.util.HashSet;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Time;
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
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Catalog;
import com.nadmm.airports.DatabaseManager.Nav1;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.afd.BrowseFragment;
import com.nadmm.airports.afd.FavoritesFragment;
import com.nadmm.airports.donate.DonateActivity;
import com.nadmm.airports.donate.DonateDatabase;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.ExternalStorageActivity;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.SystemUtils;

public class ActivityBase extends ActionBarActivity {

    private DatabaseManager mDbManager;
    private MenuItem mRefreshItem;
    private Drawable mRefreshDrawable;
    private LayoutInflater mInflater;
    private CursorAsyncTask mTask;

    private Handler mHandler = new Handler();
    IntentFilter mFilter;
    BroadcastReceiver mExternalStorageReceiver;
    boolean mExternalStorageAvailable = false;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        mDbManager = DatabaseManager.instance( this );
        mInflater = getLayoutInflater();
        overridePendingTransition( R.anim.fade_in, R.anim.fade_out );

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
            Cursor c = db.queryAlldonations();
            Application.sDonationDone = c.moveToFirst();
            db.close();
        }

        super.onCreate( savedInstanceState );
    }

    @Override
    protected void onPause() {
        if ( mTask != null ) {
            mTask.cancel( true );
        }
        overridePendingTransition( R.anim.fade_in, R.anim.fade_out );
        unregisterReceiver( mExternalStorageReceiver );
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver( mExternalStorageReceiver, mFilter );

        ActionBar actionBar = getSupportActionBar();
        if ( actionBar != null ) {
            actionBar.setHomeButtonEnabled( true );
            actionBar.setDisplayHomeAsUpEnabled( true );
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

    protected void setSlidingMenuActivatedItem( int position ) {
        //mSlidingMenuFragment.setActivatedItem( position );
    }

    protected View createContentView( int id ) {
        View view = inflate( id );
        return createContentView( view );
    }

    public View createContentView( View view ) {
        FrameLayout root = new FrameLayout( this );
        root.setLayoutParams(new FrameLayout.LayoutParams(
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

    public Fragment replaceFragment( Class<?> clss, Bundle args ) {
        return replaceFragment( clss, args, R.id.fragment_container );
    }

    public Fragment replaceFragment( Class<?> clss, Bundle args, int id  ) {
        return replaceFragment( clss, args, id, true );
    }

    public Fragment replaceFragment( Class<?> clss, Bundle args, int id, boolean addTostack  ) {
        String tag = clss.getSimpleName();
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentByTag( tag );
        if ( f == null ) {
            f = Fragment.instantiate( this, clss.getName(), args );
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace( id, f, tag );
            if ( addTostack ) {
                ft.addToBackStack( clss.getSimpleName() );
            }
            ft.commit();
        }
        return f;
    }

    public Fragment addFragment( Class<?> clss, Bundle args ) {
        return addFragment( clss, args, R.id.fragment_container );
    }

    public Fragment addFragment( Class<?> clss, Bundle args, int id ) {
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
        builder.setTables( Airports.TABLE_NAME+" a LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
        Cursor c = builder.query( db, new String[] { "*" }, Airports.SITE_NUMBER+"=?",
                new String[] { siteNumber }, null, null, null, null );
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
            Intent intent = new Intent( this, ExternalStorageActivity.class );
            return intent;
        }

        Cursor c = mDbManager.getCurrentFromCatalog();

        String msg = null;
        HashSet<String> installed = new HashSet<String>();

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
        TextView tv = (TextView) root.findViewById( R.id.airport_name );
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
        tv.setText( String.format( "%s - %s %s", code, name, type ) );
        tv = (TextView) root.findViewById( R.id.airport_info );
        String city = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
        String state = c.getString( c.getColumnIndex( States.STATE_NAME ) );
        if ( state == null ) {
            state = c.getString( c.getColumnIndex( Airports.ASSOC_COUNTY ) );
        }
        tv.setText( String.format( "%s, %s", city, state ) );
        tv = (TextView) root.findViewById( R.id.airport_info2 );
        int distance = c.getInt( c.getColumnIndex( Airports.DISTANCE_FROM_CITY_NM ) );
        String dir = c.getString( c.getColumnIndex( Airports.DIRECTION_FROM_CITY ) );
        String status = c.getString( c.getColumnIndex( Airports.STATUS_CODE ) );
        tv.setText( String.format( "%s, %d miles %s of city center",
                DataUtils.decodeStatus( status ), distance, dir ) );
        tv = (TextView) root.findViewById( R.id.airport_info3 );
        float elev_msl = c.getFloat( c.getColumnIndex( Airports.ELEVATION_MSL ) );
        int tpa_agl = c.getInt( c.getColumnIndex( Airports.PATTERN_ALTITUDE_AGL ) );
        String est = "";
        if ( tpa_agl == 0 ) {
            tpa_agl = 1000;
            est = " (est.)";
        }
        tv.setText( String.format( "%s MSL elevation - %s MSL TPA %s",
                FormatUtils.formatFeet( elev_msl ),
                FormatUtils.formatFeet( elev_msl+tpa_agl ), est ) );

        String s = c.getString( c.getColumnIndex( Airports.EFFECTIVE_DATE ) );
        Time endDate = new Time();
        endDate.set( Integer.valueOf( s.substring( 3, 5 ) ).intValue(),
                Integer.valueOf( s.substring( 0, 2 ) )-1,
                Integer.valueOf( s.substring( 6 ) ).intValue() );
        // Calculate end date of the 56-day cycle
        endDate.monthDay += 56;
        endDate.normalize( false );
        Time now = new Time();
        now.setToNow();
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

    public void startRefreshAnimation() {
        if ( mRefreshItem != null ) {
            Resources res = getResources();
            AnimationDrawable d = (AnimationDrawable) res.getDrawable( R.drawable.ic_popup_sync );
            mRefreshItem.setIcon( d );
            d.start();
        }
    }

    public void stopRefreshAnimation() {
        if ( mRefreshItem != null ) {
            mRefreshItem.setIcon( mRefreshDrawable );
        }
    }

    public void setRefreshItemVisible( Boolean visible ) {
        if ( mRefreshItem != null ) {
            mRefreshItem.setVisible( visible );
        }
    }

    @TargetApi(11)
    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.mainmenu, menu );
        mRefreshItem = menu.findItem( R.id.menu_refresh );
        mRefreshDrawable = mRefreshItem.getIcon();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            SearchManager searchManager =
                    (SearchManager) getSystemService( Context.SEARCH_SERVICE );
            SearchView searchView = new SearchView( getSupportActionBar().getThemedContext() );
            searchView.setSearchableInfo( searchManager.getSearchableInfo( getComponentName() ) );
            searchView.setIconifiedByDefault( false );
            menu.findItem( R.id.menu_search ).setActionView( searchView );
        }

        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch ( item.getItemId() ) {
        case android.R.id.home:
            //startHomeActivity();
            return true;
        case R.id.menu_search:
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                onSearchRequested();
            }
            return true;
        case R.id.menu_download:
            Intent download = new Intent( this, DownloadActivity.class );
            startActivity( download );
            return true;
        case R.id.menu_settings:
            Intent settings = new Intent( this, PreferencesActivity.class  );
            startActivity( settings );
            return true;
        case R.id.menu_donate:
            Intent donate = new Intent( this, DonateActivity.class );
            startActivity( donate );
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
        setActionBarTitle( c, getTitle().toString() );
    }

    protected void setActionBarTitle( Cursor c, String subtitle ) {
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null  || code.length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
        }
        String title = code;
        Boolean showAirportName = getResources().getBoolean( R.bool.ActionbarShowAirportName );
        if ( showAirportName ) {
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
        getSupportActionBar().setTitle( title );
        getSupportActionBar().setSubtitle( subtitle );
    }

    protected void setActionBarSubtitle( String subtitle ) {
        getSupportActionBar().setSubtitle( subtitle );
    }

    protected void showFaddsEffectiveDate( Cursor c ) {
        TextView tv = (TextView) findViewById( R.id.effective_date );
        tv.setText( "Effective date: "
                +c.getString( c.getColumnIndex( Airports.EFFECTIVE_DATE ) ) );
    }

    protected Class<?> getHomeFragmentClass() {
        ArrayList<String> fav = mDbManager.getAptFavorites();
        Class<?> clss = null;
        if ( fav.size() > 0 ) {
            clss = FavoritesFragment.class;
        } else {
            clss = BrowseFragment.class;
        }
        return clss;
    }

}
