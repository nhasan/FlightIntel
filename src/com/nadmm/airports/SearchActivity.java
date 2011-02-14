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
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

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

        Intent intent = getIntent();
        
        if ( Intent.ACTION_SEARCH.equals( intent.getAction() ) ) {
            String query = intent.getStringExtra( SearchManager.QUERY );
            Log.i( TAG, "query="+query );
            showResults( query );
        }
    }

    @Override
    protected void onNewIntent( Intent intent ) {
        setIntent( intent );
        handleIntent( intent );
    }

    private void handleIntent( Intent intent ) {
        if ( Intent.ACTION_SEARCH.equals( intent.getAction() ) ) {
          String query = intent.getStringExtra( SearchManager.QUERY );
        }
    }

    private void showResults( String query ) {
        Cursor cursor = managedQuery( AirportsProvider.CONTENT_URI, null, null, 
                new String[] { query }, null );

        if ( cursor == null ) {
            mTextView.setText( R.string.search_not_found );
        } else {
            int count = cursor.getCount();
            startManagingCursor( cursor );
            mTextView.setText( getResources().getQuantityString( R.plurals.search_entry_found, 
                    count, new Object[] { count } ) );
            SimpleCursorAdapter adapter = new SimpleCursorAdapter( this,
                    android.R.layout.two_line_list_item,
                    cursor,
                    new String[] { SearchManager.SUGGEST_COLUMN_TEXT_1, 
                                   SearchManager.SUGGEST_COLUMN_TEXT_2 },
                    new int[] {android.R.id.text1, android.R.id.text2} );
    
            mListView.setAdapter( adapter );
        }
    }
}
