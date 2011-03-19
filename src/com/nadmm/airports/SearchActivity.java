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
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

public class SearchActivity extends Activity {

    private static final String TAG = SearchActivity.class.getSimpleName();
    private TextView mTextView;
    private ListView mListView;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.airport_list_view );
        mTextView = (TextView) findViewById( R.id.message );
        mListView = (ListView) findViewById( R.id.list_view );

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
        Cursor c = managedQuery( AirportsProvider.CONTENT_URI, null, null, 
                new String[] { query }, null );
        if ( c != null ) {
            startManagingCursor( c );
            int count = c.getCount();
            mTextView.setText( getResources().getQuantityString( R.plurals.search_entry_found, 
                    count, new Object[] { count } ) );
            mListView.setAdapter( new AirportsCursorAdapter( this, c ) );
        } else {
            mTextView.setText( R.string.search_not_found );
            mListView.setAdapter( null );
        }
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
            try {
                Intent browse = new Intent( this, BrowseActivity.class );
                browse.putExtra( BrowseActivity.EXTRA_BUNDLE, new Bundle() );
                startActivity( browse );
            } catch ( ActivityNotFoundException e ) {
            }
            return true;
        case R.id.menu_nearby:
            try {
                Intent nearby = new Intent( this, NearbyActivity.class );
                startActivity( nearby );
            } catch ( ActivityNotFoundException e ) {
            }
            return true;
        case R.id.menu_download:
            try {
                Intent download = new Intent( this, DownloadActivity.class );
                startActivity( download );
            } catch ( ActivityNotFoundException e ) {
            }
            return true;
        case R.id.menu_settings:
            try {
                Intent settings = new Intent( this, PreferencesActivity.class  );
                startActivity( settings );
            } catch ( ActivityNotFoundException e ) {
            }
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

}
