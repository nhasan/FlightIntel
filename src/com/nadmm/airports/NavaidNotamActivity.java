/*
 * FlightIntel for Pilots
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

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;

import com.nadmm.airports.DatabaseManager.Nav1;
import com.nadmm.airports.DatabaseManager.States;
import com.nadmm.airports.utils.UiUtils;

public class NavaidNotamActivity extends NotamActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Intent intent = getIntent();
        String navaidId = intent.getStringExtra( Nav1.NAVAID_ID );
        String navaidType = intent.getStringExtra( Nav1.NAVAID_TYPE );
        NotamTask task = new NotamTask();
        task.execute( navaidId, navaidType );
    }

    private final class NotamTask extends CursorAsyncTask {

        String mIcaoCode;

        @Override
        protected Cursor[] doInBackground( String... params ) {
            Cursor[] result = new Cursor[ 1 ];
            String navaidId = params[ 0 ];
            String navaidType = params[ 1 ];
            mIcaoCode = "K"+navaidId;

            SQLiteDatabase db = mDbManager.getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Nav1.TABLE_NAME+" a LEFT OUTER JOIN "+States.TABLE_NAME+" s"
                    +" ON a."+Nav1.ASSOC_STATE+"=s."+States.STATE_CODE );
            Cursor c = builder.query( db, new String[] { "*" },
                    Nav1.NAVAID_ID+"=? AND "+Nav1.NAVAID_TYPE+"=?",
                    new String[] { navaidId, navaidType }, null, null, null, null );
            result[ 0 ] = c;
            if ( !c.moveToFirst() ) {
                return null;
            }

            getNotams( mIcaoCode );

            return result;
        }
        
        @Override
        protected void onResult( Cursor[] result ) {
            setContentView( R.layout.navaid_notam_view );

            if ( result == null ) {
                UiUtils.showToast( getApplicationContext(), "Navaid not found" );
                finish();
                return;
            }

            showNavaidTitle( result[ 0 ] );
            showNotams( mIcaoCode );
        }

    }

}
