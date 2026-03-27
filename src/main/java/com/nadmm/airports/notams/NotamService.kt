/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.notams

import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import com.nadmm.airports.utils.CoroutineIntentService
import com.nadmm.airports.utils.NetworkUtils.doHttpGet
import com.nadmm.airports.utils.UiUtils.showToast
import java.io.File
import java.net.URL
import java.util.Date

class NotamService : CoroutineIntentService(SERVICE_NAME) {

    override fun onCreate() {
        super.onCreate()
        cleanupCache()
    }

    override suspend fun onHandleIntent(intent: Intent?) {
        val location = intent!!.getStringExtra(LOCATION)
        val force = intent.getBooleanExtra(FORCE_REFRESH, false)
        val notamFile = File(localDataDir, "NOTAM_$location.json")
        if (force || !notamFile.exists()) {
            try {
                fetchNotams(location, notamFile)
            } catch (e: Exception) {
                showToast(this, e.message, Toast.LENGTH_LONG)
            }
        }
        sendResult(location, notamFile)
    }

    private suspend fun fetchNotams(location: String?, notamFile: File) {
        val notamUrl = "https://api.flightintel.com/notams/%s"
        val url = URL(String.format(notamUrl, location))
        val ok = doHttpGet(this, url, notamFile, null, null, null)
        if (ok && notamFile.length() > 0) {
            sendResult(location, notamFile)
        }
    }

    private suspend fun sendResult(location: String?, notamFile: File) {
        val result = Intent()
        result.action = ACTION_GET_NOTAM
        if (notamFile.exists()) {
            result.putExtra(NOTAM_PATH, notamFile.absolutePath)
        }
        result.putExtra(LOCATION, location)
        Events.post(result)
    }

    private fun cleanupCache() {
        val now = Date()
        val files = localDataDir!!.listFiles()
        if (files != null) {
            for (file in files) {
                val age = now.time - file.lastModified()
                if (age > NOTAM_CACHE_MAX_AGE) {
                    file.delete()
                }
            }
        }
    }

    companion object {
        const val ACTION_GET_NOTAM = "flightintel.intent.action.GET_NOTAM"
        const val LOCATION = "LOCATION"
        const val FORCE_REFRESH = "FORCE_REFRESH"
        const val NOTAM_PATH = "NOTAM_PATH"
        private const val NOTAM_CACHE_MAX_AGE = 15 * DateUtils.MINUTE_IN_MILLIS
        private const val SERVICE_NAME = "notam"
    }
}
