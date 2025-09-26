/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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
import android.text.format.DateUtils
import android.util.Log
import androidx.core.os.bundleOf
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch
import java.io.File

class MetarService : NoaaService("metar", CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_METAR || action == ACTION_CACHE_METAR) {
                    val type = intent.getStringExtra(TYPE)
                    if (type == TYPE_TEXT) {
                        getMetarText(intent)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun getMetarText(intent: Intent) {
        // Get request parameters
        val stationIds = intent.getStringArrayListExtra(STATION_IDS) ?: return

        val action = intent.action
        var cacheOnly = intent.getBooleanExtra(CACHE_ONLY, false)
        if (action == ACTION_CACHE_METAR) {
            // Do not try to use the cache. We are populating the cache.
            cacheOnly = false
        }
        val forceRefresh = intent.getBooleanExtra(FORCE_REFRESH, false)

        Log.d(TAG, "getMetarText: stationIds=$stationIds, cacheOnly=$cacheOnly, forceRefresh=$forceRefresh")

        if (forceRefresh) {
            wxCache.cleanupCache(stationIds)
        }

        val missing = stationIds.filterNot { stationId -> cacheOnly || wxCache.fileExists(stationId) }
        if (missing.isNotEmpty()) {
            var xmlFile: File? = null
            try {
                xmlFile =  wxCache.createTempFile()
                val query = ("ids=${missing.joinToString(",")}&format=xml")
                val success = fetchFromNoaa("/api/data/metar", query, xmlFile)
                if (success) {
                    parseMetars(xmlFile, missing)
                }
            } catch (e: Exception) {
                showToast(this, "Unable to fetch METAR: ${e.message}")
            } finally {
                // Clean up the temporary file
                xmlFile?.delete()
            }
        }

        // Now read the METAR objects from the cache
        if (action == ACTION_GET_METAR) {
            for (stationId in stationIds) {
                val metar = wxCache.deserializeObject<Metar>(stationId) ?: Metar(stationId = stationId)
                val result = bundleOf(
                    ACTION to action,
                    TYPE to TYPE_TEXT,
                    RESULT to metar
                )
                Events.post(result)
            }
        }
    }

    private suspend fun parseMetars(xmlFile: File, stationIds: List<String>) {
        if (xmlFile.exists()) {
            val parser = MetarParser()
            parser.parse(xmlFile, stationIds).forEach { metar ->
                wxCache.serializeObject(metar, metar.stationId!!)
            }
        }
    }

    companion object {
        private val TAG = MetarService::class.java.simpleName
        private const val CACHE_MAX_AGE = 30 * DateUtils.MINUTE_IN_MILLIS

        // Helper function to start the service
        fun startService(context: Context, stationId: String, force: Boolean, cacheOnly: Boolean = false) {
            val intent = Intent(context, MetarService::class.java).apply {
                action = ACTION_GET_METAR
                putExtra(STATION_IDS, arrayListOf(stationId))
                putExtra(TYPE, TYPE_TEXT)
                putExtra(FORCE_REFRESH, force)
                putExtra(CACHE_ONLY, cacheOnly)
            }
            context.startService(intent)
        }
    }
}