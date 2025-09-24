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
import android.text.format.DateUtils.HOUR_IN_MILLIS
import android.util.Log
import androidx.core.content.IntentCompat
import androidx.core.os.bundleOf
import com.nadmm.airports.utils.GeoUtils
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch
import java.io.File

class PirepService : NoaaService("pirep", CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_PIREP) {
                    val type = intent.getStringExtra(TYPE)
                    if (type == TYPE_TEXT) {
                        getPirepText(intent)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun getPirepText(intent: Intent) {
        // Get request parameters
        val location = IntentCompat.getParcelableExtra(intent, LOCATION, Location::class.java) ?: return
        val stationId = intent.getStringExtra(STATION_ID) ?: return
        val radiusNM = intent.getIntExtra(RADIUS_NM, 50)
        val forceRefresh = intent.getBooleanExtra(FORCE_REFRESH, false)

        val cachedFile = wxCache.getCachedFile(stationId)
        if (forceRefresh) {
            // If force refresh is requested, delete the cached files
            cachedFile.delete()
        }

        Log.d(TAG, "getPirepText: stationId=$stationId, forceRefresh=$forceRefresh")

        if (!cachedFile.exists()) {
            var xmlFile: File? = null
            try {
                xmlFile = wxCache.createTempFile()
                val query = "id=$stationId&distance=$radiusNM&format=xml"
                Log.d(TAG, "getPirepText: query=$query")
                val success = fetchFromNoaa("/api/data/pirep", query, xmlFile)
                if (success) {
                    val pirep = PirepParser.parse(xmlFile, location, radiusNM)
                    pirep.stationId = stationId
                    wxCache.serializeObject(pirep, stationId)
                }
            } catch (e: Exception) {
                showToast(this, "Unable to fetch PIREP: " + e.message)
            } finally {
                // Clean up the temporary file
                xmlFile?.delete()
            }
        }
        // Send the Pirep
        val pirep = wxCache.deserializeObject<Pirep>(stationId) ?: Pirep()

        val result = bundleOf(
            ACTION to ACTION_GET_PIREP,
            TYPE to TYPE_TEXT,
            RESULT to pirep
        )
        Events.post(result)
    }

    companion object {
        private val TAG = PirepService::class.java.simpleName
        private const val CACHE_MAX_AGE = HOUR_IN_MILLIS
        private const val PIREP_RADIUS_NM = (50 * GeoUtils.STATUTE_MILES_PER_NAUTICAL_MILES).toInt()

        fun startService(context: Context, stationId: String, location: Location, refresh: Boolean) {
            val intent = Intent(context, PirepService::class.java).apply {
                setAction(ACTION_GET_PIREP)
                putExtra(STATION_ID, stationId)
                putExtra(TYPE, TYPE_TEXT)
                putExtra(RADIUS_NM, PIREP_RADIUS_NM)
                putExtra(LOCATION, location)
                putExtra(FORCE_REFRESH, refresh)
            }
            context.startService(intent)
        }
    }
}
