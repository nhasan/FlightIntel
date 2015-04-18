/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-205 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.wx;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import com.nadmm.airports.DatabaseManager;
import com.nadmm.airports.DatabaseManager.LocationColumns;
import com.nadmm.airports.PreferencesActivity;
import com.nadmm.airports.utils.NearbyHelper;

import java.util.Locale;

public class NearbyWxFragment extends WxListFragmentBase {

    private NearbyHelper mNearbyHelper;
    private int mRadius;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( getActivity() );
        mRadius = Integer.valueOf( prefs.getString(
                PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "30" ) );

        setEmptyText( "No wx stations found nearby." );
    }

    @Override
    public void onResume() {
        super.onResume();

        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopLocationUpdates();
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Location location = null;
        Bundle args = getArguments();
        if ( args != null ) {
            location = args.getParcelable( LocationColumns.LOCATION );
        }
        if ( location != null ) {
            // If we are passed a location use that
            onLocationChanged( location );
        } else {
            // Otherwise get the current location updates
            mNearbyHelper = new NearbyHelper( getActivity(), this );
        }

        View view = getView();
        view.setKeepScreenOn( true );

        setActionBarSubtitle( String.format( Locale.US, "Within %d NM radius", mRadius ) );
    }

    @Override
    public void onLocationChanged( Location location ) {
        if ( getActivity() != null ) {
            new NearbyWxTask().execute( location );
        } else {
            stopLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if ( mNearbyHelper != null ) {
            mNearbyHelper.startLocationUpdates();
        }
    }

    private void stopLocationUpdates() {
        if ( mNearbyHelper != null ) {
            mNearbyHelper.stopLocationUpdates();
        }
    }

    private final class NearbyWxTask extends AsyncTask<Location, Void, Cursor> {

        @Override
        protected Cursor doInBackground( Location... params ) {
            Location location = params[ 0 ];
            SQLiteDatabase db = getDatabase( DatabaseManager.DB_FADDS );
            return new NearbyWxCursor( db, location, mRadius );
        }

        @Override
        protected void onPostExecute( Cursor c ) {
            setCursor( c );
        }
    }

}
