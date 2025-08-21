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
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch
import java.io.File

class TafService : NoaaService2("taf", TAF_CACHE_MAX_AGE) {

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

    private fun getTafText(intent: Intent) {
        // Get request parameters
        val stationId = intent.getStringExtra(STATION_ID) ?: return
        val forceRefresh = intent.getBooleanExtra(FORCE_REFRESH, false)

        if (forceRefresh) {
            cleanupCache(listOf(stationId))
        }

        Log.d(TAG, "getMetarText: stationId=$stationId, forceRefresh=$forceRefresh")

        if (!cacheFileExists(stationId)) {
            var xmlFile: File? = null
            try {
                xmlFile = createTempFile()
                val hoursBeforeNow = intent.getIntExtra(HOURS_BEFORE, 6)
                val query = ("dataSource=tafs&requestType=retrieve"
                        + "&format=xml&hoursBeforeNow=${hoursBeforeNow}&mostRecent=true&stationString=${stationId}")
                fetchFromNoaa(query, xmlFile, false)
                val parser: TafParser = TafParser()
                val taf = parser.parse(xmlFile)
                serializeObject(taf, stationId)
            } catch (e: Exception) {
                showToast(this, "Unable to fetch TAF: " + e.message)
            } finally {
                xmlFile?.delete()
            }
        }

        val taf = deserializeObject<Taf>(stationId) ?: Taf()
        sendParcelableResultIntent(intent.action, stationId, taf)
    }

    companion object {
        private val TAG = TafService::class.java.simpleName
        private const val TAF_CACHE_MAX_AGE = 30 * DateUtils.MINUTE_IN_MILLIS

        // Helper function to start the service
        fun startTafService(context: Context, stationId: String, refresh: Boolean) {
            val intent = Intent(context, TafService::class.java).apply {
                action = ACTION_GET_TAF
                putExtra(NoaaService.STATION_IDS, arrayListOf(stationId))
                putExtra(NoaaService.TYPE, NoaaService.TYPE_TEXT)
                putExtra(NoaaService.HOURS_BEFORE, NoaaService.METAR_HOURS_BEFORE)
                putExtra(NoaaService.FORCE_REFRESH, refresh)
            }
            context.startService(intent)
        }
    }
}
