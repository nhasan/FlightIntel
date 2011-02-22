/*
 * Airports for Android
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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.view.View;
import android.view.Window;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;

public class BrowseActivity extends ListActivity {

    static public final String EXTRA_BUNDLE = "bundle";

    static private final String BUNDLE_KEY_STATE = "state";
    static private final String BUNDLE_KEY_SITE_NUMBER = "site_number";

    // Projection maps for queries
    static private final HashMap<String, String> sStateMap = buildStateMap();
    static private final HashMap<String, String> sCityMap = buildCityMap();

    private DatabaseManager mDbManager;

    static HashMap<String, String> buildStateMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put( BaseColumns._ID, "max("+BaseColumns._ID+") AS "+BaseColumns._ID );
        map.put( Airports.ASSOC_STATE, Airports.ASSOC_STATE );
        return map;
    }

    static HashMap<String, String> buildCityMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put( BaseColumns._ID, BaseColumns._ID );
        map.put( Airports.ASSOC_CITY, Airports.ASSOC_CITY );
        map.put( Airports.FACILITY_NAME, Airports.FACILITY_NAME );
        map.put( Airports.ICAO_CODE,
                "IFNULL("+Airports.ICAO_CODE+", "+Airports.FAA_CODE+")"
                +" AS "+Airports.ICAO_CODE );
        map.put( Airports.SITE_NUMBER, Airports.SITE_NUMBER );
        return map;
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mDbManager = new DatabaseManager( this );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        Intent intent = getIntent();
        Bundle extra = intent.getBundleExtra( EXTRA_BUNDLE );
        if ( !extra.containsKey( BUNDLE_KEY_SITE_NUMBER ) ) {
            // Show browse list
            String state = extra.getString( BUNDLE_KEY_STATE );
            if ( state == null ) {
                setTitle( getTitle()+" - All States" );
            } else {
                setTitle( getTitle()+" - "+DataUtils.getStateName( state ) );
            }
            QueryBrowseTask task = new QueryBrowseTask();
            task.execute( extra );
        }
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position, long id ) {
        CursorAdapter adapter = (CursorAdapter) l.getAdapter();
        Cursor c = adapter.getCursor();
        if ( c.getColumnIndex( Airports.SITE_NUMBER ) == -1 ) {
            // User clicked on a state
            String state = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
            Intent browse = new Intent( this, BrowseActivity.class );
            Bundle extra = new Bundle();
            extra.putString( BUNDLE_KEY_STATE, state );
            browse.putExtra( BrowseActivity.EXTRA_BUNDLE, extra );
            // Start this activity again with state parameter
            startActivity( browse );
        } else {
            // An airport was selected - Launch the detail view activity
        }
    }

    private final class QueryBrowseTask extends AsyncTask<Bundle, Void, Cursor> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor doInBackground( Bundle... params ) {
            Cursor cursor;
            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Airports.TABLE_NAME );

            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences( BrowseActivity.this );
            String type = prefs.getString( PreferencesActivity.KEY_SEARCH_AIRPORT_TYPES, "ALL" );

            Bundle extra = params[ 0 ];
            if ( !extra.containsKey( BUNDLE_KEY_STATE ) ) {
                // Show all the states with airports alphabetically
                builder.setProjectionMap( sStateMap );
                String selection = Airports.ASSOC_STATE+"<>''";
                String[] selectionArgs = null;
                if ( !type.equals( "ALL" ) ) {
                    selection += " AND "+Airports.FACILITY_USE+"=?";
                    selectionArgs = new String[] { type };
                }
                cursor = builder.query( db,
                        // String[] projectionIn
                        new String[] { Airports._ID, Airports.ASSOC_STATE },
                        // String selection
                        selection,
                        // String[] selectionArgs
                        selectionArgs,
                        // String groupBy
                        Airports.ASSOC_STATE,
                        // String having
                        null,
                        // String sortOrder
                        Airports.ASSOC_STATE );
            } else {
                // A state was selected, list all the airports in the state grouped by city
                String state = extra.getString( BUNDLE_KEY_STATE );
                builder.setProjectionMap( sCityMap );
                String selection = Airports.ASSOC_STATE+"=?";
                String[] selectionArgs = null;
                if ( !type.equals( "ALL" ) ) {
                    selection += " AND "+Airports.FACILITY_USE+"=?";
                    selectionArgs = new String[] { state, type };
                } else {
                    selectionArgs = new String[] { state };
                }
                cursor = builder.query( db,
                        // String[] projectionIn
                        new String[] { Airports._ID,
                                       Airports.ASSOC_CITY,
                                       Airports.FACILITY_NAME,
                                       Airports.ICAO_CODE,
                                       Airports.SITE_NUMBER }, 
                        // String selection
                        selection,
                        // String[] selectionArgs
                        selectionArgs,
                        // String groupBy
                        null,
                        // String having
                        null,                        
                        // String sortOrder
                        Airports.ASSOC_CITY );
            }
            return cursor;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            setProgressBarIndeterminateVisibility( false );
            if ( c.getColumnIndex( Airports.SITE_NUMBER ) == -1 ) {
                BrowseCursorAdapter adapter = new BrowseCursorAdapter( BrowseActivity.this,
                        R.layout.browse_all_item, c, R.id.browse_all_section );
                BrowseActivity.this.setListAdapter( adapter );
            } else {
                BrowseCursorAdapter adapter = new BrowseCursorAdapter( BrowseActivity.this,
                        R.layout.browse_state_item, c, R.id.browse_state_section );
                BrowseActivity.this.setListAdapter( adapter );
            }
        }
    }

    private final class BrowseCursorAdapter extends SectionedCursorAdapter {

        public BrowseCursorAdapter( Context context, int layout, Cursor c, int section ) {
            super( context, layout, c, section );
        }

        @Override
        public String getSectionName() {
            Cursor c = getCursor();
            String name;
            if ( c.getColumnIndex( Airports.SITE_NUMBER ) == -1 ) {
                // Section name is the first character of the state postal code
                name = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) ).substring( 0, 1 );
            } else {
                // Section name is name of the city
                name = c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
            }

            return name;
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            if ( c.getColumnIndex( Airports.SITE_NUMBER ) == -1 ) {
                String state = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
                TextView tv = (TextView) view.findViewById( R.id.browse_state_name );
                tv.setText( DataUtils.getStateName( state ) );
            } else {
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
}
