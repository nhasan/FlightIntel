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
package com.nadmm.airports.aeronav

import android.app.Service
import com.nadmm.airports.utils.FileUtils
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

abstract class AeroNavService(private val name: String) : Service() {
    private val serviceJob = SupervisorJob() // Use SupervisorJob for better error handling in children
    // Coroutine scope tied to Dispatchers.IO for background work
    protected val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    var serviceDataDir: File? = null
        private set

    override fun onCreate() {
        super.onCreate()
        serviceDataDir = SystemUtils.getExternalDir(this, name)
    }

    protected fun getCycleDir(cycle: String): File {
        val dir = File(serviceDataDir, cycle)
        if (!dir.exists()) {
            cleanupOldCycles()
            dir.mkdir()
        }
        return dir
    }

    protected fun fetch(path: String, file: File): Boolean {
        try {
            return NetworkUtils.doHttpsGet(this, AERONAV_HOST, path, file)
        } catch (e: Exception) {
            UiUtils.showToast(this, "Error: " + e.message)
        }
        return false
    }

    private fun cleanupOldCycles() {
        val cycles = serviceDataDir?.listFiles() ?: return
        for (cycle in cycles) {
            FileUtils.removeDir(cycle)
        }
    }

    companion object {
        const val AERONAV_HOST = "aeronav.faa.gov"
        const val ACTION_GET_AFD = "flightintel.intent.action.GET_AFD"
        const val ACTION_CHECK_AFD = "flightintel.intent.action.CHECK_AFD"
        const val ACTION_GET_CHARTS = "flightintel.intent.action.GET_CHARTS"
        const val ACTION_CHECK_CHARTS = "flightintel.intent.action.CHECK_CHARTS"
        const val ACTION_DELETE_CHARTS = "flightintel.intent.action.DELETE_CHARTS"
        const val ACTION_COUNT_CHARTS = "flightintel.intent.action.COUNT_CHARTS"
        const val CYCLE_NAME = "CYCLE_NAME"
        const val TPP_VOLUME = "TPP_VOLUME"
        const val PDF_NAME = "PDF_NAME"
        const val PDF_NAMES = "PDF_NAMES"
        const val DOWNLOAD_IF_MISSING = "DOWNLOAD_IF_MISSING"
    }
}