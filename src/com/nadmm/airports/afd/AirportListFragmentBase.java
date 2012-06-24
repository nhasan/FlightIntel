/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.aeronav.BatchDownloadActivity;
import com.nadmm.airports.ListFragmentBase;
import com.nadmm.airports.R;

public class AirportListFragmentBase extends ListFragmentBase {

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        setHasOptionsMenu( true );
        super.onCreate( savedInstanceState );
    }

    @Override
    protected CursorAdapter newListAdapter( Context context, Cursor c ) {
        return new AirportsCursorAdapter( context, c );
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position ) {
        Cursor c = (Cursor) l.getItemAtPosition( position );
        String siteNumber = c.getString( c.getColumnIndex( Airports.SITE_NUMBER ) );
        Intent intent = new Intent( getActivity(), AirportDetailsActivity.class );
        intent.putExtra( Airports.SITE_NUMBER, siteNumber );
        startActivity( intent );
    }

    @Override
    public void onPrepareOptionsMenu( Menu menu ) {
        menu.findItem( R.id.menu_charts_download ).setVisible( true );
        super.onPrepareOptionsMenu( menu );
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        // Handle item selection
        switch ( item.getItemId() ) {
        case R.id.menu_charts_download:
            Intent intent = new Intent( getActivity(), BatchDownloadActivity.class );
            getActivity().startActivity( intent );
            return true;
        default:
            return super.onOptionsItemSelected( item );
        }
    }

}
