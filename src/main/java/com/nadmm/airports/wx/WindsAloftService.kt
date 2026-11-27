/*
 * FlightIntel for Pilots
 *
 * Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
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
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import androidx.core.content.IntentCompat
import com.nadmm.airports.utils.UiUtils.showToast
import java.util.Locale

/**
 * Fetches winds aloft for a station from NOAA's GFS model through
 * Open-Meteo, as the NWS no longer issues its FB forecasts, and builds the
 * table with [WindsAloftBuilder].
 */
class WindsAloftService : NoaaService("windsaloft", CACHE_MAX_AGE) {

    override suspend fun onHandleIntent(intent: Intent?) {
        if (intent?.action == ACTION_GET_WINDS_ALOFT) {
            getWindsAloft(intent)
        }
    }

    private suspend fun getWindsAloft(intent: Intent) {
        val location = IntentCompat.getParcelableExtra(intent, LOCATION, Location::class.java) ?: return
        val stationId = intent.getStringExtra(STATION_ID) ?: return
        val elevationFt = intent.getIntExtra(ELEVATION_FT, 0)
        val cachedFile = wxCache.getCachedFile(stationId)
        // Also refetch once the first forecast's hour is over, so the first
        // card is always the hour under way
        val cached = if (cachedFile.exists()) wxCache.deserializeObject<WindsAloft>(stationId) else null
        val firstHourOver = cached?.forecasts?.firstOrNull()
            ?.let { it.timeMillis + HOUR_IN_MILLIS <= System.currentTimeMillis() } ?: false
        if (intent.getBooleanExtra(FORCE_REFRESH, false) || firstHourOver) {
            cachedFile.delete()
        }

        if (!cachedFile.exists()) {
            val jsonFile = wxCache.createTempFile()
            try {
                val query = String.format(Locale.US,
                    "latitude=%.4f&longitude=%.4f&hourly=%s&wind_speed_unit=kn&forecast_hours=%d",
                    location.latitude, location.longitude,
                    WindsAloftBuilder.hourlyVariables(), FORECAST_HOURS)
                if (fetch(OPEN_METEO_HOST, "/v1/gfs", query, jsonFile)) {
                    val winds = WindsAloftBuilder.build(
                        jsonFile.readText(), stationId, elevationFt, System.currentTimeMillis())
                    if (winds.forecasts.isNotEmpty()) {
                        wxCache.serializeObject(winds, stationId)
                    }
                }
            } catch (e: Exception) {
                showToast(this, "Unable to fetch winds aloft: " + e.message)
            } finally {
                jsonFile.delete()
            }
        }

        Events.post(Result(
            action = ACTION_GET_WINDS_ALOFT,
            type = TYPE_TEXT,
            result = wxCache.deserializeObject<WindsAloft>(stationId) ?: WindsAloft(stationId)
        ))
    }

    companion object {
        private const val OPEN_METEO_HOST = "api.open-meteo.com"
        // The model runs every 6 hours, so this is fresh enough
        private const val CACHE_MAX_AGE = 30 * MINUTE_IN_MILLIS
        // Enough for the forecast 24 hours ahead
        private const val FORECAST_HOURS = 26
        private const val ELEVATION_FT = "ELEVATION_FT"

        fun startService(
            context: Context, stationId: String, location: Location,
            elevationFt: Int, refresh: Boolean
        ) {
            context.startService(Intent(context, WindsAloftService::class.java).apply {
                action = ACTION_GET_WINDS_ALOFT
                putExtra(STATION_ID, stationId)
                putExtra(LOCATION, location)
                putExtra(ELEVATION_FT, elevationFt)
                putExtra(FORCE_REFRESH, refresh)
            })
        }
    }
}
