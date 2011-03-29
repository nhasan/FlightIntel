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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;

import com.nadmm.airports.DatabaseManager.Airports;

public class AirportDetailsActivity extends Activity {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        AirportDetailsTask task = new AirportDetailsTask();
        task.execute( siteNumber );
    }

    private final class AirportDetailsTask extends AsyncTask<String, Void, Cursor[]> {

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor[] doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor[] cursors = new Cursor[ 1 ];
            DatabaseManager dbManager = DatabaseManager.instance();
            SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables( Airports.TABLE_NAME );
            cursors[ 0 ] = builder.query( db, new String[] { "*" }, Airports.SITE_NUMBER+"=?",
                    new String[] { siteNumber }, null, null, null, null );

            return cursors;
        }
        
        @Override
        protected void onPostExecute( Cursor[] result ) {
            super.onPostExecute( result );
            setProgressBarIndeterminateVisibility( false );
        }

    }

}
