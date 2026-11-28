/*
 * FlightIntel for Pilots
 *
 * Copyright 2015-2026 Nadeem Hasan <nhasan@nadmm.com>
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
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.nadmm.airports.data.DatabaseManager.LocationColumns
import com.nadmm.airports.utils.GeoUtils

/**
 * A screen of things near the device's location, or near a location passed in
 * the arguments: it follows the location while showing, and calls
 * [startLocationTask] when there is one, or it has moved.
 *
 * Asks for location permission the first time the screen needs it. After one
 * denial it explains why and offers to ask again; once Android stops showing
 * the request, it offers the app's settings instead. When location is turned
 * off it shows the system dialog to turn it on. Why there is no location goes
 * to [showLocationMessage], for the screen to show in place of its content.
 */
abstract class LocationFragmentBase : FragmentBase() {

    protected var isLocationUpdateEnabled = false
        private set
    protected var lastLocation: Location? = null
        private set

    private var mRequestingLocationUpdates = false
    // Set once this fragment has shown the request, so onResume does not
    // show it again by itself
    private var mPermissionAsked = false
    private var mSettingsAsked = false
    private var mSnackbar: Snackbar? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback
    private lateinit var mRequestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var mLocationSettingsLauncher: ActivityResultLauncher<IntentSenderRequest>

    protected val nearbyRadius: Int
        get() = activityBase.prefNearbyRadius

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            lastLocation = BundleCompat.getParcelable(
                it,
                LocationColumns.LOCATION,
                Location::class.java
            )
        }

        isLocationUpdateEnabled = lastLocation == null

        if (isLocationUpdateEnabled) {
            setupFusedLocationProvider()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isLocationUpdateEnabled) {
            // Location was passed to us, so just run the task
            startLocationTask()
            return
        }

        when {
            isLocationPermissionGranted() -> {
                activityBase.postRunnable({ startLocationUpdates() }, 0)
            }
            !mPermissionAsked && !wasPermissionRequested() -> {
                // First time: the screen makes the reason clear, so ask right away
                requestPermission()
            }
            else -> {
                // Answered before: explain, with an action to ask again or
                // open Settings, so the user is never left without a way on
                showPermissionNeeded()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (mRequestingLocationUpdates) {
            stopLocationUpdates()
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        val context = requireContext()
        return ActivityCompat.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun setupFusedLocationProvider() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        mLocationRequest = GeoUtils.buildLocationRequest(5_000, activityBase.prefUseGps)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                updateLocation(p0.locations.lastOrNull())
            }
        }

        mRequestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            isRequestingPermission = false
            // Approximate location is enough for nearby things
            if (permissions.values.any { it }) {
                startLocationUpdates()
            } else {
                showPermissionNeeded()
            }
        }

        mLocationSettingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startLocationUpdates()
            } else {
                showLocationOff(null)
            }
        }
    }

    private fun requestPermission() {
        mPermissionAsked = true
        isRequestingPermission = true
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
            putBoolean(KEY_LOCATION_PERMISSION_REQUESTED, true)
        }
        mRequestPermissionLauncher.launch(
            arrayOf(permission.ACCESS_FINE_LOCATION, permission.ACCESS_COARSE_LOCATION)
        )
    }

    private fun wasPermissionRequested() =
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean(KEY_LOCATION_PERMISSION_REQUESTED, false)

    /**
     * Explains why there is nothing to show. While Android still shows the request,
     * the action asks again; after that, it opens the app's settings.
     */
    private fun showPermissionNeeded() {
        showLocationMessage(PERMISSION_MESSAGE)
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), permission.ACCESS_FINE_LOCATION)
        ) {
            showSnackbar("Allow location access to see nearby facilities.", "Allow") {
                requestPermission()
            }
        } else {
            showSnackbar("Turn on location permission in Settings.", "Settings") {
                showApplicationSettings()
            }
        }
    }

    private fun startLocationUpdates() {
        mSnackbar?.dismiss()
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
                } catch (_: SecurityException) {
                    // Ignore exception
                }
            }
            // Location is off: show the system dialog to turn it on, once
            .addOnFailureListener(requireActivity()) { exception ->
                if (exception is ResolvableApiException && !mSettingsAsked) {
                    mSettingsAsked = true
                    launchLocationSettings(exception)
                } else {
                    showLocationOff(exception as? ResolvableApiException)
                }
            }
    }

    private fun launchLocationSettings(exception: ResolvableApiException) {
        try {
            mLocationSettingsLauncher.launch(
                IntentSenderRequest.Builder(exception.resolution).build()
            )
        } catch (_: Exception) {
            showLocationOff(null)
        }
    }

    private fun showLocationOff(exception: ResolvableApiException?) {
        showLocationMessage(LOCATION_OFF_MESSAGE)
        showSnackbar("Location is turned off.", "Turn on") {
            if (exception != null) launchLocationSettings(exception)
            else showLocationSettings()
        }
    }

    /** Shows why there is no location, such as no permission, in place of the content. */
    protected abstract fun showLocationMessage(message: String)

    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            .addOnCompleteListener(requireActivity()) { mRequestingLocationUpdates = false }
    }

    private fun updateLocation(location: Location?) {
        location?.let {
            if (lastLocation == null || it.distanceTo(lastLocation!!) > 500) {
                // Preserve battery: only update once we have moved more than 500 meters
                lastLocation = it
                startLocationTask()
            }
        }
    }

    private fun showSnackbar(text: String, action: String, listener: () -> Unit) {
        mSnackbar?.dismiss()
        mSnackbar = view?.let {
            Snackbar.make(it, text, Snackbar.LENGTH_INDEFINITE)
                .setAction(action) { listener() }
                .apply { show() }
        }
    }

    private fun showLocationSettings() {
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun showApplicationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = "package:${requireContext().packageName}".toUri()
        startActivity(intent)
    }

    protected abstract fun startLocationTask()

    companion object {
        private const val KEY_LOCATION_PERMISSION_REQUESTED = "location_permission_requested"

        /**
         * True while the location permission request is showing, so other
         * prompts can wait for it.
         */
        var isRequestingPermission = false
            private set

        private const val PERMISSION_MESSAGE = "FlightIntel needs location permission to " +
                "show nearby facilities.\n" +
                "Your location stays on this device and is not shared with anyone."
        private const val LOCATION_OFF_MESSAGE = "Turn on location to see nearby facilities."
    }
}
