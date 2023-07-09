/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2023 Nadeem Hasan <nhasan@nadmm.com>
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
import com.nadmm.airports.utils.UiUtils.showToast
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.xml.parsers.ParserConfigurationException

class MetarService : NoaaService(name, METAR_CACHE_MAX_AGE) {
    private val mParser: MetarParser = MetarParser()

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (action == ACTION_GET_METAR || action == ACTION_CACHE_METAR) {
            val type = intent.getStringExtra(TYPE)
            if (type == TYPE_TEXT) {
                // Get request parameters
                val stationIds = intent.getStringArrayListExtra(STATION_IDS) ?: return
                val hours = intent.getIntExtra(HOURS_BEFORE, 3)
                var cacheOnly = intent.getBooleanExtra(CACHE_ONLY, false)
                val forceRefresh = intent.getBooleanExtra(FORCE_REFRESH, false)
                if (action == ACTION_CACHE_METAR) {
                    // Do not try to use the cache. We are populating the cache.
                    cacheOnly = false
                }
                if (forceRefresh || !cacheOnly) {
                    val missing = stationIds.filter { forceRefresh || !getObjFile(it).exists() }
                    if (missing.isNotEmpty()) {
                        val param = stationIds.joinToString()
                        var tmpFile: File? = null
                        try {
                            tmpFile = File.createTempFile(name, null)
                            val query = String.format(Locale.US, METAR_TEXT_QUERY, hours, param)
                            fetchFromNoaa(query, tmpFile, true)
                            parseMetars(tmpFile, missing)
                        } catch (e: Exception) {
                            showToast(this, "Unable to fetch METAR: " + e.message)
                        } finally {
                            tmpFile?.delete()
                        }
                    }
                }
                if (action == ACTION_GET_METAR) {
                    for (stationId in stationIds) {
                        val objFile = getObjFile(stationId)
                        val metar: Metar = if (objFile.exists()) {
                            readObject(objFile) as Metar
                        } else {
                            Metar()
                        }
                        // Broadcast the result
                        sendSerializableResultIntent(action, stationId, metar)
                    }
                }
            } else if (type == TYPE_IMAGE) {
                val code = intent.getStringExtra(IMAGE_CODE) ?: return
                val imageName = String.format(METAR_IMAGE_NAME, code.lowercase(Locale.getDefault()))
                val imageFile = getDataFile(imageName)
                if (!imageFile.exists()) {
                    try {
                        val path = "$METAR_IMAGE_PATH/$imageName"
                        fetchFromNoaa(path, null, imageFile, false)
                    } catch (e: Exception) {
                        showToast(this, "Unable to fetch METAR image: " + e.message)
                    }
                }
                // Broadcast the result
                sendImageResultIntent(action, code, imageFile)
            }
        }
    }

    @Throws(ParserConfigurationException::class, SAXException::class, IOException::class)
    private fun parseMetars(xmlFile: File?, stationIds: List<String?>) {
        if (xmlFile!!.exists()) {
            mParser.parse(xmlFile, stationIds).forEach { metar ->
                writeObject(metar, getObjFile(metar.stationId))
            }
        }
    }

    private fun getObjFile(stationId: String?): File {
        return getDataFile("${name}_${stationId}.obj")
    }

    companion object {
        private const val name = "metar"
        private const val METAR_CACHE_MAX_AGE = 30 * DateUtils.MINUTE_IN_MILLIS
        private const val METAR_IMAGE_NAME = "metars_%s.gif"
        private const val METAR_TEXT_QUERY = ("datasource=metars&requesttype=retrieve"
                + "&hoursBeforeNow=%d&mostRecentForEachStation=constraint"
                + "&format=xml&compression=gzip&stationString=%s")
        private const val METAR_IMAGE_PATH = "/data/obs/metar"
    }
}