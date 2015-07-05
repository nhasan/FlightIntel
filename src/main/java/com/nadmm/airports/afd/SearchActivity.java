/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2015 Nadeem Hasan <nhasan@nadmm.com>
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
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.ListView;

import com.nadmm.airports.FragmentActivityBase;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.providers.AirportsProvider;

public class SearchActivity extends FragmentActivityBase {

    SearchFragment mFragment;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        mFragment = (SearchFragment) addFragment( SearchFragment.class, null );

        handleIntent();
    }

    @Override
    protected void onNewIntent( Intent intent ) {
        setIntent( intent );
        handleIntent();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();
        if ( Intent.ACTION_SEARCH.equals( action ) ) {
            // Perform the search using user provided query string
            String query = intent.getStringExtra( SearchManager.QUERY );
            showResults( query );
        } else if ( Intent.ACTION_VIEW.equals( action ) ) {
            // User clicked on a suggestion
            Bundle extra = intent.getExtras();
            String siteNumber = extra.getString( SearchManager.EXTRA_DATA_KEY );
            Intent apt = new Intent( this, AirportActivity.class );
            apt.putExtra( Airports.SITE_NUMBER, siteNumber );
            startActivity( apt );
            finish();
        }
    }

    @SuppressWarnings("deprecation")
    private void showResults( String query ) {
        Cursor c = managedQuery( AirportsProvider.CONTENT_URI, null, null,
                new String[]{ query }, null );
        if ( c.getCount() == 1 ) {
            // If there was only one result, start the airport activity directly instead of
            // showing search results in a list view
            c.moveToFirst();
            String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
            c.close();
            Intent apt = new Intent( this, AirportActivity.class );
            apt.putExtra( Airports.SITE_NUMBER, siteNumber );
            startActivity( apt );
            finish();
        }

        startManagingCursor( c );
        mFragment.setSearchCursor( c );
    }

    public static class SearchFragment extends ListFragmentBase {

        private Cursor mCursor;

        @Override
        protected CursorAdapter newListAdapter( Context context, Cursor c ) {
            return new AirportsCursorAdapter( context, c );
        }

        @Override
        protected void onListItemClick( ListView l, View v, int position ) {
            String siteNumber = mCursor.getString( mCursor.getColumnIndex( Airports.SITE_NUMBER ) );
            Intent apt = new Intent( getActivity(), AirportActivity.class );
            apt.putExtra( Airports.SITE_NUMBER, siteNumber );
            startActivity( apt );
        }

        @Override
        public void onActivityCreated( Bundle savedInstanceState ) {
            super.onActivityCreated( savedInstanceState );

            if ( mCursor != null ) {
                setCursor( mCursor );
            }
        }

        public void setSearchCursor( Cursor c ) {
            mCursor = c;
            if ( getActivity() != null && getView() != null ) {
                setCursor( mCursor );
            }
        }

    }

}
