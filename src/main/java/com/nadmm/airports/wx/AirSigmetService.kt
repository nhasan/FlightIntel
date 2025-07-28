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
import android.text.format.DateUtils
import android.util.Log
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch
import java.util.Locale

class AirSigmetService : NoaaService2("airsigmet", AIRSIGMET_CACHE_MAX_AGE) {
    private val mParser: AirSigmetParser = AirSigmetParser()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_AIRSIGMET) {
                    val type = intent.getStringExtra(TYPE)
                    if (type == TYPE_TEXT) {
                        getAirSigmetText(intent)
                    } else if (type == TYPE_IMAGE) {
                        getAirSigmetImage(intent)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun getAirSigmetText(intent: Intent) {
        val action = intent.action
        val stationId = intent.getStringExtra(STATION_ID)
        val box = intent.getDoubleArrayExtra(COORDS_BOX)
        val hours = intent.getIntExtra(HOURS_BEFORE, 3)
        val cacheOnly = intent.getBooleanExtra(CACHE_ONLY, false)
        val forceRefresh = intent.getBooleanExtra(FORCE_REFRESH, false)

        val xmlFile = getDataFile("AIRSIGMET_$stationId.xml")
        val objFile = getDataFile("AIRSIGMET_$stationId.obj")
        val airSigmet: AirSigmet

        Log.d(TAG, "getAirSigmetText: action=$action, stationId=$stationId")

        if (forceRefresh || (!cacheOnly && !xmlFile.exists())) {
            try {
                val rawQuery = ("datasource=airsigmets"
                        + "&requesttype=retrieve&format=xml"
                        + "&hoursBeforeNow=%d&minLat=%.2f&maxLat=%.2f"
                        + "&minLon=%.2f&maxLon=%.2f")
                val query = String.format(
                    Locale.US, rawQuery,
                    hours, box!![0], box[1], box[2], box[3]
                )
                fetchFromNoaa(query, xmlFile, false)
            } catch (e: Exception) {
                showToast(
                    this@AirSigmetService, "Unable to fetch AirSigmet: "
                            + e.message
                )
            }
        }

        if (objFile.exists()) {
            airSigmet = readObject(objFile) as AirSigmet? ?: AirSigmet()
        } else if (xmlFile.exists()) {
            airSigmet = AirSigmet()
            mParser.parse(xmlFile, airSigmet)
            writeObject(airSigmet, objFile)
        } else {
            airSigmet = AirSigmet()
        }

        // Broadcast the result
        sendSerializableResultIntent(action, stationId, airSigmet)
    }

    private fun getAirSigmetImage(intent: Intent) {
        val action = intent.action
        val code = intent.getStringExtra(IMAGE_CODE)
        val imageName = "sigmet_$code.gif"

        Log.d(TAG, "getAirSigmetImage: action=$action, imageName=$imageName")

        val imageFile = getDataFile(imageName)
        if (!imageFile.exists()) {
            try {
                val path = "/data/products/sigmet/$imageName"
                fetchFromNoaa(path, null, imageFile, false)
            } catch (e: Exception) {
                showToast(
                    this@AirSigmetService, "Unable to fetch AirSigmet image: "
                            + e.message
                )
            }
        }

        // Broadcast the result
        sendImageResultIntent(action, code, imageFile)
    }

    companion object {
        private val TAG = AirSigmetService::class.java.simpleName
        private const val AIRSIGMET_CACHE_MAX_AGE = 30 * DateUtils.MINUTE_IN_MILLIS
    }
}
