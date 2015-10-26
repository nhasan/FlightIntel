/*
 * FlightIntel for Pilots
 *
 * Copyright 2015 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;

import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.utils.NearbyHelper;

public abstract class LocationListFragmentBase extends ListFragmentBase {

    private NearbyHelper mNearbyHelper;
    private int mRadius;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( getActivity() );
        mRadius = Integer.valueOf(
                prefs.getString( PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "30" ) );
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

        if ( isLocationEnabled() ) {
            Location location = null;
            Bundle args = getArguments();
            if ( args != null ) {
                location = args.getParcelable( DatabaseManager.LocationColumns.LOCATION );
            }
            if ( location != null ) {
                // If we are passed a location use that
                onLocationChanged( location );
            } else {
                // Otherwise get the current location updates
                mNearbyHelper = new NearbyHelper( getActivity(), this );
            }
        }

        getActivityBase().onFragmentStarted( this );
    }

    @Override
    public void onLocationChanged( Location location ) {
        if ( getActivity() != null ) {
            newLocationTask().execute( location );
        } else {
            stopLocationUpdates();
        }
    }

    protected boolean isLocationEnabled() {
        return true;
    }

    protected abstract LocationTask newLocationTask();

    protected void startLocationUpdates() {
        if ( mNearbyHelper != null ) {
            mNearbyHelper.startLocationUpdates();
        }
    }

    protected void stopLocationUpdates() {
        if ( mNearbyHelper != null ) {
            mNearbyHelper.stopLocationUpdates();
        }
    }

    protected int getNearbyRadius() {
        return mRadius;
    }

    protected abstract class LocationTask extends AsyncTask<Location, Void, Cursor> {
    }

}
