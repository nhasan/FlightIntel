/*
 * FlightIntel for Pilots
 *
 * Copyright 2025 Nadeem Hasan <nhasan@nadmm.com>
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
import android.text.format.DateUtils
import android.util.Log
import com.nadmm.airports.utils.SystemUtils
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Date

class WxCache(
    val context: Context,
    val serviceName: String,
    val maxAgeMillis: Long = DEFAULT_CACHE_AGE_MILLIS
    ) {
    private val dataDirectory: File = SystemUtils.getExternalDir(context, "wx/$serviceName")

    init {
        // Ensure the directory exists
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs()
        }
        // Remove any old files from cache first
        cleanupCache()
    }

    private fun cleanupCache() {
        // Delete all files that are older
        val files = dataDirectory.listFiles() ?: return
        val now = Date()
        for (file in files) {
            val age = now.time - file.lastModified()
            if (age > maxAgeMillis) {
                Log.d(TAG, "Deleting aged cached file: ${file.name}")
                file.delete()
            }
        }
    }

    fun getFile(filename: String): File {
        return File(dataDirectory, filename)
    }

    fun cleanupCache(stationIds: List<String>) {
        stationIds.forEach { stationId ->
            val jsonFile = getCachedFile(stationId)
            if (jsonFile.exists()) {
                Log.d(TAG, "Deleting cached file: ${jsonFile.name}")
                jsonFile.delete()
            }
        }
    }

    fun createTempFile(): File {
        return File.createTempFile(serviceName, ".tmp", dataDirectory).apply {
            deleteOnExit() // Ensure the temp file is deleted when the JVM exits
        }
    }

    fun fileExists(stationId: String): Boolean {
        val file = getCachedFile(stationId)
        return file.exists() && (Date().time - file.lastModified() <= maxAgeMillis)
    }

    fun getCachedFile(stationId: String): File {
        return getFile("${serviceName}_${stationId}.json")
    }

    inline fun <reified T> serializeObject(obj: T, stationId: String) {
        val jsonFile = getCachedFile(stationId)
        jsonFile.bufferedWriter().use { writer ->
            val json = Json.encodeToString(obj)
            writer.write(json)
        }
    }

    inline fun <reified T> deserializeObject(stationId: String) : T? {
        val jsonFile = getCachedFile(stationId)
        return if (jsonFile.exists()) {
            jsonFile.bufferedReader().use { reader ->
                val json = reader.readText()
                Json.decodeFromString<T>(json)
            }
        } else {
            null
        }
    }

    companion object {
        const val TAG = "WxCache"
        // Default cache age is 3 hours
        const val DEFAULT_CACHE_AGE_MILLIS = 3 * DateUtils.HOUR_IN_MILLIS
    }
}