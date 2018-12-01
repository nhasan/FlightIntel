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

package com.nadmm.airports.clocks;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nadmm.airports.FragmentBase;
import com.nadmm.airports.R;
import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.utils.CursorAsyncTask;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ClockFragment extends FragmentBase {

    private final Handler mHandler = new Handler();
    private final DateFormat mTimeFormat = new SimpleDateFormat( "HH:mm:ss", Locale.US );
    private final DateFormat mDateFormat = new SimpleDateFormat( "dd MMM yyyy", Locale.US );

    private Runnable mRunnable;
    private String mHome;
    private String mHomeTzId;

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState ) {
        View view = inflate( R.layout.clocks_clock_view );
        return createContentView( view );
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        mHome = getActivityBase().getPrefHomeAirport();
        setBackgroundTask( new ClockTask( this ) ).execute();

        setFragmentContentShown( true );
    }

    @Override
    public void onPause() {
        super.onPause();

        if ( mRunnable != null ) {
            // Stop updates
            mHandler.removeCallbacks( mRunnable );
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivityBase().setDrawerIndicatorEnabled( false );

        if ( mRunnable != null ) {
            // Restart updates
            updateTime();
        }
    }

    @SuppressLint( "SetTextI18n" )
    private void updateTime() {
        Date now = new Date();

        TimeZone utcTz = TimeZone.getTimeZone( "GMT" );
        mTimeFormat.setTimeZone( utcTz );
        mDateFormat.setTimeZone( utcTz );
        TextView tv = findViewById( R.id.utc_time_value );
        tv.setText( mTimeFormat.format( now )+" UTC" );
        tv = findViewById( R.id.utc_date_value );
        tv.setText( mDateFormat.format( now ) );

        TimeZone localTz = TimeZone.getDefault();
        mTimeFormat.setTimeZone( localTz );
        mDateFormat.setTimeZone( localTz );
        tv = findViewById( R.id.local_time_value );
        tv.setText( mTimeFormat.format( now )
                +" "+localTz.getDisplayName( localTz.inDaylightTime( now ), TimeZone.SHORT ) );
        tv = findViewById( R.id.local_date_value );
        tv.setText( mDateFormat.format( now ) );

        if ( mHomeTzId != null && mHomeTzId.length() > 0 ) {
            TimeZone homeTz = TimeZone.getTimeZone( mHomeTzId );
            mTimeFormat.setTimeZone( homeTz );
            mDateFormat.setTimeZone( homeTz );
            tv = findViewById( R.id.home_time_value );
            tv.setText( mTimeFormat.format( now )
                    +" "+homeTz.getDisplayName( homeTz.inDaylightTime( now ), TimeZone.SHORT ) );
            tv = findViewById( R.id.home_date_value );
            tv.setText( mDateFormat.format( now ) );
        }

        scheduleUpdate();
    }

    private void scheduleUpdate() {
        if ( mRunnable == null ) {
            mRunnable = () -> updateTime();
        }
        // Reschedule at the next second boundary
        long now = new Date().getTime();
        long delay = 1000-(now%1000);
        mHandler.postDelayed( mRunnable, delay );
    }

    private Cursor[] doQuery() {
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
            return new Cursor[] { c };
        }

        return null;
    }

    private void setCursor( Cursor c ) {
        if ( c != null && c.moveToFirst() ) {
            mHomeTzId = c.getString( c.getColumnIndex( Airports.TIMEZONE_ID ) );
        }
        if ( mHomeTzId == null || mHomeTzId.length() == 0 ) {
            View v = findViewById( R.id.home_time_label );
            v.setVisibility( View.GONE );
            v = findViewById( R.id.home_time_layout );
            v.setVisibility( View.GONE );
        }
        updateTime();
    }

    private static class ClockTask extends CursorAsyncTask<ClockFragment> {

        private ClockTask( ClockFragment fragment ) {
            super( fragment );
        }

        @Override
        protected Cursor[] onExecute( ClockFragment fragment, String... params ) {
            return fragment.doQuery();
        }

        @Override
        protected boolean onResult( ClockFragment fragment, Cursor[] result ) {
            if ( result != null ) {
                fragment.setCursor( result[ 0 ] );
            }
            return true;
        }

    }

}
