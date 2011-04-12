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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.nadmm.airports.DatabaseManager.Airports;

public class FavoritesActivity extends ListActivity {

    private DatabaseManager mDbManager = null;
    private AirportsCursorAdapter mListAdapter = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setTitle( "Favorite Airports" );
        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
        registerForContextMenu( getListView() );

        mDbManager = DatabaseManager.instance( this );

        mListAdapter = new AirportsCursorAdapter( FavoritesActivity.this, null );
        setListAdapter( mListAdapter );

        getListView().setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view,
                    int position, long id ) {
                Cursor c = mListAdapter.getCursor();
                c.moveToPosition( position );
                String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
                Intent intent = new Intent( FavoritesActivity.this, AirportDetailsActivity.class );
                intent.putExtra( Airports.SITE_NUMBER, siteNumber );
                startActivity( intent );
            }
        } );
   }

    @Override
    protected void onPause() {
        super.onPause();
        if ( mListAdapter != null ) {
            Cursor c = mListAdapter.getCursor();
            c.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getFavorites();
    }

    protected void getFavorites() {
        // Get the favorites list
        FavoritesTask task = new FavoritesTask();
        task.execute( (Void[]) null );
    }

    private final class FavoritesTask extends AsyncTask<Void, Void, Cursor> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor doInBackground( Void... params ) {
            ArrayList<String> favorites = mDbManager.getFavorites();
            String selection = "";
            for (String site_number : favorites ) {
                if ( selection.length() > 0 ) {
                    selection += ", ";
                }
                selection += "'"+site_number+"'";
            };

            // Query for the favorite airports
            selection = "a."+Airports.SITE_NUMBER+" in ("+selection+")";
            Cursor c = AirportsCursorHelper.query( FavoritesActivity.this, selection, 
                    null, null, null, Airports.FACILITY_NAME, null );

            return c;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            mListAdapter.changeCursor( c );
            setProgressBarIndeterminateVisibility( false );
        }

    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor c = mListAdapter.getCursor();
        int pos = c.getPosition();
        c.moveToPosition( info.position );
        String facility_name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        c.moveToPosition( pos );

        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.airport_list_context_menu, menu );
        menu.setHeaderTitle( facility_name );
        menu.removeItem( R.id.menu_add_favorites );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.mainmenu, menu );
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu ) {
        MenuItem browse = menu.findItem( R.id.menu_favorites );
        browse.setEnabled( false );
        return super.onPrepareOptionsMenu( menu );
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
            browse.putExtra( BrowseActivity.EXTRA_BUNDLE, new Bundle() );
            startActivity( browse );
            return true;
        case R.id.menu_nearby:
            Intent nearby = new Intent( this, NearbyActivity.class );
            startActivity( nearby );
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
    public boolean onContextItemSelected( MenuItem item ) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor c = mListAdapter.getCursor();
        int pos = c.getPosition();
        c.moveToPosition( info.position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        c.moveToPosition( pos );

        switch ( item.getItemId() ) {
            case R.id.menu_remove_favorites:
                mDbManager.removeFromFavorites( siteNumber );
                getFavorites();
                break;
            default:
        }
        return super.onContextItemSelected( item );
    }

}
