/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.format.Time;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Catalog;
import com.nadmm.airports.DatabaseManager.Nav1;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.FormatUtils;
import com.nadmm.airports.utils.UiUtils;

public class ActivityBase extends FragmentActivity {

    protected DatabaseManager mDbManager;
    private MenuItem mRefreshItem;
    private Drawable mRefreshDrawable;
    private LayoutInflater mInflater;
    private Handler mHandler = new Handler();
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick( View v ) {
            Intent intent = (Intent) v.getTag();
            startActivity( intent );
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        mDbManager = DatabaseManager.instance( this );
        SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
        if ( db == null ) {
            // Make sure database is available and usable
            Intent intent = checkData();
            if ( intent != null ) {
                startActivity( intent );
                finish();
            }
        } else {
            mInflater = getLayoutInflater();
            overridePendingTransition( R.anim.fade_in, R.anim.fade_out );
            super.onCreate( savedInstanceState );
        }
    }

    @Override
    protected void onPause() {
        overridePendingTransition( R.anim.fade_in, R.anim.fade_out );
        super.onPause();
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
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT ) );

        LinearLayout pframe = new LinearLayout( this );
        pframe.setId( R.id.INTERNAL_PROGRESS_CONTAINER_ID );
        pframe.setOrientation( LinearLayout.VERTICAL );
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

    public void setContentShown( View view, boolean shown ) {
        View progress = view.findViewById( R.id.INTERNAL_PROGRESS_CONTAINER_ID );
        View content = view.findViewById( R.id.INTERNAL_FRAGMENT_CONTAINER_ID );
        if ( shown ) {
            progress.startAnimation( AnimationUtils.loadAnimation( this, R.anim.fade_out ) );
            content.startAnimation( AnimationUtils.loadAnimation( this, R.anim.fade_in ) );
            progress.setVisibility( View.GONE );
            content.setVisibility( View.VISIBLE );
        } else {
            progress.startAnimation( AnimationUtils.loadAnimation( this, R.anim.fade_in ) );
            content.startAnimation( AnimationUtils.loadAnimation( this, R.anim.fade_out ) );
            progress.setVisibility( View.VISIBLE );
            content.setVisibility( View.GONE );
        }
    }

    protected void setContentShown( boolean shown ) {
        View root = findViewById( android.R.id.content );
        setContentShown( root, shown );
    }

    protected void setContentMsg( String msg ) {
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL );
        tv.setPadding( 12, 8, 12, 8 );
        tv.setText( msg );
        setContentView( createContentView( tv ) );
    }

    protected void addFragment( Class<?> clss ) {
        String tag = clss.getSimpleName();
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentByTag( tag );
        if ( f == null ) {
            f = Fragment.instantiate( this, clss.getName() );
        }
        FragmentTransaction ft = fm.beginTransaction();
        ft.add( R.id.fragment_container, f, tag );
        ft.commit();
    }

    protected Fragment getFragment( Class<?> clss ) {
        String tag = clss.getSimpleName();
        FragmentManager fm = getSupportFragmentManager();
        return fm.findFragmentByTag( tag );
    }

    protected View inflate( int resId ) {
        return mInflater.inflate( resId, null );
    }

    public Cursor getAirportDetails( String siteNumber ) {
        SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );

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

    protected Intent checkData() {
        Cursor c = mDbManager.getCurrentFromCatalog();
        if ( !c.moveToFirst() ) {
            Intent download = new Intent( this, DownloadActivity.class );
            download.putExtra( "MSG", "Please install the data before using the app" );
            c.close();
            return download;
        }

        boolean dtppFound = false;

        // Check if we have any expired data. If yes, then redirect to download activity
        do {
            String type = c.getString( c.getColumnIndex( Catalog.TYPE ) );
            if ( type.equals( "FADDS" ) ) {
                int version = c.getInt( c.getColumnIndex( Catalog.VERSION ) );
                if ( version < 64 ) {
                    Intent download = new Intent( this, DownloadActivity.class );
                    download.putExtra( "MSG", "This app version requires latest data update" );
                    c.close();
                    return download;
                }
            } else if ( type.equals( "DTPP" ) ) {
                dtppFound = true;
            }

            int age = c.getInt( c.getColumnIndex( "age" ) );
            if ( age <= 0 ) {
                // We have some expired data
                Intent download = new Intent( this, DownloadActivity.class );
                download.putExtra( "MSG", "One or more data items have expired" );
                c.close();
                return download;
            }

            // Try to make sure we can open the databases
            SQLiteDatabase db = mDbManager.getDatabase( type );
            if ( db == null ) {
                Intent download = new Intent( this, DownloadActivity.class );
                download.putExtra( "MSG", "Database is corrupted. Please delete and re-install" );
                c.close();
                return download;
            }
        } while ( c.moveToNext() );
        c.close();

        if ( !dtppFound ) {
            Intent download = new Intent( this, DownloadActivity.class );
            download.putExtra( "MSG", "Please download the current d-TPP database" );
            return download;
        }

        return null;
    }

    protected void showAirportTitle( Cursor c ) {
        View root = findViewById( R.id.airport_title_layout );
        TextView tv = (TextView) root.findViewById( R.id.airport_name );
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null || code.length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
        }
        String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        tv.setText( String.format( "%s - %s", code, name ) );
        tv = (TextView) root.findViewById( R.id.airport_info );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String type = DataUtils.decodeLandingFaclityType( siteNumber );
        String city = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
        String state = c.getString( c.getColumnIndex( States.STATE_NAME ) );
        if ( state == null ) {
            state = c.getString( c.getColumnIndex( Airports.ASSOC_COUNTY ) );
        }
        tv.setText( String.format( "%s, %s, %s", type, city, state ) );
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
        int elev_msl = c.getInt( c.getColumnIndex( Nav1.ELEVATION_MSL ) );
        tv = (TextView) root.findViewById( R.id.navaid_info2 );
        tv.setText( String.format( "%s, %s MSL elevation",
                use.equals( "Y" )? "Public use" : "Private use",
                FormatUtils.formatFeet( elev_msl ) ) );
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

    protected int getSelectorResourceForRow( int curRow, int totRows ) {
        // TODO: Add more states to the drawables
        if ( totRows == 1 ) {
            return R.drawable.row_selector;
        } else if ( curRow == 0 ) {
            return R.drawable.row_selector_top;
        } else if ( curRow == totRows-1 ) {
            return R.drawable.row_selector_bottom;
        } else {
            return R.drawable.row_selector_middle;
        }
    }

    protected View addClickableRow( LinearLayout layout, String label,
            Intent intent, int resid ) {
        return addClickableRow( layout, label, null, intent, resid );
    }

    protected View addClickableRow( LinearLayout layout, String label, String value,
            Intent intent, int resid ) {
        View row = addRow( layout, label+"...", value );
        row.setBackgroundResource( resid );
        row.setTag( intent );
        row.setOnClickListener( mOnClickListener );
        return row;
    }

    protected View addClickableRow( LinearLayout layout, String label1, String value1,
            String label2, String value2, Intent intent, int resid ) {
        View row = addRow( layout, label1, value1, label2, value2 );
        row.setBackgroundResource( resid );
        row.setTag( intent );
        row.setOnClickListener( mOnClickListener );
        return row;
    }

    protected View addRow( LinearLayout layout, String label ) {
        return addRow( layout, label, null );
    }

    protected View addRow( LinearLayout layout, String label, String value ) {
        LinearLayout row = (LinearLayout) inflate( R.layout.detail_row_item2 );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( label );
        tv = (TextView) row.findViewById( R.id.item_value );
        if ( value != null && value.length() > 0 ) {
            tv.setText( value );
        } else {
            tv.setVisibility( View.GONE );
        }
        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        return row;
    }

    protected View addRow( LinearLayout layout, String label, String value1, String value2 ) {
        LinearLayout row = (LinearLayout) inflate( R.layout.detail_row_item3 );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( label );
        tv = (TextView) row.findViewById( R.id.item_value );
        if ( value1 != null && value1.length() > 0 ) {
            tv.setText( value1 );
        } else {
            tv.setVisibility( View.GONE );
        }
        tv = (TextView) row.findViewById( R.id.item_extra_value );
        if ( value2 != null && value2.length() > 0 ) {
            tv.setText( value2 );
        } else {
            tv.setVisibility( View.GONE );
        }
        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        return row;
    }

    protected View addRow( LinearLayout layout, String label1, String value1,
            String label2, String value2 ) {
        LinearLayout row = (LinearLayout) inflate( R.layout.detail_row_item4 );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( label1 );
        tv = (TextView) row.findViewById( R.id.item_value );
        if ( value1 != null && value1.length() > 0 ) {
            tv.setText( value1 );
        } else {
            tv.setVisibility( View.GONE );
        }
        tv = (TextView) row.findViewById( R.id.item_extra_label );
        if ( label2 != null && label2.length() > 0 ) {
            tv.setText( label2 );
        } else {
            tv.setVisibility( View.GONE );
        }
        tv = (TextView) row.findViewById( R.id.item_extra_value );
        if ( value2 != null && value2.length() > 0 ) {
            tv.setText( value2 );
        } else {
            tv.setVisibility( View.GONE );
        }
        layout.addView( row, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
        return row;
    }

    protected View addPhoneRow( LinearLayout layout, String label, final String phone ) {
        View row = addRow( layout, label, phone );
        TextView tv = (TextView) row.findViewById( R.id.item_value );
        UiUtils.makeClickToCall( this, tv );
        return row;
    }

    protected void addBulletedRow( LinearLayout layout, String text ) {
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( UiUtils.convertDpToPx( this, 6 ), 2, 2, 2 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 12, 2 );
        tv.setText( text );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( LinearLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, new LayoutParams( LayoutParams.MATCH_PARENT, 1 ) );
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

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.mainmenu, menu );
        mRefreshItem = menu.findItem( R.id.menu_refresh );
        mRefreshDrawable = mRefreshItem.getIcon();
        return super.onCreateOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        switch ( item.getItemId() ) {
        case android.R.id.home:
            startHomeActivity();
            return true;
        case R.id.menu_search:
            onSearchRequested();
            return true;
        case R.id.menu_browse:
            Intent browse = new Intent( this, BrowseActivity.class );
            browse.putExtras( new Bundle() );
            startActivity( browse );
            return true;
        case R.id.menu_nearby:
            Intent nearby = new Intent( this, NearbyActivity.class );
            startActivity( nearby );
            return true;
        case R.id.menu_favorites:
            Intent favorites = new Intent( this, FavoritesActivity.class );
            startActivity( favorites );
            return true;
        case R.id.menu_download:
            Intent download = new Intent( this, DownloadActivity.class );
            startActivity( download );
            return true;
        case R.id.menu_settings:
            Intent settings = new Intent( this, PreferencesActivity.class  );
            startActivity( settings );
            return true;
        case R.id.menu_about:
            Intent about = new Intent( this, AboutActivity.class );
            startActivity( about );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

    protected void setActionBarTitle( Cursor c ) {
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
            String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
            title += " - "+name;
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

    protected void startHomeActivity() {
        ArrayList<String> fav = mDbManager.getAptFavorites();
        Class<?> clss;
        if ( fav.size() > 0 ) {
            clss = FavoritesActivity.class;
        } else {
            clss = BrowseActivity.class;
        }
        if ( getClass() != clss ) {
            // Start home activity if it is not the current activity
            Intent intent = new Intent( this, clss );
            intent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
            startActivity( intent );
        }
    }

}
