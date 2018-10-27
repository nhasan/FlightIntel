/*
 * FlightIntel for Pilots
 *
 * Copyright 2015-2018 Nadeem Hasan <nhasan@nadmm.com>
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

import android.Manifest.permission;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.material.snackbar.Snackbar;
import com.nadmm.airports.data.DatabaseManager.LocationColumns;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public abstract class LocationListFragmentBase extends ListFragmentBase {

    private boolean mLocationUpdatesEnabled = false;
    private boolean mRequestingLocationUpdates = false;
    private boolean mPermissionDenied = false;
    private Location mLastLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private SettingsClient mSettingsClient;
    private LocationSettingsRequest mLocationSettingsRequest;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Bundle args = getArguments();
        if ( args != null && args.containsKey( LocationColumns.LOCATION ) ) {
            mLastLocation = args.getParcelable( LocationColumns.LOCATION );
        }

        mLocationUpdatesEnabled = ( mLastLocation == null );
    }

    @Override
    public void onResume() {
        super.onResume();

        if ( mLocationUpdatesEnabled ) {
            if ( checkPermission() ) {
                getActivityBase().postRunnable( () -> startLocationUpdates(), 0 );
            } else {
                requestPermission();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if ( mRequestingLocationUpdates ) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState ) {
        super.onActivityCreated( savedInstanceState );

        if ( mLocationUpdatesEnabled ) {
            setupFusedLocationProvider();
        } else {
            // Location was passed to us, so just run the task
            newLocationTask().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult( int requestCode, @NonNull String[] permissions,
                                            @NonNull int[] grantResults ) {
        if ( requestCode == PERMISSION_REQUEST_FINE_LOCATION ) {
            if ( grantResults.length <= 0 ) {
                // If user interaction gets interrupted, permission request is cancelled and
                // we get an empty array.
            } else if ( grantResults[ 0 ] == PackageManager.PERMISSION_GRANTED ) {
                startLocationUpdates();
            } else {
                // Set the flag so we do not ask for permission repeatedly during the
                // fragment lifetime
                mPermissionDenied = true;

                setEmptyText( "Unable to show nearby facilities.\n"
                        + "FlightIntel needs location permission." );
                setListShown( false );
                setFragmentContentShown( true );
            }
        } else {
            super.onRequestPermissionsResult( requestCode, permissions, grantResults );
        }
    }

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        // This is not getting called but it should.
        if ( requestCode ==PERMISSION_REQUEST_FINE_LOCATION ) {
            if ( resultCode == RESULT_OK)  {
                // Nothing to do. startLocationupdates() gets called in onResume again.
            } else if ( resultCode == RESULT_CANCELED ) {
                mLocationUpdatesEnabled = false;
            }
        } else {
            super.onActivityResult( requestCode, resultCode, data );
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission( getActivity(),
                permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale( getActivity(),
                        permission.ACCESS_FINE_LOCATION );
        if ( shouldProvideRationale ) {
            showSnackbar( "FlightIntel needs access to device's location to show nearby facilities.",
                    view -> requestPermissions( new String[]{ permission.ACCESS_FINE_LOCATION },
                            PERMISSION_REQUEST_FINE_LOCATION ) );
        } else if ( !mPermissionDenied ) {
            requestPermissions( new String[]{ permission.ACCESS_FINE_LOCATION },
                    PERMISSION_REQUEST_FINE_LOCATION );
        } else {
            // Set the flag so we do not ask for permission repeatedly during the
            // fragment lifetime
            showSnackbar( "Please enable location permission in the application details settings.",
                    v -> showApplicationSettings() );
        }
    }

    private void setupFusedLocationProvider() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient( getActivity() );
        mSettingsClient = LocationServices.getSettingsClient( getActivity() );

        makeLocationRequest( getActivityBase().getPrefUseGps() );
        makeLocationCallback();
        buildLocationSettingsRequest();
    }

    private void makeLocationRequest( boolean useGps ) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval( 7000 );
        mLocationRequest.setFastestInterval( 3000 );
        mLocationRequest.setPriority( useGps?
                LocationRequest.PRIORITY_HIGH_ACCURACY :
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY );
    }

    private void makeLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult( LocationResult locationResult ) {
                if ( locationResult != null ) {
                    List<Location> locations = locationResult.getLocations();
                    if ( locations.size() >= 1 ) {
                        // Use the latest location
                        updateLocation( locations.get( locations.size() - 1 ) );
                    }
                }
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest( mLocationRequest );
        mLocationSettingsRequest = builder.build();
    }

    private void startLocationUpdates() {
        // First check if location is enabled in the settings
        mSettingsClient.checkLocationSettings( mLocationSettingsRequest )

        // If location is enabled then register for location updates
        .addOnSuccessListener( getActivity(), locationSettingsResponse -> {
            requestLocationUpdates();
        } )

        // If location is not enabled then prompt the user to enable by showing system dialog
        .addOnFailureListener( getActivity(), e -> {
            showSnackbar( "Please enable location in settings to see nearby facilities.",
                    v -> showLocationSettings() );

            setEmptyText( "Unable to show nearby facilities.\n"
                    + "Location is not enabled on this device." );
            setListShown( false );
            setFragmentContentShown( true );
        } );
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates( mLocationCallback )
            .addOnCompleteListener( getActivity(), task -> mRequestingLocationUpdates = false );
    }

    private void requestLocationUpdates() {
        if ( checkPermission() ) {
            // Get the last location as the starting point
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener( getActivity(), location -> updateLocation( location ) );

            mFusedLocationClient.requestLocationUpdates( mLocationRequest, mLocationCallback, null );
            mRequestingLocationUpdates = true;
        }
    }

    private void updateLocation( Location location ) {
        if ( location != null ) {
            if ( mLastLocation == null || location.distanceTo( mLastLocation ) > 50 ) {
                // Preserve battery. Only update the list of we have moved more than 50 meters.
                mLastLocation = location;
                newLocationTask().execute();
            }
        }
    }

    private void showSnackbar( String text, View.OnClickListener listener) {
        Snackbar.make( getListView(), text, Snackbar.LENGTH_INDEFINITE )
                .setAction( android.R.string.ok, listener )
                .show();
    }

    private void showLocationSettings() {
        Intent intent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity( intent );
    }

    private void showApplicationSettings() {
        Intent intent = new Intent();
        intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
        Uri uri = Uri.fromParts( "package", BuildConfig.APPLICATION_ID, null );
        intent.setData( uri );
        intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        startActivity( intent );
    }

    protected int getNearbyRadius() {
        return getActivityBase().getPrefNearbyRadius();
    }

    protected Location getLastLocation() {
        return mLastLocation;
    }

    protected boolean isLocationUpdateEnabled()
    {
        return mLocationUpdatesEnabled;
    }

    protected abstract LocationTask newLocationTask();

    protected abstract class LocationTask extends AsyncTask<Void, Void, Cursor> {
    }

}
