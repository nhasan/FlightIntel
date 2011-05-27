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

import java.util.ArrayList;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.nadmm.airports.DatabaseManager.Airports;

public class SearchActivity extends Activity {

    private TextView mHeader;
    private TextView mEmpty;
    private ListView mListView;
    private ArrayList<String> mFavorites;

    private DatabaseManager mDbManager = null;
    private CursorAdapter mListAdapter = null;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.airport_list_view );
        mHeader = (TextView) getLayoutInflater().inflate( R.layout.list_header, null );
        mEmpty = (TextView) findViewById( android.R.id.empty );
        mFavorites = null;

        mDbManager = DatabaseManager.instance( this );

        mListView = (ListView) findViewById( R.id.list_view );
        mListView.addHeaderView( mHeader );
        registerForContextMenu( mListView );
        mListView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view,
                    int position, long id ) {
                Cursor c = mListAdapter.getCursor();
                // Subtract 1 from position to account for header item
                c.moveToPosition( position-1 );
                String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
                Intent intent = new Intent( SearchActivity.this, AirportDetailsActivity.class );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                startActivity( intent );
            }

        } );

        handleIntent( getIntent() );
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFavorites = mDbManager.getFavorites();
    }

    @Override
    protected void onNewIntent( Intent intent ) {
        setIntent( intent );
        handleIntent( intent );
    }

    private void handleIntent( Intent intent ) {
        if ( Intent.ACTION_SEARCH.equals( intent.getAction() ) ) {
            // Perform the search using user provided query string
            String query = intent.getStringExtra( SearchManager.QUERY );
            showResults( query );
        } else if ( Intent.ACTION_VIEW.equals( intent.getAction() ) ) {
            // User clicked on a suggestion
            Bundle extra = intent.getExtras();
            String siteNumber = extra.getString( SearchManager.EXTRA_DATA_KEY );
            Intent view = new Intent( this, AirportDetailsActivity.class );
            view.putExtra( Airports.SITE_NUMBER, siteNumber );
            startActivity( view );
            finish();
        }
    }

    private void showResults( String query ) {
        Cursor c = managedQuery( AirportsProvider.CONTENT_URI, null, null, 
                new String[] { query }, null );
        startManagingCursor( c );
        int count = c.getCount();
        mListAdapter = new AirportsCursorAdapter( this, c );
        mListView.setAdapter( mListAdapter );
        mEmpty.setVisibility( View.GONE );
        mListView.setVisibility( View.VISIBLE );
        mHeader.setText( getResources().getQuantityString( R.plurals.search_entry_found, 
                count, new Object[] { count, query } ) );
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
        default:
            return super.onOptionsItemSelected( item );
        }
    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor c = mListAdapter.getCursor();
        int pos = c.getPosition();
        c.moveToPosition( info.position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        String code = c.getString( c.getColumnIndex( Airports.ICAO_CODE ) );
        if ( code == null || code.length() == 0 ) {
            code = c.getString( c.getColumnIndex( Airports.FAA_CODE ) );            
        }
        String facilityName = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        c.moveToPosition( pos );

        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.airport_list_context_menu, menu );
        menu.setHeaderTitle( code+" - "+facilityName );

        // Show either "Add" or "Remove" entry depending on the context
        if ( mFavorites.contains( siteNumber ) ) {
            menu.removeItem( R.id.menu_add_favorites );
        } else {
            menu.removeItem( R.id.menu_remove_favorites );
        }
    }

    @Override
    public boolean onContextItemSelected( MenuItem item ) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor c = mListAdapter.getCursor();
        int pos = c.getPosition();
        c.moveToPosition( info.position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        c.moveToPosition( pos );

        switch ( item.getItemId() ) {
            case R.id.menu_add_favorites:
                mDbManager.addToFavorites( siteNumber );
                mFavorites.add( siteNumber );
                break;
            case R.id.menu_remove_favorites:
                mDbManager.removeFromFavorites( siteNumber );
                mFavorites.remove( siteNumber );
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

}
