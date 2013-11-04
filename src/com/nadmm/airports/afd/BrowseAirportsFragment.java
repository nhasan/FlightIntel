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
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.ListView;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.DownloadActivity;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.SectionedCursorAdapter;

import java.util.Locale;

public final class BrowseAirportsFragment extends ListFragmentBase {

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
        mAdapter = new CityCursorAdapter( getActivity(), c );
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

        AirportsCursorAdapter mAdapter;
        public CityCursorAdapter( Context context, Cursor c ) {
            super( context, R.layout.airport_list_item, c, R.layout.list_item_header );
            mAdapter = new AirportsCursorAdapter( context, c );
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            mAdapter.bindView( view, context, c );
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

            String stateCode = params[ 0 ];
            String stateName = params[ 1 ];

            String selection = "("+Airports.ASSOC_STATE+" <> '' AND "+Airports.ASSOC_STATE
                    +"=?) OR ("+Airports.ASSOC_STATE+" = '' AND "+Airports.ASSOC_COUNTY+"=?)";
            String[] selectionArgs = new String[] { stateCode, stateName };

            Cursor c = AirportsCursorHelper.query( db, selection, selectionArgs, null, null,
                    Airports.ASSOC_CITY+", "+Airports.FACILITY_NAME, null );

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
