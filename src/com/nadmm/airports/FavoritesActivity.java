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

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Favorites;
import com.nadmm.airports.DatabaseManager.States;

public class FavoritesActivity extends ListActivity {

    private final String[] mQueryColumns = new String[] {
            BaseColumns._ID,
            "a."+Airports.SITE_NUMBER,
            Airports.ICAO_CODE,
            Airports.FAA_CODE,
            Airports.FACILITY_NAME,
            Airports.ASSOC_CITY,
            Airports.ASSOC_STATE,
            Airports.FACILITY_TYPE,
            Airports.FUEL_TYPES,
            Airports.UNICOM_FREQS,
            Airports.ELEVATION_MSL,
            Airports.STATUS_CODE,
            States.STATE_NAME,
         };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setTitle( "Favorite Airports" );
        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
        registerForContextMenu( getListView() );

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
            SQLiteDatabase db = dbManager.getUserDataDb();
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Favorites.TABLE_NAME );
            Cursor c = builder.query( db, new String[] { Favorites.SITE_NUMBER }, 
                    null, null, null, null, null );
            if ( !c.moveToFirst() ) {
                c.close();
                return null;
            }

            // Build the list of favorites
            String favorites = "";
            do {
                if ( favorites.length() > 0 ) {
                    favorites += ", ";
                }
                favorites += "'"+c.getString( c.getColumnIndex( Favorites.SITE_NUMBER ) )+"'";
            } while ( c.moveToNext() );
            c.close();

            // Query for the favorite airports
            builder = new SQLiteQueryBuilder();
            builder.setTables( Airports.TABLE_NAME+" a INNER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Airports.ASSOC_STATE+"=s."+States.STATE_CODE );
            String selection = "a."+Airports.SITE_NUMBER+" in ("+favorites+")";
            db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
            c = builder.query( db, mQueryColumns, selection, null, null, null, 
                    Airports.FACILITY_NAME );

            return c;
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            setProgressBarIndeterminateVisibility( false );
            ListAdapter adapter = new AirportsCursorAdapter( FavoritesActivity.this, c );
            setListAdapter( adapter );
        }

    }

    @Override
    public void onCreateContextMenu( ContextMenu menu, View v, ContextMenuInfo menuInfo ) {
        super.onCreateContextMenu( menu, v, menuInfo );

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor c = ((CursorAdapter) getListAdapter()).getCursor();
        int pos = c.getPosition();
        c.moveToPosition( info.position );
        String name = c.getString( c.getColumnIndex( Airports.FACILITY_NAME ) );
        c.moveToPosition( pos );

        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.airport_list_context_menu, menu );
        menu.setHeaderTitle( name );
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
        Cursor c = ((CursorAdapter) getListAdapter()).getCursor();
        int pos = c.getPosition();
        c.moveToPosition( info.position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        c.moveToPosition( pos );

        switch ( item.getItemId() ) {
            case R.id.menu_add_favorites:
                DatabaseManager.instance().addToFavorites( siteNumber );
            default:
                return super.onContextItemSelected( item );
        }
    }

}
