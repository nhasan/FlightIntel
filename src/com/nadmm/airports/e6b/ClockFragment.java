/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2013 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.e6b;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.Airports;
import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.R;
import com.nadmm.airports.utils.CursorAsyncTask;

public class ClockFragment extends FragmentBase {

    private final Handler mHandler = new Handler();
    private String mHome;
    private String mHomeTzId;
    private DateFormat mFormat = new SimpleDateFormat( "HH:mm:ss", Locale.US );

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        return inflate( R.layout.e6b_clock_layout );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences( getActivity() );
        mHome = prefs.getString( PreferencesActivity.KEY_HOME_AIRPORT, "" );
        setBackgroundTask( new ClockTask() ).execute();
    }

    private final class ClockTask extends CursorAsyncTask {

        @Override
        protected Cursor[] doInBackground( String... params ) {
            Cursor[] cursors = new Cursor[ 1 ];

            if ( mHome != null ) {
                SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
                SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables( Airports.TABLE_NAME );
                Cursor c = builder.query( db,
                        new String[] {
                            Airports.SITE_NUMBER,
                            Airports.TIMEZONE_ID
                        },
                        Airports.FAA_CODE+"=? OR "+Airports.ICAO_CODE+"=?",
                        new String[] { mHome, mHome }, null, null, null, null );
    
                cursors[ 0 ] = c;
            }

            return cursors;
        }

        @Override
        protected boolean onResult( Cursor[] result ) {
            Cursor c = result[ 0 ];
            if ( c != null && c.moveToFirst() ) {
                mHomeTzId = c.getString( c.getColumnIndex( Airports.TIMEZONE_ID ) );
            }
            if ( mHomeTzId == null || mHomeTzId.length() == 0 ) {
                TextView tv = (TextView) findViewById( R.id.home_time_label );
                tv.setVisibility( View.GONE );
                tv = (TextView) findViewById( R.id.home_time_value );
                tv.setVisibility( View.GONE );
            }
            updateTime();
            return true;
        }

        protected void updateTime() {
            Date now = new Date();

            TimeZone utcTz = TimeZone.getTimeZone( "GMT" );
            mFormat.setTimeZone( utcTz );
            TextView tv = (TextView) findViewById( R.id.utc_time_value );
            tv.setText( mFormat.format( now )+" UTC" );

            TimeZone localTz = TimeZone.getDefault();
            mFormat.setTimeZone( localTz );
            tv = (TextView) findViewById( R.id.local_time_value );
            tv.setText( mFormat.format( now )
                    +" "+localTz.getDisplayName( localTz.inDaylightTime( now ), TimeZone.SHORT ) );

            if ( mHomeTzId != null && mHomeTzId.length() > 0 ) {
                TimeZone homeTz = TimeZone.getTimeZone( mHomeTzId );
                mFormat.setTimeZone( homeTz );
                tv = (TextView) findViewById( R.id.home_time_value );
                tv.setText( mFormat.format( now )
                        +" "+homeTz.getDisplayName( homeTz.inDaylightTime( now ), TimeZone.SHORT ) );
            }

            long millis = SystemClock.uptimeMillis();
            // Calculate the next second boundary
            long future = ( ( millis+1000 )/1000 )*1000;            
            mHandler.postAtTime( new Runnable() {

                @Override
                public void run() {
                    updateTime();
                }
            }, future );
        }

    }

}
