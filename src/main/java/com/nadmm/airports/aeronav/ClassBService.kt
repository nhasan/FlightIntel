/*
 * FlightIntel for Pilots
 *
 * Copyright 2016-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.aeronav

import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.text.format.DateUtils
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.utils.ClassBUtils
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class ClassBService : AeroNavService("classb") {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            intent?.action?.let { action ->
                if (action == ACTION_GET_CLASSB_GRAPHIC) {
                    getClassBGraphic(intent)
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        serviceDataDir?.let { cleanupCache(it) }
    }

    private fun cleanupCache(dir: File) {
        // Delete all files that are older
        val now = Date()
        dir.listFiles()?.let {
            for (file in it) {
                val age = now.time - file.lastModified()
                if (age > DateUtils.DAY_IN_MILLIS) {
                    file.delete()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun getClassBGraphic(intent: Intent) {
        val faaCode = intent.getStringExtra(Airports.FAA_CODE)
        val classBFilename = ClassBUtils.getClassBFilename(faaCode) ?: return
        val pdfFile = File(serviceDataDir, classBFilename)
        if (!pdfFile.exists()) {
            try {
                NetworkUtils.doHttpsGet(
                    this,
                    FAA_HOST,
                    "$CLASS_B_PATH/$classBFilename",
                    pdfFile
                )
            } catch (e: Exception) {
                UiUtils.showToast(this, "Error: " + e.message)
            }
        }

        if (pdfFile.exists()) {
            val extras = Bundle()
            extras.putString(Airports.FAA_CODE, faaCode)
            extras.putString(PDF_PATH, pdfFile.absolutePath)

            Events.post(pdfFile.absolutePath)
        }
    }

    object Events {
        private val _events = MutableSharedFlow<String>()
        val events = _events.asSharedFlow()

        suspend fun post(path: String) {
            _events.emit(path)
        }
    }

    companion object {
        const val ACTION_GET_CLASSB_GRAPHIC = "flightintel.intent.action.ACTION_GET_CLASSB_GRAPHIC"
        const val PDF_PATH = "PDF_PATH"
        private const val FAA_HOST = "aeronav.faa.gov"
        private const val CLASS_B_PATH = "/visual/vfr_class_B/"
    }
}