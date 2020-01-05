/*
 * FlightIntel for Pilots
 *
 * Copyright 2015-2020 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports

import android.Manifest.permission
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.nadmm.airports.data.DatabaseManager.LocationColumns

abstract class LocationListFragmentBase : ListFragmentBase() {

    protected var isLocationUpdateEnabled = false
        private set
    private var mRequestingLocationUpdates = false
    private var mPermissionDenied = false
    protected var lastLocation: Location? = null
        private set
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var mSettingsClient: SettingsClient? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null

    protected val nearbyRadius: Int
        get() = activityBase.prefNearbyRadius

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null && args.containsKey(LocationColumns.LOCATION)) {
            lastLocation = args.getParcelable(LocationColumns.LOCATION)
        }

        isLocationUpdateEnabled = lastLocation == null
    }

    override fun onResume() {
        super.onResume()

        if (isLocationUpdateEnabled) {
            if (checkPermission()) {
                activityBase.postRunnable(Runnable { this.startLocationUpdates() }, 0)
            } else {
                requestPermission()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (mRequestingLocationUpdates) {
            stopLocationUpdates()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (isLocationUpdateEnabled) {
            setupFusedLocationProvider()
        } else {
            // Location was passed to us, so just run the task
            startLocationTask()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            when {
                grantResults.isEmpty() -> {
                    // If user interaction gets interrupted, permission request is cancelled and
                    // we get an empty array.
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    startLocationUpdates()
                }
                else -> {
                    // Set the flag so we do not ask for permission repeatedly during the
                    // fragment lifetime
                    mPermissionDenied = true

                    setEmptyText("Unable to show nearby facilities.\n" + "FlightIntel needs location permission.")
                    setListShown(false)
                    setFragmentContentShown(true)
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // This is not getting called but it should.
        if (requestCode == PERMISSION_REQUEST_FINE_LOCATION) {
            when (resultCode) {
                RESULT_OK -> {
                    // Nothing to do. startLocationupdates() gets called in onResume again.
                }
                RESULT_CANCELED -> {
                    isLocationUpdateEnabled = false
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(activity!!,
                permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity!!,
                permission.ACCESS_FINE_LOCATION)
        if (shouldProvideRationale) {
            showSnackbar("FlightIntel needs access to device's location to show nearby facilities.",
                    View.OnClickListener {
                        requestPermissions(arrayOf(permission.ACCESS_FINE_LOCATION),
                                PERMISSION_REQUEST_FINE_LOCATION)
                    })
        } else if (!mPermissionDenied) {
            requestPermissions(arrayOf(permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_FINE_LOCATION)
        } else {
            // Set the flag so we do not ask for permission repeatedly during the
            // fragment lifetime
            showSnackbar("Please enable location permission in the application details settings.",
                    View.OnClickListener { showApplicationSettings() })
        }
    }

    private fun setupFusedLocationProvider() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!)
        mSettingsClient = LocationServices.getSettingsClient(activity!!)

        makeLocationRequest(activityBase.prefUseGps)
        makeLocationCallback()
        buildLocationSettingsRequest()
    }

    private fun makeLocationRequest(useGps: Boolean) {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = 7000
        mLocationRequest!!.fastestInterval = 3000
        mLocationRequest!!.priority = if (useGps)
            LocationRequest.PRIORITY_HIGH_ACCURACY
        else
            LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    private fun makeLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult != null) {
                    val locations = locationResult.locations
                    if (locations.size >= 1) {
                        // Use the latest location
                        updateLocation(locations[locations.size - 1])
                    }
                }
            }
        }
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
    }

    private fun startLocationUpdates() {
        if (activity == null) {
            return
        }
        // First check if location is enabled in the settings
        mSettingsClient!!.checkLocationSettings(mLocationSettingsRequest)
                // If location is enabled then register for location updates
                .addOnSuccessListener(activity!!) { requestLocationUpdates() }
                // If location is not enabled then prompt the user to enable by showing system dialog
                .addOnFailureListener(activity!!) {
                    showSnackbar("Please enable location in settings to see nearby facilities.",
                            View.OnClickListener { showLocationSettings() })

                    setEmptyText("Unable to show nearby facilities.\n" + "Location is not enabled on this device.")
                    setListShown(false)
                    setFragmentContentShown(true)
                }
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient!!.removeLocationUpdates(mLocationCallback!!)
                .addOnCompleteListener(activity!!) { mRequestingLocationUpdates = false }
    }

    private fun requestLocationUpdates() {
        if (checkPermission()) {
            // Get the last location as the starting point
            mFusedLocationClient!!.lastLocation
                    .addOnSuccessListener(activity!!) { this.updateLocation(it) }

            mFusedLocationClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback!!, null)
            mRequestingLocationUpdates = true
        }
    }

    private fun updateLocation(location: Location?) {
        if (location != null) {
            if (lastLocation == null || location.distanceTo(lastLocation) > 100) {
                // Preserve battery. Only update the list of we have moved more than 100 meters.
                lastLocation = location
                startLocationTask()
            }
        }
    }

    private fun showSnackbar(text: String, listener: View.OnClickListener) {
        Snackbar.make(listView, text, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, listener)
                .show()
    }

    private fun showLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun showApplicationSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    protected abstract fun startLocationTask()

    companion object {

        private const val PERMISSION_REQUEST_FINE_LOCATION = 1
    }
}
