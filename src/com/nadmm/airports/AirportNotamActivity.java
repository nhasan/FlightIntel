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
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;

import com.nadmm.airports.DatabaseManager.Airports;

public class AirportNotamActivity extends NotamActivityBase {

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        requestWindowFeature( Window.FEATURE_INDETERMINATE_PROGRESS );
        setContentView( R.layout.wait_msg );

        Intent intent = getIntent();
        String siteNumber = intent.getStringExtra( Airports.SITE_NUMBER );

        NotamTask task = new NotamTask();
        task.execute( siteNumber );
    }

    private final class NotamTask extends AsyncTask<String, Void, Cursor> {

        String mIcaoCode;

        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility( true );
        }

        @Override
        protected Cursor doInBackground( String... params ) {
            String siteNumber = params[ 0 ];
            Cursor apt = mDbManager.getAirportDetails( siteNumber );

            mIcaoCode = apt.getString( apt.getColumnIndex( Airports.ICAO_CODE ) );
            if ( mIcaoCode == null || mIcaoCode.length() == 0 ) {
                String faaCode = apt.getString( apt.getColumnIndex( Airports.FAA_CODE ) );
                mIcaoCode = "K"+faaCode;
            }

            getNotams( mIcaoCode );

            return apt;
        }
        
        @Override
        protected void onPostExecute( Cursor result ) {
            setProgressBarIndeterminateVisibility( false );

            View view = inflate( R.layout.airport_notam_view );
            setContentView( view );
            mMainLayout = (LinearLayout) view.findViewById( R.id.notam_top_layout );

            // Title
            showAirportTitle( mMainLayout, result );

            showNotams( mIcaoCode );

            // Cleanup cursor
            result.close();
        }

    }

}
