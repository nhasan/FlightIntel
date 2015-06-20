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

import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.DatabaseManager.Remarks;
import com.nadmm.airports.DatabaseManager.Runways;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;
import com.nadmm.airports.utils.DataUtils;

public final class OwnershipFragment extends FragmentBase {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.ownership_detail_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        getActivityBase().onFragmentStarted( this );

        Bundle args = getArguments();
        String siteNumber = args.getString( Airports.SITE_NUMBER );
        setBackgroundTask( new FragmentTask() ).execute( siteNumber );
    }

    protected void showDetails( Cursor[] result ) {
        Cursor apt = result[ 0 ];

        showAirportTitle( apt );
        showOwnershipType( result );
        showOwnerInfo( result );
        showManagerInfo( result );
        showRemarks( result );

        setContentShown( true );
    }

    protected void showOwnershipType( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_ownership_type_layout );
        String ownership = DataUtils.decodeOwnershipType(
                apt.getString( apt.getColumnIndex( Airports.OWNERSHIP_TYPE ) ) );
        String use = DataUtils.decodeFacilityUse(
                apt.getString( apt.getColumnIndex( Airports.FACILITY_USE ) ) );
        addSimpleRow( layout, ownership+" / "+use );
    }

    protected void showOwnerInfo( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        String text;
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_owner_layout );
        text = apt.getString( apt.getColumnIndex( Airports.OWNER_NAME ) );
        addSimpleRow( layout, text );
        text = apt.getString( apt.getColumnIndex( Airports.OWNER_ADDRESS ) );
        addSimpleRow( layout, text );
        text = apt.getString( apt.getColumnIndex( Airports.OWNER_CITY_STATE_ZIP ) );
        addSimpleRow( layout, text );
        layout = (LinearLayout) findViewById( R.id.detail_owner_phone_layout );
        text = apt.getString( apt.getColumnIndex( Airports.OWNER_PHONE ) );
        if ( text.length() > 0 ) {
            addPhoneRow( layout, text );
        } else {
            layout.setVisibility( View.GONE );
            findViewById( R.id.detail_owner_phone_label ).setVisibility( View.GONE );
        }
    }

    protected void showManagerInfo( Cursor[] result ) {
        Cursor apt = result[ 0 ];
        String text;
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_manager_layout );
        text = apt.getString( apt.getColumnIndex( Airports.MANAGER_NAME ) );
        addSimpleRow( layout, text );
        text = apt.getString( apt.getColumnIndex( Airports.MANAGER_ADDRESS ) );
        addSimpleRow( layout, text );
        text = apt.getString( apt.getColumnIndex( Airports.MANAGER_CITY_STATE_ZIP ) );
        addSimpleRow( layout, text );

        layout = (LinearLayout) findViewById( R.id.detail_manager_phone_layout );
        text = apt.getString( apt.getColumnIndex( Airports.MANAGER_PHONE ) );
        if ( text.length() > 0 ) {
            addPhoneRow( layout, text );
        } else {
            layout.setVisibility( View.GONE );
            findViewById( R.id.detail_manager_phone_label ).setVisibility( View.GONE );
        }
    }

    protected void showRemarks( Cursor[] result ) {
        Cursor rmk = result[ 1 ];
        if ( !rmk.moveToFirst() ) {
            return;
        }
        LinearLayout layout = (LinearLayout) findViewById( R.id.detail_remarks_layout );
        layout.setVisibility( View.VISIBLE );
        do {
            String remark = rmk.getString( rmk.getColumnIndex( Remarks.REMARK_TEXT ) );
            addBulletedRow( layout, remark );
        } while ( rmk.moveToNext() );
    }

    private final class FragmentTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            Cursor[] cursors = new Cursor[ 2 ];

            cursors[ 0 ] = getAirportDetails( siteNumber );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Remarks.TABLE_NAME );
            cursors[ 1 ] = builder.query( db, new String[] { Remarks.REMARK_TEXT },
                    Runways.SITE_NUMBER+"=? "
                    +"AND "+Remarks.REMARK_NAME+" in ('A11', 'A12', 'A13', 'A14', 'A15', 'A16')",
                    new String[] { siteNumber }, null, null, null, null );

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            showDetails( result );
            return true;
        }

    }

}
