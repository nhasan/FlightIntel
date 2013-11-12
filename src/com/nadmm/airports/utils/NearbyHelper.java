/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2013 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import com.nadmm.airports.PreferencesActivity;

import java.util.Date;

public class NearbyHelper implements LocationListener {

    private Context mContext;
    private LocationManager mLocationManager;
    private LocationListener mLocationListner;
    private Location mLastLocation;
    private int mRadius;

    private final long TOO_OLD = 5*DateUtils.MINUTE_IN_MILLIS;

    public NearbyHelper( Context context, LocationListener locationListener ) {
        mContext = context;
        mLocationListner = locationListener;
        mLocationManager = (LocationManager) mContext.getSystemService(
                Context.LOCATION_SERVICE );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( mContext );
        mRadius = Integer.valueOf( prefs.getString(
                PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "30" ) );
    }

    public void startLocationUpdates() {
        Location location = getLastKnownGoodLocation();
        if ( location != null ) {
            onLocationChanged( location );
        }

        if ( mLocationManager != null ) {
            mLocationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER,
                    30*DateUtils.SECOND_IN_MILLIS, 0.5f*GeoUtils.METERS_PER_STATUTE_MILE,
                    this );

            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences( mContext );
            boolean useGps = prefs.getBoolean( PreferencesActivity.KEY_LOCATION_USE_GPS, false );
            if ( useGps ) {
                mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                        30*DateUtils.SECOND_IN_MILLIS, 0.5f*GeoUtils.METERS_PER_STATUTE_MILE,
                        this );
            }
        }
    }

    public int getRadius() {
        return mRadius;
    }

    public void stopLocationUpdates() {
        if ( mLocationManager != null ) {
            mLocationManager.removeUpdates( this );
        }
    }

    @Override
    public void onLocationChanged( Location location ) {
        // Is this location an improvement from the last?
        if ( GeoUtils.isBetterLocation( location, mLastLocation ) ) {
            mLocationListner.onLocationChanged( location );
            mLastLocation = location;
        }
    }

    @Override
    public void onStatusChanged( String provider, int status, Bundle extras ) {
    }

    @Override
    public void onProviderEnabled( String provider ) {
    }

    @Override
    public void onProviderDisabled( String provider ) {
    }

    protected Location getLastKnownGoodLocation() {
        // Get the last known location to use a starting point
        Location location = mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
        if ( location == null ) {
            // Try to get last location from network provider
            location = mLocationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER );
        }

        if ( location != null ) {
            Date now = new Date();
            long age = now.getTime()-location.getTime();
            if ( age > TOO_OLD ) {
                // Discard too old
                location = null;
            }
        }

        return location;
    }

}
