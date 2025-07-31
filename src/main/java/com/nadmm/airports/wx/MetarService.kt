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
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import javax.xml.parsers.ParserConfigurationException

class MetarService : NoaaService2(name, METAR_CACHE_MAX_AGE) {
    private val mParser: MetarParser = MetarParser()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_METAR || action == ACTION_CACHE_METAR) {
                    val type = intent.getStringExtra(TYPE)
                    if (type == TYPE_TEXT) {
                        getMetarText(intent)
                    } else if (type == TYPE_GRAPHIC) {
                        getMetarImage(intent)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun getMetarText(intent: Intent) {
        // Get request parameters
        intent.getStringArrayListExtra(STATION_IDS)?.let { stationIds ->
            val action = intent.action
            var cacheOnly = intent.getBooleanExtra(CACHE_ONLY, false)
            if (action == ACTION_CACHE_METAR) {
                // Do not try to use the cache. We are populating the cache.
                cacheOnly = false
            }
            val forceRefresh = intent.getBooleanExtra(FORCE_REFRESH, false)

            Log.d(TAG, "getMetarText: action=$action, stationIds=$stationIds")

            if (forceRefresh || !cacheOnly) {
                val missing = stationIds.filter { forceRefresh || !getObjFile(it).exists() }
                if (missing.isNotEmpty()) {
                    val hours = intent.getIntExtra(HOURS_BEFORE, 3)
                    var tmpFile: File? = null
                    try {
                        tmpFile = File.createTempFile(name, null)
                        val query = ("datasource=metars&requesttype=retrieve"
                                + "&hoursBeforeNow=${hours}&mostRecentForEachStation=constraint"
                                + "&format=xml&stationString=${stationIds.joinToString()}")
                        fetchFromNoaa(query, tmpFile, false)
                        parseMetars(tmpFile, missing)
                    } catch (e: Exception) {
                        showToast(this, "Unable to fetch METAR: ${e.message}")
                    } finally {
                        tmpFile?.delete()
                    }
                }
            }
            if (action == ACTION_GET_METAR) {
                for (stationId in stationIds) {
                    val objFile = getObjFile(stationId)
                    val metar: Metar = if (objFile.exists()) {
                        readObject(objFile) as Metar? ?: Metar()
                    } else {
                        Metar()
                    }
                    // Broadcast the result
                    sendSerializableResultIntent(action, stationId, metar)
                }
            }
        }
    }

    private fun getMetarImage(intent: Intent) {
        intent.getStringExtra(IMAGE_CODE)?.let { code ->
            val imageName = "metars_${code.lowercase()}.gif"
            val imageFile = getDataFile(imageName)
            val action = intent.action
            Log.d(TAG, "getMetarImage: action=$action, code=$code, imageName=$imageName")
            if (!imageFile.exists()) {
                try {
                    val path = "/data/obs/metar/$imageName"
                    fetchFromNoaa(path, null, imageFile, false)
                } catch (e: Exception) {
                    showToast(this@MetarService, "Unable to fetch METAR image: " + e.message)
                }
            }
            // Broadcast the result
            sendImageResultIntent(action, code, imageFile)
        }
    }

    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    private fun parseMetars(xmlFile: File?, stationIds: List<String?>) {
        if (xmlFile?.exists() == true) {
            mParser.parse(xmlFile, stationIds).forEach { metar ->
                writeObject(metar, getObjFile(metar.stationId))
            }
        }
    }

    private fun getObjFile(stationId: String?): File {
        return getDataFile("${name}_${stationId}.obj")
    }

    companion object {
        private val TAG = MetarService::class.java.simpleName
        private const val name = "metar"
        private const val METAR_CACHE_MAX_AGE = 30 * DateUtils.MINUTE_IN_MILLIS

        // Helper function to start the service
        fun startMetarService(context: Context, stationId: String, refresh: Boolean) {
            val intent = Intent(context, MetarService::class.java).apply {
                action = ACTION_GET_METAR
                putExtra(NoaaService.STATION_IDS, arrayListOf(stationId))
                putExtra(NoaaService.TYPE, NoaaService.TYPE_TEXT)
                putExtra(NoaaService.HOURS_BEFORE, NoaaService.METAR_HOURS_BEFORE)
                putExtra(NoaaService.FORCE_REFRESH, refresh)
            }
            context.startService(intent)
        }
    }
}