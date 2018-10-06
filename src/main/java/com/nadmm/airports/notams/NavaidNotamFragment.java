/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2017 Nadeem Hasan <nhasan@nadmm.com>
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
        Bundle args = getArguments();
        String navaidId = args.getString( DatabaseManager.Nav1.NAVAID_ID );
        String navaidType = args.getString( DatabaseManager.Nav1.NAVAID_TYPE );
        setBackgroundTask( new NavaidNotamTask() ).execute( navaidId, navaidType );

        super.onActivityCreated( savedInstanceState );
    }

    private final class NavaidNotamTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            Cursor[] result = new Cursor[ 1 ];
            String navaidId = params[ 0 ];
            String navaidType = params[ 1 ];

            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( DatabaseManager.Nav1.TABLE_NAME + " a LEFT OUTER JOIN " + DatabaseManager.States.TABLE_NAME + " s"
                    + " ON a." + DatabaseManager.Nav1.ASSOC_STATE + "=s." + DatabaseManager.States.STATE_CODE );
            Cursor c = builder.query( db, new String[]{ "*" },
                    DatabaseManager.Nav1.NAVAID_ID + "=? AND " + DatabaseManager.Nav1.NAVAID_TYPE + "=?",
                    new String[]{ navaidId, navaidType }, null, null, null, null );
            result[ 0 ] = c;
            if ( !c.moveToFirst() ) {
                return null;
            }

            return result;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            Cursor nav1 = result[ 0 ];

            String id = nav1.getString( nav1.getColumnIndex( DatabaseManager.Nav1.NAVAID_ID ) );
            String icaoCode = "K" + id;
            setActionBarTitle( icaoCode );
            showNavaidTitle( nav1 );
            getNotams( icaoCode, "navaid" );

            return true;
        }

    }

}
