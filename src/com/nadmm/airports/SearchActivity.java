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

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager.Airports;

public class SearchActivity extends Activity {

    private static final String TAG = SearchActivity.class.getSimpleName();
    private TextView mTextView;
    private ListView mListView;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.search_list_view );
        mTextView = (TextView) findViewById( R.id.search_msg );
        mListView = (ListView) findViewById( R.id.search_list );

        handleIntent( getIntent() );
    }

    @Override
    protected void onNewIntent( Intent intent ) {
        setIntent( intent );
        handleIntent( intent );
    }

    private void handleIntent( Intent intent ) {
        if ( Intent.ACTION_SEARCH.equals( intent.getAction() ) ) {
            String query = intent.getStringExtra( SearchManager.QUERY );
            Log.i( TAG, "query="+query );
            showResults( query );
        } else if ( Intent.ACTION_VIEW.equals( intent.getAction() ) ) {
            // User clicked on a suggestion
        }
    }

    private void showResults( String query ) {
        Cursor cursor = managedQuery( AirportsProvider.CONTENT_URI, null, null, 
                new String[] { query }, null );
        if ( cursor != null ) {
            startManagingCursor( cursor );
            int count = cursor.getCount();
            mTextView.setText( getResources().getQuantityString( R.plurals.search_entry_found, 
                    count, new Object[] { count } ) );
            SearchCursorAdapter adapter = new SearchCursorAdapter( this, cursor );
            mListView.setAdapter( adapter );
        } else {
            mTextView.setText( R.string.search_not_found );
            mListView.setAdapter( null );
        }
    }

    private final class SearchCursorAdapter extends ResourceCursorAdapter {

        public SearchCursorAdapter( Context context, Cursor c ) {
            super( context, R.layout.airport_list_item, c );
        }

        @Override
        public void bindView( View view, Context context, Cursor cursor ) {
            TextView tv;
            String name = cursor.getString( cursor.getColumnIndex( Airports.FACILITY_NAME ) );
            tv = (TextView) view.findViewById( R.id.facility_name );
            tv.setText( name );
            tv = (TextView) view.findViewById( R.id.facility_id );
            String code = cursor.getString( cursor.getColumnIndex( Airports.ICAO_CODE ) );
            if ( code == null || code.trim().length() == 0 ) {
                code = cursor.getString( cursor.getColumnIndex( Airports.FAA_CODE ) );
            }
            tv.setText( code );
            String city = cursor.getString( cursor.getColumnIndex( Airports.ASSOC_CITY ) );
            String state = cursor.getString( cursor.getColumnIndex( Airports.ASSOC_STATE ) );
            tv = (TextView) view.findViewById( R.id.location );
            tv.setText( city+", "+DataUtils.getStateName( state ) );
            tv = (TextView) view.findViewById( R.id.other_info );
            String type = cursor.getString( cursor.getColumnIndex( Airports.FACILITY_TYPE ) );
            String use = cursor.getString( cursor.getColumnIndex( Airports.FACILITY_USE ) );
            String ownership = cursor.getString( cursor.getColumnIndex( Airports.OWNERSHIP_TYPE ) );
            tv.setText( type+", "+Airports.decodeOwnershipType( ownership )
                    +", "+Airports.decodeFacilityUse( use ) );
        }
    }
}
