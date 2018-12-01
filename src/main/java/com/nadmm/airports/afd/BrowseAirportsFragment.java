/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2018 Nadeem Hasan <nhasan@nadmm.com>
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
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DownloadActivity;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.SectionedCursorAdapter;

import java.util.HashMap;
import java.util.Locale;

import androidx.cursoradapter.widget.CursorAdapter;

public class BrowseAirportsFragment extends ListFragmentBase {

    // Projection map for queries
    static private final HashMap<String, String> sStateMap;

    static {
        sStateMap = new HashMap<>();
        sStateMap.put( BaseColumns._ID, "max(" + BaseColumns._ID + ") AS " + BaseColumns._ID );
        sStateMap.put( DatabaseManager.Airports.ASSOC_STATE, DatabaseManager.Airports.ASSOC_STATE );
        sStateMap.put( DatabaseManager.States.STATE_NAME,
                "IFNULL(" + DatabaseManager.States.STATE_NAME + ", " + DatabaseManager.Airports.ASSOC_COUNTY + ")"
                        + " AS " + DatabaseManager.States.STATE_NAME );
        sStateMap.put( BaseColumns._COUNT, "count(*) AS " + BaseColumns._COUNT );
    }

    private static final int BROWSE_STATE_MODE = 0;
    private static final int BROWSE_AIRPORTS_MODE = 1;

    private static final String BROWSE_MODE = "BROWSE_MODE";

    private SectionedCursorAdapter mAdapter;
    private int mMode;
    private Parcelable mListState = null;
    private String mStateCode;
    private String mStateName;

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        if ( savedInstanceState != null && savedInstanceState.containsKey( BROWSE_MODE ) ) {
            mMode = savedInstanceState.getInt( BROWSE_MODE );
            if ( mMode == BROWSE_AIRPORTS_MODE ) {
                mStateCode = savedInstanceState.getString( DatabaseManager.Airports.ASSOC_STATE );
                mStateName = savedInstanceState.getString( DatabaseManager.States.STATE_NAME );
            }
        } else {
            mMode = BROWSE_STATE_MODE;
        }

        if ( mMode == BROWSE_STATE_MODE ) {
            setBackgroundTask( new BrowseStateTask( this ) ).execute();
        } else {
            setBackgroundTask( new BrowseAirportsTask( this ) ).execute( mStateCode, mStateName );
        }

