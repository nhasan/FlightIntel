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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.data.DatabaseManager.Remarks;
import com.nadmm.airports.data.DatabaseManager.Runways;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;

public final class RemarksFragment extends FragmentBase {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.remarks_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        setActionBarTitle( "Remarks", "" );

        Bundle args = getArguments();
        String siteNumber = args.getString( Airports.SITE_NUMBER );
        setBackgroundTask( new RemarksTask() ).execute( siteNumber );
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        showAirportTitle( apt );
        showRemarksDetails( result );

        setFragmentContentShown( true );
    }

    protected void showRemarksDetails( Cursor[] result ) {
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_remarks_layout );
        Cursor rmk = result[ 1 ];
        if ( rmk.moveToFirst() ) {
            do {
                String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
                addBulletedRow( layout, remark );
            } while ( rmk.moveToNext() );
        } else {
            addBulletedRow( layout, "No airport remarks available" );
        }
    }

    private final class RemarksTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 2 ];

            Cursor apt = getAirportDetails( siteNumber );
            cursors[ 0 ] = apt;

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            cursors[ 1 ] = builder.query( db,
                    new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=? "
                    +"AND substr("+Remarks.REMARK_NAME+", 1, 2) not in ('A3', 'A4', 'A5', 'A6') "
                    +"AND substr("+Remarks.REMARK_NAME+", 1, 3) not in ('A23', 'A17', 'A81')"
                    +"AND "+Remarks.REMARK_NAME
                    +" not in ('E147', 'A3', 'A11', 'A12', 'A13', 'A14', 'A15', 'A16', 'A17', "
                    +"'A24', 'A70', 'A75', 'A82')",
                    new String[] { siteNumber }, null, null, Remarks.REMARK_NAME, null );

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            showDetails( result );
            return true;
        }

    }

}
