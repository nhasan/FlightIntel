/*
 * FlightIntel for Pilots
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Catalog;
import com.nadmm.airports.DatabaseManager.Nav1;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.utils.DataUtils;

public class ActivityBase extends Activity {

    protected DatabaseManager mDbManager;
    private LayoutInflater mInflater;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        mDbManager = DatabaseManager.instance( this );
        mInflater = getLayoutInflater();
    }

    protected View inflate( int resource ) {
        return mInflater.inflate( resource, null );
    }

    protected Intent checkData() {
        Cursor c = mDbManager.getCurrentFromCatalog();
        if ( !c.moveToFirst() ) {
            c.close();
            Intent download = new Intent( this, DownloadActivity.class );
            download.putExtra( "MSG", "Please install the data before using the app" );
            c.close();
            return download;
        }

        int version = c.getInt( c.getColumnIndex( Catalog.VERSION ) );
        if ( version < 62 ) {
            c.close();
            Intent download = new Intent( this, DownloadActivity.class );
            download.putExtra( "MSG", "ATTENTION: The app version requires latest data update" );
            c.close();
            return download;
        }

        // Check if we have any expired data. If yes, then redirect to download activity
        do {
            int age = c.getInt( c.getColumnIndex( "age" ) );
            if ( age <= 0 ) {
                // We have some expired data
                c.close();
                Intent download = new Intent( this, DownloadActivity.class );
                download.putExtra( "MSG", "One or more data items have expired" );
                c.close();
                return download;
            }
        } while ( c.moveToNext() );

        c.close();

        return null;
    }

    protected void showAirportTitle( View root, Cursor c ) {
        TextView tv = (TextView) root.findViewById( R.id.airport_name );
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null || code.length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
        }
        String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        String title = code + " - " + name;
        tv.setText( title );
        tv = (TextView) root.findViewById( R.id.airport_info );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String type = DataUtils.decodeLandingFaclityType( siteNumber );
        String city = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
        String state = c.getString( c.getColumnIndex( States.STATE_NAME ) );
        if ( state == null ) {
            state = c.getString( c.getColumnIndex( Airports.ASSOC_COUNTY ) );
        }
        String info = type+", "+city+", "+state;
        tv.setText( info );
        tv = (TextView) root.findViewById( R.id.airport_info2 );
        int distance = c.getInt( c.getColumnIndex( Airports.DISTANCE_FROM_CITY_NM ) );
        String dir = c.getString( c.getColumnIndex( Airports.DIRECTION_FROM_CITY ) );
        String status = c.getString( c.getColumnIndex( Airports.STATUS_CODE ) );
        tv.setText( DataUtils.decodeStatus( status )+", "
                +String.valueOf( distance )+" miles "+dir+" of city center" );
        tv = (TextView) root.findViewById( R.id.airport_info3 );
        int elev_msl = c.getInt( c.getColumnIndex( Airports.ELEVATION_MSL ) );
        String info2 = String.valueOf( elev_msl )+"' MSL elevation, ";
        int tpa_agl = c.getInt( c.getColumnIndex( Airports.PATTERN_ALTITUDE_AGL ) );
        String est = "";
        if ( tpa_agl == 0 ) {
            tpa_agl = 1000;
            est = " (est.)";
        }
        info2 += String.valueOf( elev_msl+tpa_agl)+"' MSL TPA"+est;
        tv.setText( info2 );

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
                    mDbManager.addToFavorites( siteNumber );
                    Toast.makeText( ActivityBase.this, "Added to favorites list",
                            Toast.LENGTH_LONG ).show();
                } else {
                    mDbManager.removeFromFavorites( siteNumber );
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

    protected void showNavaidTitle( View root, Cursor c ) {
        String id = c.getString( c.getColumnIndex( Nav1.NAVAID_ID ) );
        String name = c.getString( c.getColumnIndex( Nav1.NAVAID_NAME ) );
        String type = c.getString( c.getColumnIndex( Nav1.NAVAID_TYPE ) );
        TextView tv = (TextView) root.findViewById( R.id.navaid_name );
        tv.setText( id+" - "+name+" "+type );
        String city = c.getString( c.getColumnIndex( Nav1.ASSOC_CITY ) );
        String state = c.getString( c.getColumnIndex( States.STATE_NAME ) );        
        tv = (TextView) root.findViewById( R.id.navaid_info );
        tv.setText( city+", "+state );
        String use = c.getString( c.getColumnIndex( Nav1.PUBLIC_USE ) );
        int elev_msl = c.getInt( c.getColumnIndex( Nav1.ELEVATION_MSL ) );
        String info2 = use.equals( "Y" )? "Public use" : "Private use";
        info2 += ", ";
        info2 += String.valueOf( elev_msl )+"' MSL elevation";
        tv = (TextView) root.findViewById( R.id.navaid_info2 );
        tv.setText( info2 );
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

    protected void makeClickToCall( TextView tv ) {
        if ( getPackageManager().hasSystemFeature( PackageManager.FEATURE_TELEPHONY ) ) {
            tv.setOnClickListener( new OnClickListener() {

                @Override
                public void onClick( View v ) {
                    TextView tv = (TextView) v;
                    Intent intent = new Intent( Intent.ACTION_CALL,
                            Uri.parse( "tel:"+tv.getText().toString() ) );
                    startActivity( intent );
                }

            } );
        }
    }

    protected int getSelectorResourceForRow( int curRow, int totRows ) {
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

    protected void addRow( TableLayout table, String label, String text ) {
        RelativeLayout row = (RelativeLayout) mInflater.inflate( R.layout.airport_detail_item, null );
        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( label );
        tv = (TextView) row.findViewById( R.id.item_value );
        tv.setText( text );
        table.addView( row, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addRow( TableLayout table, String label, Pair<String, String> values ) {
        RelativeLayout layout = (RelativeLayout) inflate( R.layout.airport_detail_item );
        TextView tv = (TextView) layout.findViewById( R.id.item_label );
        tv.setText( label );
        tv = (TextView) layout.findViewById( R.id.item_value );
        tv.setText( values.first );
        tv = (TextView) layout.findViewById( R.id.item_extra_value );
        if ( values.second.length() > 0 ) {
            tv.setText( values.second );
            tv.setVisibility( View.VISIBLE );
        }
        table.addView( layout, new TableLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addClickableRow( TableLayout table, String label, String value,
            final Intent intent, int resid ) {
        LinearLayout row = (LinearLayout) mInflater.inflate( R.layout.clickable_detail_item, null );
        row.setBackgroundResource( resid );

        TextView tv = (TextView) row.findViewById( R.id.item_label );
        tv.setText( label );
        if ( value != null && value.length() > 0 ) {
            tv = (TextView) row.findViewById( R.id.item_value );
            tv.setText( value );
        }
        row.setOnClickListener( new OnClickListener() {

            @Override
            public void onClick( View v ) {
                startActivity( intent );
            }

        } );

        table.addView( row, new TableLayout.LayoutParams( 
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addBulletedRow( LinearLayout layout, String remark ) {
        LinearLayout innerLayout = new LinearLayout( this );
        innerLayout.setOrientation( LinearLayout.HORIZONTAL );
        TextView tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 10, 2, 2, 2 );
        tv.setText( "\u2022 " );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f ) );
        tv = new TextView( this );
        tv.setGravity( Gravity.LEFT );
        tv.setPadding( 2, 2, 12, 2 );
        tv.setText( remark );
        innerLayout.addView( tv, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f ) );
        layout.addView( innerLayout, new LinearLayout.LayoutParams(
                LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );
    }

    protected void addSeparator( LinearLayout layout ) {
        View separator = new View( this );
        separator.setBackgroundColor( Color.LTGRAY );
        layout.addView( separator, new LayoutParams( LayoutParams.FILL_PARENT, 1 ) );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.mainmenu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
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

}
