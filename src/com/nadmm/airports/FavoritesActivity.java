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
import android.content.ActivityNotFoundException;
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
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.nadmm.airports.DatabaseManager.Airports;

public class FavoritesActivity extends ListActivity {

    AirportsCursorAdapter mListAdapter = null;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setTitle( "Favorite Airports" );
        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
        registerForContextMenu( getListView() );
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
            DatabaseManager dbManager = DatabaseManager.instance();
            ArrayList<String> favorites = dbManager.getFavorites();
            String selection = "";
            for (String site_number : favorites ) {
                if ( selection.length() > 0 ) {
                    selection += ", ";
                }
                selection += "'"+site_number+"'";
            };

            // Query for the favorite airports
            selection = "a."+Airports.SITE_NUMBER+" in ("+selection+")";
            Cursor c = AirportsCursorHelper.query( selection, null, null, null, 
                    Airports.FACILITY_NAME );

            return c;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            if ( mListAdapter == null ) {
                mListAdapter = new AirportsCursorAdapter( FavoritesActivity.this, c );
            } else {
                mListAdapter.changeCursor( c );
                mListAdapter.notifyDataSetChanged();
            }
            setListAdapter( mListAdapter );
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
                DatabaseManager.instance().removeFromFavorites( siteNumber );
                getFavorites();
                break;
            default:
        }
        return super.onContextItemSelected( item );
    }

}
