/*
 * FlightIntel for Pilots
 *
 * Copyright 2015-2022 Nadeem Hasan <nhasan@nadmm.com>
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
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.nadmm.airports.data.DatabaseManager.LocationColumns

abstract class LocationListFragmentBase : ListFragmentBase() {

    protected var isLocationUpdateEnabled = false
        private set
    protected var lastLocation: Location? = null
        private set

    private var mRequestingLocationUpdates = false
    private var mPermissionDenied = false
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mRequestPermissionLauncher : ActivityResultLauncher<Array<String>>

    protected val nearbyRadius: Int
        get() = activityBase.prefNearbyRadius

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastLocation = arguments?.getParcelable(LocationColumns.LOCATION)

        isLocationUpdateEnabled = lastLocation == null

        if (isLocationUpdateEnabled) {
            setupFusedLocationProvider()
        }
    }

    override fun onResume() {
        super.onResume()

        if (isLocationUpdateEnabled) {
            when {
                isLocationPermissionGranted() -> {
                    activityBase.postRunnable( { startLocationUpdates() }, 0)
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    permission.ACCESS_FINE_LOCATION) -> {
                    showSnackbar("FlightIntel needs access to this device's location to " +
                            "show nearby facilities."
                    ) {
                        mRequestPermissionLauncher.launch(
                            arrayOf(
                                permission.ACCESS_FINE_LOCATION,
                                permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                } else -> {
                    resolvePermission { showApplicationSettings() }
                }
            }
        } else {
            // Location was passed to us, so just run the task
            startLocationTask()
        }
    }

    override fun onPause() {
        super.onPause()

        if (mRequestingLocationUpdates) {
            stopLocationUpdates()
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return (ActivityCompat.checkSelfPermission(
                requireContext(),
                permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
    }

    private fun setupFusedLocationProvider() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mLocationRequest = LocationRequest.create().apply {
            interval = 9000
            fastestInterval = 5000
            priority = if (activityBase.prefUseGps)
                Priority.PRIORITY_HIGH_ACCURACY
            else
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                updateLocation(p0.locations.lastOrNull())
            }
        }

        mRequestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                when {
                    permissions.getOrDefault(permission.ACCESS_FINE_LOCATION, false) -> {
                        startLocationUpdates()
                    }
                    permissions.getOrDefault(permission.ACCESS_COARSE_LOCATION, false) -> {
                        startLocationUpdates()
                    } else -> {
                        mPermissionDenied = true
                        setEmptyText("Unable to show nearby facilities.\n"
                                + "FlightIntel needs location permission.")
                        setListShown(false)
                        setFragmentContentShown(true)
                    }
                }
            }
    }

    private fun startLocationUpdates() {
        // First check if location is enabled in the settings
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)
        val client = LocationServices.getSettingsClient(requireActivity())
        client.checkLocationSettings(builder.build())
            // If location is enabled then register for location updates
            .addOnSuccessListener(requireActivity()) {
                try {
                    mFusedLocationClient.lastLocation
                        .addOnSuccessListener(requireActivity()) { updateLocation(it) }
                    mFusedLocationClient.requestLocationUpdates(
                        mLocationRequest,
                        mLocationCallback, Looper.getMainLooper()
                    )
                    mRequestingLocationUpdates = true
                } catch (e: SecurityException) {
                    // Ignore exception
                }
            }
            // If location is not enabled then prompt the user to enable by showing system dialog
            .addOnFailureListener(requireActivity()) { exception ->
                if (exception is ResolvableApiException) {
                    resolvePermission { showLocationSettings() }
                }
            }
    }

    private fun resolvePermission(func: () -> Unit) {
        mPermissionDenied = true
        setEmptyText("Unable to show nearby facilities.\n"
                + "FlightIntel needs location permission.")
        setListShown(false)
        setFragmentContentShown(true)
        showSnackbar(
            "Please enable location in Settings to see nearby facilities."
        ) { func() }
    }

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            .addOnCompleteListener(requireActivity()) { mRequestingLocationUpdates = false }
    }

    private fun updateLocation(location: Location?) {
        location?.let {
            if (lastLocation == null || it.distanceTo(lastLocation!!) > 500) {
                // Preserve battery. Only update the list of we have moved more than 500 meters.
                lastLocation = it
                startLocationTask()
            }
        }
    }

    private fun showSnackbar(text: String, listener: View.OnClickListener) {
        listView?.apply {
            Snackbar.make(this, text, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, listener)
                .show()
        }
    }

    private fun showLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun showApplicationSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", activity?.packageName, null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    protected abstract fun startLocationTask()

}
