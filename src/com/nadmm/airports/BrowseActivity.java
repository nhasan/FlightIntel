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

import java.util.HashMap;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.States;

public class BrowseActivity extends ActivityBase {

    static private final String BUNDLE_KEY_SITE_NUMBER = "site_number";
    static private final String BUNDLE_KEY_STATE_CODE = "state_code";
    static private final String BUNDLE_KEY_STATE_NAME = "state_name";

    // Projection maps for queries
    static private final HashMap<String, String> sStateMap = buildStateMap();
    static private final HashMap<String, String> sCityMap = buildCityMap();

    private ListView mListView;
    private BrowseCursorAdapter mListAdapter;

    static HashMap<String, String> buildStateMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put( BaseColumns._ID, "max("+BaseColumns._ID+") AS "+BaseColumns._ID );
        map.put( Airports.ASSOC_STATE, Airports.ASSOC_STATE );
        map.put( States.STATE_NAME,
                 "IFNULL("+States.STATE_NAME+", "+Airports.ASSOC_COUNTY+")"
                 +" AS "+States.STATE_NAME );
        return map;
    }

    static HashMap<String, String> buildCityMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put( BaseColumns._ID, BaseColumns._ID );
        map.put( Airports.SITE_NUMBER, Airports.SITE_NUMBER );
        map.put( Airports.ASSOC_CITY, Airports.ASSOC_CITY );
        map.put( Airports.FACILITY_NAME, Airports.FACILITY_NAME );
        map.put( Airports.ICAO_CODE,
                "IFNULL("+Airports.ICAO_CODE+", "+Airports.FAA_CODE+")"
                +" AS "+Airports.ICAO_CODE );
        return map;
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        setContentView( R.layout.airport_list_view );
        mListView = (ListView) findViewById( R.id.list_view );
        mListView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view,
                    int position, long id ) {
                onListItemClick( position );
            }

        } );

        Intent intent = getIntent();
        Bundle extra = intent.getExtras();
        if ( extra == null ) {
            // Activity was instantiated by system directly
            extra = new Bundle();
        }

        if ( !extra.containsKey( BUNDLE_KEY_SITE_NUMBER ) ) {
            // Show browse list
            String stateCode = extra.getString( BUNDLE_KEY_STATE_CODE );
            if ( stateCode == null ) {
                setTitle( "Browse Airports - All Locations" );
            } else {
                String stateName = extra.getString( BUNDLE_KEY_STATE_NAME );
                setTitle( "Browse Airports - "+stateName );
                registerForContextMenu( mListView );
            }
            BrowseTask task = new BrowseTask();
            task.execute( extra );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ( mListAdapter != null ) {
            Cursor c = mListAdapter.getCursor();
            c.close();
        }
    }

    private final class BrowseTask extends AsyncTask<Bundle, Void, Cursor> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor doInBackground( Bundle... params ) {
            Cursor c = null;
            DatabaseManager dbManager = DatabaseManager.instance( BrowseActivity.this );
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

            Bundle extra = params[ 0 ];
            if ( !extra.containsKey( BUNDLE_KEY_STATE_CODE ) ) {
                // Show all the states grouped by first letter
                builder.setTables( Airports.TABLE_NAME+" a LEFT OUTER JOIN "
                        +States.TABLE_NAME+" s"+" ON a."+Airports.ASSOC_STATE
                        +"=s."+States.STATE_CODE );
                builder.setProjectionMap( sStateMap );
                c = builder.query( db,
                        // String[] projectionIn
                        new String[] { Airports._ID, 
                                       Airports.ASSOC_STATE,
                                       States.STATE_NAME },
                        // String selection
                        null,
                        // String[] selectionArgs
                        null,
                        // String groupBy
                        States.STATE_NAME,
                        // String having
                        null,
                        // String sortOrder
                        States.STATE_NAME );
            } else {
                // Show all the airports in the selected state grouped by city
                builder.setTables( Airports.TABLE_NAME );
                String state_code = extra.getString( BUNDLE_KEY_STATE_CODE );
                String state_name = extra.getString( BUNDLE_KEY_STATE_NAME );
                builder.setProjectionMap( sCityMap );
                String selection = "("+Airports.ASSOC_STATE+" <> '' AND "+Airports.ASSOC_STATE
                        +"=?) OR ("+Airports.ASSOC_STATE+" = '' AND "+Airports.ASSOC_COUNTY+"=?)";
                String[] selectionArgs = new String[] { state_code, state_name };

                c = builder.query( db,
                        // String[] projectionIn
                        new String[] { Airports._ID,
                                       Airports.SITE_NUMBER,
                                       Airports.ASSOC_CITY,
                                       Airports.FACILITY_NAME,
                                       Airports.ICAO_CODE }, 
                        // String selection
                        selection,
                        // String[] selectionArgs
                        selectionArgs,
                        // String groupBy
                        null,
                        // String having
                        null,                        
                        // String sortOrder
                        Airports.ASSOC_CITY+", "+Airports.FACILITY_NAME );
            }
            return c;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            if ( c.getColumnIndex( Airports.SITE_NUMBER ) == -1 ) {
                 mListAdapter = new BrowseCursorAdapter( BrowseActivity.this,
                        R.layout.browse_all_item, c, R.id.browse_all_section,
                        BrowseCursorAdapter.STATE_MODE );
            } else {
                mListAdapter = new BrowseCursorAdapter( BrowseActivity.this,
                        R.layout.browse_state_item, c, R.id.browse_state_section,
                        BrowseCursorAdapter.CITY_MODE );
            }

            mListView.setAdapter( mListAdapter );
            mListView.setVisibility( View.VISIBLE );
            TextView tv = (TextView) findViewById( android.R.id.empty );
            tv.setVisibility( View.GONE );
            setProgressBarIndeterminateVisibility( false );
        }

    }

    private final class BrowseCursorAdapter extends SectionedCursorAdapter {

        public static final int STATE_MODE = 0;
        public static final int CITY_MODE = 1;

        private int mMode;

        public BrowseCursorAdapter( Context context, int layout, Cursor c, int section,
                int mode ) {
            super( context, layout, c, section );
            mMode = mode;
        }

        @Override
        public String getSectionName() {
            Cursor c = getCursor();
            String name;
            if ( mMode == STATE_MODE ) {
                // Section name is the first character of the state postal code
                name = c.getString( c.getColumnIndex( States.STATE_NAME ) ).substring( 0, 1 );
            } else {
                // Section name is name of the city
                name = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
            }

            return name;
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            if ( mMode == STATE_MODE ) {
                // Browsing all states
                String state_name = c.getString( c.getColumnIndex( States.STATE_NAME ) );
                TextView tv = (TextView) view.findViewById( R.id.browse_state_name );
                tv.setText( state_name );
            } else {
                // Browsing all airports in a state
                TextView tv;
                String icao = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
                tv = (TextView) view.findViewById( R.id.browse_airport_code );
                tv.setText( icao );
                String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
                tv = (TextView) view.findViewById( R.id.browse_airport_name );
                tv.setText( name );
            }
        }

    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        MenuItem browse = menu.findItem( R.id.menu_browse );
        browse.setEnabled( false );
        return super.onPrepareOptionsMenu( menu );
    }

    protected void onListItemClick( int position ) {
        Cursor c = mListAdapter.getCursor();
        c.moveToPosition( position );
        if ( c.getColumnIndex( Airports.SITE_NUMBER ) == -1 ) {
            // User clicked on a state
            Intent browse = new Intent( this, BrowseActivity.class );
            Bundle extra = new Bundle();
            String state_code = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
            String state_name = c.getString( c.getColumnIndex( States.STATE_NAME ) );
            extra.putString( BUNDLE_KEY_STATE_CODE, state_code );
            extra.putString( BUNDLE_KEY_STATE_NAME, state_name );
            browse.putExtras( extra );
            // Start this activity again with state parameter
            startActivity( browse );
        } else {
            // An airport was selected - Launch the detail view activity
            String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            Intent intent = new Intent( this, AirportDetailsActivity.class );
            intent.putExtra( Airports.SITE_NUMBER, siteNumber );
            startActivity( intent );
        }
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );

        Cursor c = mListAdapter.getCursor();
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null || code.length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );
        }
        String facilityName = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );

        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.airport_list_context_menu, menu );
        menu.setHeaderTitle( code+" - "+facilityName );

        // Show either "Add" or "Remove" entry depending on the context
        if ( mDbManager.isFavoriteAirport( siteNumber ) ) {
            menu.removeItem( R.id.menu_add_favorites );
        } else {
            menu.removeItem( R.id.menu_remove_favorites );
        }
    }

    @Override
    public boolean onContextItemSelected( MenuItem item ) {
        Cursor c = mListAdapter.getCursor();
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );

        switch ( item.getItemId() ) {
            case R.id.menu_add_favorites:
                mDbManager.addToFavorites( siteNumber );
                break;
            case R.id.menu_remove_favorites:
                mDbManager.removeFromFavorites( siteNumber );
                break;
            case R.id.menu_view_details:
                Intent intent = new Intent( this, AirportDetailsActivity.class );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                startActivity( intent );
                break;
            default:
        }
        return super.onContextItemSelected( item );
    }

    protected void showErrorMessage( String msg )
    {
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( msg )
            .setTitle( "Download Error" )
            .setPositiveButton( "Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            } );
        AlertDialog alert = builder.create();
        alert.show();
    }

}
