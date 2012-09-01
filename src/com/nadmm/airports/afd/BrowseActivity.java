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

package com.nadmm.airports.afd;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.ActivityBase;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.DownloadActivity;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.SectionedCursorAdapter;

public final class BrowseActivity extends ActivityBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.fragment_activity_layout );

        Bundle args = getIntent().getExtras();
        if ( args == null ) {
            args = new Bundle();
        }
        addFragment( BrowseFragment.class, args );
    }

    public static class BrowseFragment extends ListFragmentBase {

        // Projection maps for queries
        static private final HashMap<String, String> sStateMap = buildStateMap();
        static private final HashMap<String, String> sCityMap = buildCityMap();

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

        private final class BrowseTask extends CursorAsyncTask {

            @Override
            protected Cursor[] doInBackground( String... params ) {
                if ( getActivity() == null ) {
                    cancel( false );
                    return null;
                }

                SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
                if ( db == null ) {
                    return null;
                }

                Cursor c = null;
                String stateCode = params[ 0 ];
                String stateName = params[ 1 ];

                if ( stateCode == null ) {
                    // Show all the states grouped by first letter
                    SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
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
                    SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                    builder.setTables( Airports.TABLE_NAME );
                    builder.setProjectionMap( sCityMap );
                    String selection = "("+Airports.ASSOC_STATE+" <> '' AND "+Airports.ASSOC_STATE
                            +"=?) OR ("+Airports.ASSOC_STATE+" = '' AND "+Airports.ASSOC_COUNTY+"=?)";
                    String[] selectionArgs = new String[] { stateCode, stateName };

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

                return new Cursor[] { c };
            }

            @Override
            protected boolean onResult( Cursor[] result ) {
                if ( result != null ) {
                    setCursor( result[ 0 ] );
                } else {
                    Intent intent = new Intent( getActivity(), DownloadActivity.class );
                    intent.putExtra( "MSG", "Please install the data before using the app" );
                    startActivity( intent );
                }
                return false;
            }

        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            Bundle args = getArguments();
            // Show browse list
            String stateCode = args.getString( States.STATE_CODE );
            String stateName = args.getString( States.STATE_NAME );
            if ( stateCode == null ) {
                setActionBarSubtitle( "All Locations" );
            } else {
                setActionBarSubtitle( stateName );
            }
            setBackgroundTask( new BrowseTask() ).execute( stateCode, stateName );

            super.onActivityCreated( savedInstanceState );
        }

        @Override
        protected CursorAdapter newListAdapter( Context context, Cursor c ) {
            BrowseCursorAdapter adapter;
            if ( c.getColumnIndex( Airports.SITE_NUMBER ) == -1 ) {
                 adapter = new BrowseCursorAdapter( getActivity(),
                        R.layout.browse_all_item, c, R.id.browse_all_section,
                        BrowseCursorAdapter.STATE_MODE );
            } else {
                adapter = new BrowseCursorAdapter( getActivity(),
                        R.layout.browse_state_item, c, R.id.browse_state_section,
                        BrowseCursorAdapter.CITY_MODE );
                setActionBarSubtitle( String.format( "%s  (%d)",
                        getSupportActionBar().getSubtitle(), c.getCount() ) );
            }
            return adapter;
        }

        @Override
        protected void onListItemClick( ListView l, View v, int position ) {
            BrowseCursorAdapter adapter = (BrowseCursorAdapter) getListAdapter();
            Cursor c = adapter.getCursor();
            c.moveToPosition( position );
            if ( c.getColumnIndex( Airports.SITE_NUMBER ) == -1 ) {
                // User clicked on a state
                Intent browse = new Intent( getActivity(), BrowseActivity.class );
                Bundle extra = new Bundle();
                String state_code = c.getString( c.getColumnIndex( Airports.ASSOC_STATE ) );
                String state_name = c.getString( c.getColumnIndex( States.STATE_NAME ) );
                extra.putString( States.STATE_CODE, state_code );
                extra.putString( States.STATE_NAME, state_name );
                browse.putExtras( extra );
                // Start this activity again with state parameter
                startActivity( browse );
            } else {
                // An airport was selected - Launch the detail view activity
                String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
                Intent intent = new Intent( getActivity(), AirportDetailsActivity.class );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                startActivity( intent );
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

    }

}