/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.wx

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.core.content.IntentCompat
import com.nadmm.airports.utils.GeoUtils
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch

class PirepService : NoaaService2("pirep", PIREP_CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_PIREP) {
                    val type = intent.getStringExtra(TYPE)
                    if (type == TYPE_TEXT) {
                        handleTextPirepRequest(intent)
                    } else if (type == TYPE_GRAPHIC) {
                        handleGraphicPirepRequest(intent)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun handleTextPirepRequest(intent: Intent) {
        // Get request parameters
        val action = intent.action
        val location = IntentCompat.getParcelableExtra(intent, LOCATION, Location::class.java) ?: return
        val stationId = intent.getStringExtra(STATION_ID) ?: return
        val radiusNM = intent.getIntExtra(RADIUS_NM, 50)
        val hours = intent.getIntExtra(HOURS_BEFORE, 3)
        val cacheOnly = intent.getBooleanExtra(CACHE_ONLY, false)
        val forceRefresh = intent.getBooleanExtra(FORCE_REFRESH, false)

        val xmlFile = getDataFile("PIREP_$stationId.xml")
        val objFile = getDataFile("PIREP_$stationId.obj")
        if (forceRefresh) {
            // If force refresh is requested, delete the cached files
            xmlFile.delete()
            objFile.delete()
        }

        var pirep: Pirep?
        if (objFile.exists()) {
            // Check if the cached PIREP is still valid
            pirep = readObject(objFile) as Pirep?
            if (pirep != null) {
                // If the cached object is valid, send it
                sendSerializableResultIntent(action, stationId, pirep)
                return
            }
            // If the cached object is invalid, delete it
            xmlFile.delete()
            objFile.delete()
        }

        if (cacheOnly) {
            // At this point, we have no cached PIREP so send an empty one
            sendSerializableResultIntent(action, stationId, Pirep())
            return
        }

        try {
            val query = ("dataSource=aircraftreports&requestType=retrieve&format=xml"
                    + "&hoursBeforeNow=%d&radialDistance=%.0f;%.2f,%.2f").format(hours,
                radiusNM * GeoUtils.STATUTE_MILES_PER_NAUTICAL_MILES,
                location.longitude, location.latitude
            )
            fetchFromNoaa(query, xmlFile, false)

            if (!xmlFile.exists()) {
                throw Exception("Unknown error")
            }

            pirep = PirepParser.parse(xmlFile, location, radiusNM)
            pirep.stationId = stationId
            writeObject(pirep, objFile)
        } catch (e: Exception) {
            showToast(this, "Unable to fetch PIREP: " + e.message)
            pirep = Pirep()
        }
        // Send the Pirep
        sendSerializableResultIntent(action, stationId, pirep)
    }

    private fun handleGraphicPirepRequest(intent: Intent) {
        val action = intent.action
        val imgType = intent.getStringExtra(IMAGE_TYPE)
        val code = intent.getStringExtra(IMAGE_CODE)
        val imageName = String.format("pireps_%s_%s.gif", imgType, code)
        val imageFile = getDataFile(imageName)
        if (!imageFile.exists()) {
            try {
                var path = "/data/obs/pirep/"
                path += imageName
                fetchFromNoaa(path, null, imageFile, false)
            } catch (e: Exception) {
                showToast(this, "Unable to fetch PIREP image: " + e.message)
            }
        }
        // Broadcast the result
        sendImageResultIntent(action, code, imageFile)
    }

    companion object {

        fun startService(context: Context, stationId: String, location: Location, refresh: Boolean) {
            val intent = Intent(context, PirepService::class.java).apply {
                setAction(NoaaService.ACTION_GET_PIREP)
                putExtra(NoaaService.STATION_ID, stationId)
                putExtra(NoaaService.TYPE, NoaaService.TYPE_TEXT)
                putExtra(NoaaService.RADIUS_NM, PIREP_RADIUS_NM)
                putExtra(NoaaService.HOURS_BEFORE, PIREP_HOURS_BEFORE)
                putExtra(NoaaService.LOCATION, location)
                putExtra(NoaaService.FORCE_REFRESH, refresh)
            }
            context.startService(intent)
        }
    }
}
