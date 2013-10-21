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

package com.nadmm.airports.afd;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.ResourceCursorAdapter;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.DownloadActivity;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.SectionedCursorAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public final class BrowseAirportsFragment extends ListFragmentBase {

    // Projection map for queries
    static private final HashMap<String, String> sCityMap;
    static {
        sCityMap = new HashMap<String, String>();
        sCityMap.put(BaseColumns._ID, BaseColumns._ID);
        sCityMap.put(Airports.SITE_NUMBER, Airports.SITE_NUMBER);
        sCityMap.put( Airports.ASSOC_CITY, Airports.ASSOC_CITY );
        sCityMap.put( Airports.FACILITY_NAME, Airports.FACILITY_NAME );
        sCityMap.put( Airports.ICAO_CODE,
                "IFNULL("+Airports.ICAO_CODE+", "+Airports.FAA_CODE+")"
                +" AS "+Airports.ICAO_CODE );
    }

    private SectionedCursorAdapter mAdapter;

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        Bundle args = getArguments();
        String stateCode = args.getString( States.STATE_CODE );
        String stateName = args.getString( States.STATE_NAME );
        setActionBarTitle( "Browse" );
        setActionBarSubtitle( stateName );
        getListView().setCacheColorHint( 0xffffffff );

        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_STANDARD );
        actionBar.setDisplayShowTitleEnabled( true );

        setBackgroundTask(new BrowseTask()).execute( stateCode, stateName );

        super.onActivityCreated( savedInstanceState );
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        mAdapter = new CityCursorAdapter( getActivity(), R.layout.browse_state_item, c );
        setActionBarSubtitle( String.format( Locale.US, "%s  (%d)",
                getSupportActionBar().getSubtitle(), c.getCount() ) );

        return mAdapter;
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        position = mAdapter.sectionedPositionToPosition( position );
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        Cursor c = adapter.getCursor();
        c.moveToPosition( position );
        // An airport was selected - Launch the detail view activity
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        Intent intent = new Intent( getActivity(), AirportActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        startActivity( intent );
    }

    private final class CityCursorAdapter extends SectionedCursorAdapter {

        public CityCursorAdapter( Context context, int layout, Cursor c ) {
            super( context, layout, c, R.layout.list_item_header );
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            // Browsing all airports in a state
            TextView tv;
            String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            String type = DataUtils.decodeLandingFaclityType( siteNumber );
            String icao = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
            tv = (TextView) view.findViewById( R.id.browse_airport_code );
            tv.setText( icao );
            String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
            tv = (TextView) view.findViewById( R.id.browse_airport_name );
            tv.setText( name+" "+type );
        }

        @Override
        public String getSectionName() {
            Cursor c = getCursor();
            return c.getString( c.getColumnIndex( Airports.ASSOC_CITY ) );
        }
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

}
