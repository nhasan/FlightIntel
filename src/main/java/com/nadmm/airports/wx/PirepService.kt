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

import android.content.Intent
import android.location.Location
import android.text.format.DateUtils
import androidx.core.content.IntentCompat
import com.nadmm.airports.utils.GeoUtils
import com.nadmm.airports.utils.UiUtils.showToast
import java.util.Locale

class PirepService : NoaaService("pirep", PIREP_CACHE_MAX_AGE) {
    private val mParser: PirepParser = PirepParser()

    override fun onHandleIntent(intent: Intent?) {
        intent ?: return
        val action = intent.action
        if (action == ACTION_GET_PIREP) {
            val type = intent.getStringExtra(TYPE)
            if (type == TYPE_TEXT) {
                // Get request parameters
                val location = IntentCompat.getParcelableExtra(intent, LOCATION, Location::class.java) ?: return
                val stationId = intent.getStringExtra(STATION_ID)
                val radiusNM = intent.getIntExtra(RADIUS_NM, 50)
                val hours = intent.getIntExtra(HOURS_BEFORE, 3)
                val cacheOnly = intent.getBooleanExtra(CACHE_ONLY, false)
                val forceRefresh = intent.getBooleanExtra(FORCE_REFRESH, false)

                val xmlFile = getDataFile("PIREP_$stationId.xml")
                val objFile = getDataFile("PIREP_$stationId.obj")
                val pirep: Pirep?

                if (forceRefresh || (!cacheOnly && !xmlFile.exists())) {
                    try {
                        val rawQuery = ("dataSource=aircraftreports&requestType=retrieve&format=xml"
                                + "&hoursBeforeNow=%d&radialDistance=%.0f;%.2f,%.2f")
                        val query = String.format(
                            Locale.US, rawQuery, hours,
                            radiusNM * GeoUtils.STATUTE_MILES_PER_NAUTICAL_MILES,
                            location.longitude, location.latitude
                        )
                        fetchFromNoaa(query, xmlFile, false)
                    } catch (e: Exception) {
                        showToast(this, "Unable to fetch PIREP: " + e.message)
                    }
                }

                if (objFile.exists()) {
                    pirep = readObject(objFile) as Pirep?
                } else if (xmlFile.exists()) {
                    pirep = Pirep()
                    mParser.parse(xmlFile, pirep, location, radiusNM)
                    writeObject(pirep, objFile)
                } else {
                    pirep = Pirep()
                }

                // Broadcast the result
                sendSerializableResultIntent(action, stationId, pirep)
            } else if (type == TYPE_IMAGE) {
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
        }
    }

    companion object {
        private const val PIREP_CACHE_MAX_AGE = DateUtils.HOUR_IN_MILLIS
    }
}
