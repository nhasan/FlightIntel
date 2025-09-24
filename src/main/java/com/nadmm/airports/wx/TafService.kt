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
import android.text.format.DateUtils
import android.util.Log
import androidx.core.os.bundleOf
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch
import java.io.File

class TafService : NoaaService("taf", CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_TAF) {
                    val type = intent.getStringExtra(TYPE)
                    if (type == TYPE_TEXT) {
                        getTafText(intent)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun getTafText(intent: Intent) {
        // Get request parameters
        val stationId = intent.getStringExtra(STATION_ID) ?: return
        val forceRefresh = intent.getBooleanExtra(FORCE_REFRESH, false)

        val cachedFile = wxCache.getCachedFile(stationId)
        if (forceRefresh) {
            // If force refresh is requested, delete the cached files
            cachedFile.delete()
        }

        Log.d(TAG, "getTafText: stationId=$stationId, forceRefresh=$forceRefresh")

        if (!cachedFile.exists()) {
            var xmlFile: File? = null
            try {
                xmlFile = wxCache.createTempFile()
                val query = ("dataSource=tafs&requestType=retrieve"
                        + "&hoursBeforeNow=${HOURS_BEFORE}&mostRecentForEachStation=constraint"
                        + "&format=xml&stationString=${stationId}")
                fetchFromNoaa(query, xmlFile)
                val parser = TafParser()
                val taf = parser.parse(xmlFile)
                wxCache.serializeObject(taf, stationId)
            } catch (e: Exception) {
                showToast(this, "Unable to fetch TAF: " + e.message)
            } finally {
                xmlFile?.delete()
            }
        }

        val taf = wxCache.deserializeObject<Taf>(stationId) ?: Taf()
        val result = bundleOf(
            ACTION to ACTION_GET_TAF,
            TYPE to TYPE_TEXT,
            RESULT to taf
        )
        Events.post(result)
    }

    companion object {
        private val TAG = TafService::class.java.simpleName
        private const val CACHE_MAX_AGE = 30 * DateUtils.MINUTE_IN_MILLIS
        private const val HOURS_BEFORE = 6

        const val TAF_RADIUS = 25

        // Helper function to start the service
        fun startService(context: Context, stationId: String, refresh: Boolean) {
            val intent = Intent(context, TafService::class.java).apply {
                action = ACTION_GET_TAF
                putExtra(STATION_ID, stationId)
                putExtra(TYPE, TYPE_TEXT)
                putExtra(FORCE_REFRESH, refresh)
            }
            context.startService(intent)
        }
    }
}
