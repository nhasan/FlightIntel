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

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import android.text.format.DateUtils.HOUR_IN_MILLIS
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.utils.NetworkUtils.doHttpsGet
import com.nadmm.airports.utils.SystemUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.Date
import java.util.zip.GZIPInputStream

abstract class NoaaService(protected val serviceName: String, protected val maxAgeMillis: Long) : Service() {
    private var dataDirectory: File? = null
    private val serviceJob = SupervisorJob() // Use SupervisorJob for better error handling in children
    // Coroutine scope tied to Dispatchers.IO for background work
    protected val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()

        dataDirectory = SystemUtils.getExternalDir(this, "wx/$serviceName")
        // Remove any old files from cache first
        cleanupCache(dataDirectory, maxAgeMillis)
        Log.d(TAG, "onCreate() called for $serviceName service")
    }

    override fun onBind(intent: Intent): IBinder? {
        // We are not using a bound service, so return null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called for $serviceName service")
        // Cancel all coroutines when the service is destroyed
        serviceJob.cancel() // This will cancel all coroutines launched in serviceScope
    }

    private fun cleanupCache(dir: File?, maxAge: Long) {
        // Delete all files that are older
        val files = dir?.listFiles() ?: return
        val now = Date()
        for (file in files) {
            val age = now.time - file.lastModified()
            if (age > maxAge) {
                Log.d(TAG, "Deleting old cached file: ${file.name}")
                file.delete()
            }
        }
    }

    protected fun cleanupCache(stationIds: List<String>) {
        stationIds.forEach { stationId ->
            val jsonFile = getJsonFile(stationId)
            if (jsonFile.exists()) {
                Log.d(TAG, "Deleting cached file: ${jsonFile.name}")
                jsonFile.delete()
            }
        }
    }

    protected fun cacheFileExists(stationId: String): Boolean {
        return getJsonFile(stationId).exists()
    }

    protected fun fetchFromNoaa(query: String?, file: File, compressed: Boolean = false): Boolean {
        return fetchFromNoaa(ADDS_DATASERVER_PATH, query, file, compressed)
    }

    protected fun fetchFromNoaa(path: String, query: String?, file: File, compressed: Boolean): Boolean {
        return fetch(AWC_HOST, path, query, file, compressed)
    }

    protected fun fetch(host: String, path: String, query: String?, file: File, compressed: Boolean): Boolean {
        return doHttpsGet(
            this, host, path, query, file, null, null,
            if (compressed) GZIPInputStream::class.java else null
        )
    }

    protected fun getDataFile(name: String): File {
        return File(dataDirectory, name)
    }

    protected fun getObjFile(stationId: String): File {
        return getDataFile("${serviceName}_${stationId}.obj")
    }

    protected fun getJsonFile(stationId: String): File {
        return getDataFile("${serviceName}_${stationId}.json")
    }

    protected fun createTempFile(): File {
        return File.createTempFile(serviceName, ".tmp", dataDirectory).apply {
            deleteOnExit() // Ensure the temp file is deleted when the JVM exits
        }
    }

    protected fun sendParcelableResultIntent(
        action: String?, stationId: String?,
        result: Parcelable?
    ) {
        val intent = makeResultIntent(action, TYPE_TEXT)
        intent.putExtra(STATION_ID, stationId)
        intent.putExtra(RESULT, result)
        sendResultIntent(intent)
    }

    protected fun sendSerializableResultIntent(
        action: String?, stationId: String?,
        result: Serializable?
    ) {
        val intent = makeResultIntent(action, TYPE_TEXT)
        intent.putExtra(STATION_ID, stationId)
        intent.putExtra(RESULT, result)
        sendResultIntent(intent)
    }

    protected fun sendImageResultIntent(action: String?, code: String?, result: File) {
        val intent = makeResultIntent(action, TYPE_GRAPHIC)
        intent.putExtra(IMAGE_CODE, code)
        if (result.exists()) {
            intent.putExtra(RESULT, result.absolutePath)
        }
        sendResultIntent(intent)
    }

    protected fun sendFileResultIntent(action: String?, stationId: String?, result: File) {
        val intent = makeResultIntent(action, TYPE_TEXT)
        intent.putExtra(STATION_ID, stationId)
        if (result.exists()) {
            intent.putExtra(RESULT, result.getAbsolutePath())
        }
        sendResultIntent(intent)
    }

    private fun makeResultIntent(action: String?, type: String?): Intent {
        val intent = Intent()
        intent.setAction(action)
        intent.putExtra(TYPE, type)
        return intent
    }

    private fun sendResultIntent(intent: Intent) {
        val bm = LocalBroadcastManager.getInstance(this)
        bm.sendBroadcast(intent)
    }

    protected inline fun <reified T> serializeObject(obj: T, stationId: String) {
        val jsonFile = getJsonFile(stationId)
        jsonFile.bufferedWriter().use { writer ->
            val json = Json.encodeToString(obj)
            writer.write(json)
        }
    }

    protected inline fun <reified T> deserializeObject(stationId: String) : T? {
        val jsonFile = getJsonFile(stationId)
        return if (jsonFile.exists()) {
            jsonFile.bufferedReader().use { reader ->
                val json = reader.readText()
                Json.decodeFromString<T>(json)
            }
        } else {
            null
        }
    }

    protected fun writeObject(obj: Any?, objFile: File?) {
        FileOutputStream(objFile).use { fos ->
            ObjectOutputStream(fos).use { oos ->
                oos.writeObject(obj)
            }
        }
    }

    protected inline fun <reified T> readObject(objFile: File): T? {
        ObjectInputStream(FileInputStream(objFile)).use { fis ->
            return fis.readObject() as? T
        }
    }

    companion object {
        private val TAG: String = NoaaService::class.java.simpleName

        protected const val AWC_HOST: String = "aviationweather.gov"
        protected const val ADDS_DATASERVER_PATH: String = "/cgi-bin/data/dataserver.php"

        const val METAR_HOURS_BEFORE = 3
        const val TAF_HOURS_BEFORE = 3
        const val TAF_RADIUS = 25
        const val PIREP_RADIUS_NM = 50
        const val PIREP_HOURS_BEFORE = 3
        const val PIREP_CACHE_MAX_AGE = HOUR_IN_MILLIS
        const val PROGCHART_CACHE_MAX_AGE = 3 * HOUR_IN_MILLIS

        const val STATION_ID: String = "STATION_ID"
        const val STATION_IDS: String = "STATION_IDS"
        const val CACHE_ONLY: String = "CACHE_ONLY"
        const val FORCE_REFRESH: String = "FORCE_REFRESH"
        const val RADIUS_NM: String = "RADIUS_NM"
        const val LOCATION: String = "LOCATION"
        const val HOURS_BEFORE: String = "HOURS_BEFORE"
        const val COORDS_BOX: String = "COORDS_BOX"
        const val IMAGE_TYPE: String = "IMAGE_TYPE"
        const val IMAGE_CODE: String = "IMAGE_CODE"
        const val TEXT_TYPE: String = "TEXT_TYPE"
        const val TEXT_CODE: String = "TEXT_CODE"
        const val RESULT: String = "RESULT"

        const val TYPE: String = "TYPE"
        const val TYPE_TEXT: String = "TYPE_TEXT"
        const val TYPE_GRAPHIC: String = "TYPE_GRAPHIC"

        const val ACTION_GET_METAR: String = "flightintel.intent.wx.action.GET_METAR"
        const val ACTION_CACHE_METAR: String = "flightintel.intent.wx.action.CACHE_METAR"
        const val ACTION_GET_TAF: String = "flightintel.intent.wx.action.GET_TAF"
        const val ACTION_GET_PIREP: String = "flightintel.intent.wx.action.GET_PIREP"
        const val ACTION_GET_AIRSIGMET: String = "flightintel.intent.wx.action.GET_AIRSIGMET"
        const val ACTION_GET_PROGCHART: String = "flightintel.intent.wx.action.GET_PROGCHART"
        const val ACTION_GET_ICING: String = "flightintel.intent.action.wx.GET_ICING"
        const val ACTION_GET_FA: String = "flightintel.intent.action.wx.GET_FA"
        const val ACTION_GET_FB: String = "flightintel.intent.action.wx.GET_FB"
        const val ACTION_GET_GFA: String = "flightintel.intent.action.wx.GET_GFA"
    }
}