        View view = getView();
        if ( view != null ) {
            view.setFocusableInTouchMode( true );
            view.requestFocus();
            view.setOnKeyListener( ( v, keyCode, event ) -> {
                if ( mMode == BROWSE_AIRPORTS_MODE
                        && keyCode == KeyEvent.KEYCODE_BACK
                        && event.getAction() == KeyEvent.ACTION_UP ) {
                    // Intercept back key to go back to state mode
                    mMode = BROWSE_STATE_MODE;
                    setAdapter( null );
                    setBackgroundTask( new BrowseStateTask( this ) ).execute();
                    return true;
                }
                return false;
            } );
        }
    }

    @Override
    public void onSaveInstanceState( Bundle outState ) {
        super.onSaveInstanceState( outState );

        outState.putInt( BROWSE_MODE, mMode );
        if ( mMode == BROWSE_AIRPORTS_MODE ) {
            outState.putString( DatabaseManager.Airports.ASSOC_STATE, mStateCode );
            outState.putString( DatabaseManager.States.STATE_NAME, mStateName );
        }
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        if ( mMode == BROWSE_STATE_MODE ) {
            mAdapter = new StateCursorAdapter( context, R.layout.browse_all_item, c );
        } else {
            mAdapter = new CityCursorAdapter( getActivity(), c );
        }
        return mAdapter;
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        position = mAdapter.sectionedPositionToPosition( position );
        CursorAdapter adapter = (CursorAdapter) getListAdapter();
        Cursor c = adapter.getCursor();
        c.moveToPosition( position );
        if ( mMode == BROWSE_STATE_MODE ) {
            mMode = BROWSE_AIRPORTS_MODE;
            mListState = getListView().onSaveInstanceState();
            setAdapter( null );
            mStateCode = c.getString( c.getColumnIndex( DatabaseManager.Airports.ASSOC_STATE ) );
            mStateName = c.getString( c.getColumnIndex( DatabaseManager.States.STATE_NAME ) );
            setBackgroundTask( new BrowseAirportsTask( this ) ).execute( mStateCode, mStateName );
        } else {
            String siteNumber = c.getString( c.getColumnIndex( DatabaseManager.Airports.SITE_NUMBER ) );
            Intent intent = new Intent( getActivity(), AirportActivity.class );
            intent.putExtra( DatabaseManager.Airports.SITE_NUMBER, siteNumber );
            startActivity( intent );
        }
    }

    private void setStateCursor( Cursor[] result ) {
        if ( result != null ) {
            setCursor( result[ 0 ] );
            if ( mListState != null ) {
                getListView().onRestoreInstanceState( mListState );
            }
        } else {
            Intent intent = new Intent( getActivity(), DownloadActivity.class );
            intent.putExtra( "MSG", "Please install the data before using the app" );
            startActivity( intent );
        }
    }

    private void setAirportsCursor( Cursor[] result ) {
        if ( result != null ) {
            setCursor( result[ 0 ] );
        } else {
            Intent intent = new Intent( getActivity(), DownloadActivity.class );
            intent.putExtra( "MSG", "Please install the data before using the app" );
            startActivity( intent );
        }
    }

    private final class StateCursorAdapter extends SectionedCursorAdapter {

        private StateCursorAdapter( Context context, int layout, Cursor c ) {
            super( context, layout, c, R.layout.list_item_header );
        }

        @Override
        public void bindView( View view, Context context, Cursor c ) {
            // Browsing all states
            String state_name = c.getString( c.getColumnIndex( DatabaseManager.States.STATE_NAME ) );
            int count = c.getInt( c.getColumnIndex( BaseColumns._COUNT ) );
            TextView tv = view.findViewById( R.id.browse_state_name );
            tv.setText( String.format( Locale.US, "%s (%d)", state_name, count ) );
        }

        @Override
        public String getSectionName() {
            Cursor c = getCursor();
            return c.getString( c.getColumnIndex( DatabaseManager.States.STATE_NAME ) ).substring( 0, 1 );
        }
    }

    private final class CityCursorAdapter extends SectionedCursorAdapter {

        AirportsCursorAdapter mAdapter;
        private CityCursorAdapter( Context context, Cursor c ) {
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
            return c.getString( c.getColumnIndex( DatabaseManager.Airports.ASSOC_CITY ) );
        }
    }

    private static class BrowseStateTask extends CursorAsyncTask<BrowseAirportsFragment> {

        private BrowseStateTask( BrowseAirportsFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( BrowseAirportsFragment fragment, String... params ) {
            SQLiteDatabase db = fragment.getDatabase( DatabaseManager.DB_FADDS );

            // Show all the states grouped by first letter
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( DatabaseManager.Airports.TABLE_NAME + " a LEFT OUTER JOIN "
                    + DatabaseManager.States.TABLE_NAME + " s" + " ON a." + DatabaseManager.Airports.ASSOC_STATE
                    + "=s." + DatabaseManager.States.STATE_CODE );
            builder.setProjectionMap( sStateMap );
            Cursor c = builder.query( db,
                    // String[] projectionIn
                    new String[]{ BaseColumns._ID,
                            DatabaseManager.Airports.ASSOC_STATE,
                            DatabaseManager.States.STATE_NAME,
                            BaseColumns._COUNT },
                    // String selection
                    null,
                    // String[] selectionArgs
                    null,
                    // String groupBy
                    DatabaseManager.States.STATE_NAME,
                    // String having
                    null,
                    // String sortOrder
                    DatabaseManager.States.STATE_NAME );

            return new Cursor[]{ c };
        }

        @Override
        protected boolean onResult( BrowseAirportsFragment fragment, Cursor[] result ) {
            fragment.setStateCursor( result );
            return false;
        }

    }

    private static class BrowseAirportsTask extends CursorAsyncTask<BrowseAirportsFragment> {

        private BrowseAirportsTask( BrowseAirportsFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( BrowseAirportsFragment fragment, String... params ) {
            SQLiteDatabase db = fragment.getDatabase( DatabaseManager.DB_FADDS );
            String stateCode = params[ 0 ];
            String stateName = params[ 1 ];

            String selection = "(" + DatabaseManager.Airports.ASSOC_STATE + " <> '' AND " + DatabaseManager.Airports.ASSOC_STATE
                    + "=?) OR (" + DatabaseManager.Airports.ASSOC_STATE + " = '' AND " + DatabaseManager.Airports.ASSOC_COUNTY + "=?)";
            String[] selectionArgs = new String[]{ stateCode, stateName };

            Cursor c = AirportsCursorHelper.query( db, selection, selectionArgs, null, null,
                    DatabaseManager.Airports.ASSOC_CITY + ", " + DatabaseManager.Airports.FACILITY_NAME, null );

            return new Cursor[]{ c };
        }

        @Override
        protected boolean onResult( BrowseAirportsFragment fragment, Cursor[] result ) {
            fragment.setAirportsCursor( result );
            return false;
        }


    }
}
