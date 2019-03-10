/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2018 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.notams;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Nav1;
import com.nadmm.airports.data.DatabaseManager.States;
import com.nadmm.airports.utils.CursorAsyncTask;

public class NavaidNotamFragment extends NotamFragmentBase {

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState ) {
        View view = inflater.inflate( R.layout.navaid_notam_view, container, false );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Bundle args = getArguments();
        String navaidId = args.getString( Nav1.NAVAID_ID );
        String navaidType = args.getString( Nav1.NAVAID_TYPE );
        setBackgroundTask( new NavaidNotamTask( this ) ).execute( navaidId, navaidType );
    }

    private Cursor[] doQuery( String navaidId, String navaidType ) {
        SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Nav1.TABLE_NAME
                + " a LEFT OUTER JOIN "
                + States.TABLE_NAME
                + " s ON a."
                + Nav1.ASSOC_STATE
                + "=s."
                + States.STATE_CODE );
        Cursor c = builder.query( db, new String[]{ "*" },
                Nav1.NAVAID_ID + "=? AND " + Nav1.NAVAID_TYPE + "=?",
                new String[]{ navaidId, navaidType }, null, null, null, null );
        if ( !c.moveToFirst() ) {
            return null;
        }

        return new Cursor[] { c };
    }

    private void showNotam( Cursor c ) {
        String id = c.getString( c.getColumnIndex( Nav1.NAVAID_ID ) );
        String icaoCode = "K" + id;
        setActionBarTitle( icaoCode );
        showNavaidTitle( c );
        getNotams( icaoCode, "navaid" );
    }

    private static class NavaidNotamTask extends CursorAsyncTask<NavaidNotamFragment> {

        private NavaidNotamTask( NavaidNotamFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( NavaidNotamFragment fragment, String... params ) {
            String navaidId = params[ 0 ];
            String navaidType = params[ 1 ];
            return fragment.doQuery( navaidId, navaidType );
        }

        @Override
        protected boolean onResult( NavaidNotamFragment fragment, Cursor[] result ) {
            fragment.showNotam( result[ 0 ] );
            return true;
        }

    }

}
