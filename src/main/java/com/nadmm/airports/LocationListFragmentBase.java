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

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.View;

import com.nadmm.airports.data.DatabaseManager;
import com.nadmm.airports.utils.GeoUtils;

import java.util.Date;

public abstract class LocationListFragmentBase extends ListFragmentBase
        implements LocationListener {

    private LocationManager mLocationManager;
    private int mRadius;
    private boolean mPermissionDenied = false;
    private boolean mIsUpdatingLocation = true;

    private final long TOO_OLD = 5*DateUtils.MINUTE_IN_MILLIS;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( getActivity() );
        mRadius = Integer.valueOf(
                prefs.getString( PreferencesActivity.KEY_LOCATION_NEARBY_RADIUS, "30" ) );
        if ( isLocationEnabled() ) {
            mLocationManager = (LocationManager) getActivity().getSystemService(
                    Context.LOCATION_SERVICE );
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if ( isLocationEnabled() ) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if ( isLocationEnabled() ) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        Location location = null;
        Bundle args = getArguments();
        if ( args != null ) {
            if ( args.containsKey( DatabaseManager.LocationColumns.LOCATION )) {
                // Location was passed so disable updates based on current location
                mIsUpdatingLocation = false;
                location = args.getParcelable( DatabaseManager.LocationColumns.LOCATION );
                if ( location != null ) {
                    // If we are passed a location use that
                    onLocationChanged( location );
                }
            }
        }

        getActivityBase().onFragmentStarted( this );
    }

    @Override
    public void onLocationChanged( Location location ) {
        if ( getActivity() != null ) {
            newLocationTask().execute( location );
        } else {
            if ( isLocationEnabled() ) {
                stopLocationUpdates();
            }
        }
    }

    @Override
    public void onProviderDisabled( String provider ) {
    }

    @Override
    public void onProviderEnabled( String provider ) {
    }

    @Override
    public void onStatusChanged( String provider, int status, Bundle extras ) {
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, String[] permissions,
                                            int[] grantResults ) {
        if ( requestCode == PERMISSION_REQUEST_FINE_LOCATION ) {
            if ( grantResults.length == 1
                    && grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ) {
            } else {
                // Set the flag so we do not ask for permission repeatedly during the
                // fragment lifetime
                mPermissionDenied = true;
                Snackbar.make( getView(), "FlightIntel needs location permissions.",
                        Snackbar.LENGTH_LONG ).show();
                setEmptyText( "Unable to show nearby locations.\n"
                        + "Grant location permission by going to:\n"
                        + "Settings -> Apps -> FlightIntel -> Permission" );
                setListShown( false );
                setFragmentContentShown( true );
            }
        } else {
            super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        }
    }

    protected void startLocationUpdates() {
        if ( ContextCompat.checkSelfPermission(
                getActivity(), Manifest.permission.ACCESS_FINE_LOCATION )
                == PackageManager.PERMISSION_GRANTED ) {
            final Location location = getLastKnownGoodLocation();
            if ( location != null ) {
                new Handler( Looper.getMainLooper() ).postDelayed( new Runnable() {
                    @Override
                    public void run() {
                        onLocationChanged( location );
                    }
                }, 0 );
            }

            mLocationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER,
                    30 * DateUtils.SECOND_IN_MILLIS, 0.5f * GeoUtils.METERS_PER_STATUTE_MILE,
                    this );

            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences( getActivity() );
            boolean useGps = prefs.getBoolean( PreferencesActivity.KEY_LOCATION_USE_GPS, false );
            if ( useGps ) {
                mLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                        30 * DateUtils.SECOND_IN_MILLIS, 0.5f * GeoUtils.METERS_PER_STATUTE_MILE,
                        this );
            }
        } else if ( !mPermissionDenied ) {
            if ( shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_FINE_LOCATION ) )
            {
                Snackbar.make( getView(),
                        "FlightIntel needs access to device's location.",
                        Snackbar.LENGTH_INDEFINITE )
                        .setAction( android.R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick( View v ) {
                                requestPermissions(
                                        new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                                        PERMISSION_REQUEST_FINE_LOCATION );
                            }
                        } )
                        .show();

            } else {
                requestPermissions( new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                        PERMISSION_REQUEST_FINE_LOCATION );
            }
        }
    }

    protected void stopLocationUpdates() {
        if ( ContextCompat.checkSelfPermission(
                getActivity(), Manifest.permission.ACCESS_FINE_LOCATION )
                == PackageManager.PERMISSION_GRANTED ) {
            mLocationManager.removeUpdates( this );
        }
    }

    protected Location getLastKnownGoodLocation() {
        Location location = null;
        if ( ContextCompat.checkSelfPermission(
                getActivity(), Manifest.permission.ACCESS_FINE_LOCATION )
                == PackageManager.PERMISSION_GRANTED ) {
            // Get the last known location to use a starting point
            location = mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
            if ( location == null ) {
                // Try to get last location from network provider
                location = mLocationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER );
            }

            if ( location != null ) {
                Date now = new Date();
                long age = now.getTime() - location.getTime();
                if ( age > TOO_OLD ) {
                    // Discard too old
                    location = null;
                }
            }
        }
        return location;
    }

    protected int getNearbyRadius() {
        return mRadius;
    }

    protected boolean isLocationEnabled() {
        return mIsUpdatingLocation;
    }

    protected abstract LocationTask newLocationTask();

    protected abstract class LocationTask extends AsyncTask<Location, Void, Cursor> {
    }

}
