/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2014 Nadeem Hasan <nhasan@nadmm.com>
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

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.R;
import com.nadmm.airports.providers.AirportsProvider;

public class SearchActivity extends AfdActivityBase {

    private CursorAdapter mListAdapter = null;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        handleIntent( getIntent() );
    }

    @Override
    protected View getContentView() {
        return null;
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
            startAirportActivity( siteNumber );
            finish();
        }
    }

    @SuppressWarnings("deprecation")
    private void showResults( String query ) {
        Cursor c = managedQuery( AirportsProvider.CONTENT_URI, null, null,
                new String[] { query }, null );
        int count = c.getCount();
        if ( c.getCount() == 1 ) {
            // If there was only one result, start the airport activity directly instead of
            // showing search results in a list view
            c.moveToFirst();
            String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            startAirportActivity( siteNumber );
            finish();
        }

        startManagingCursor( c );

        setContentView( R.layout.list_view_layout );
        ListView listView = (ListView) findViewById( android.R.id.list );
        listView.setOnItemClickListener( new OnItemClickListener() {

            @Override
            public void onItemClick( AdapterView<?> parent, View view,
                    int position, long id ) {
                Cursor c = mListAdapter.getCursor();
                String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
                startAirportActivity( siteNumber );
            }

        } );

        mListAdapter = new AirportsCursorAdapter( this, c );
        listView.setAdapter( mListAdapter );
        getSupportActionBar().setSubtitle(
                getResources().getQuantityString( R.plurals.search_entry_found,
                count, count, query ) );
    }

    private void startAirportActivity( String siteNumber ) {
        Intent apt = new Intent( this, AirportActivity.class );
        apt.putExtra( Airports.SITE_NUMBER, siteNumber );
        startActivity( apt );
    }

}
