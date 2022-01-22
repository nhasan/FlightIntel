/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2016 Nadeem Hasan <nhasan@nadmm.com>
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

import android.app.IntentService
import android.content.Intent
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.utils.FileUtils
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.UiUtils
import java.io.File

abstract class AeroNavService(private val mName: String) : IntentService(mName) {
    private val AERONAV_HOST = "aeronav.faa.gov"
    protected var serviceDataDir: File? = null
        private set

    override fun onCreate() {
        super.onCreate()
        serviceDataDir = SystemUtils.getExternalDir(this, mName)
    }

    protected fun getCycleDir(cycle: String?): File {
        val dir = File(serviceDataDir, cycle)
        if (!dir.exists()) {
            cleanupOldCycles()
            dir.mkdir()
        }
        return dir
    }

    protected fun fetch(path: String?, file: File?): Boolean {
        try {
            return NetworkUtils.doHttpsGet(this, AERONAV_HOST, path, file)
        } catch (e: Exception) {
            UiUtils.showToast(this, "Error: " + e.message)
        }
        return false
    }

    protected fun sendResult(action: String?, cycle: String?, pdfFile: File) {
        val extras = Bundle()
        extras.putString(CYCLE_NAME, cycle)
        extras.putString(PDF_NAME, pdfFile.name)
        if (pdfFile.exists()) {
            extras.putString(PDF_PATH, pdfFile.absolutePath)
        }
        sendResult(action, extras)
    }

    protected fun sendResult(action: String?, extras: Bundle?) {
        val result = Intent()
        result.action = action
        result.putExtras(extras!!)
        val bm = LocalBroadcastManager.getInstance(this)
        bm.sendBroadcast(result)
    }

    protected fun cleanupOldCycles() {
        val cycles = serviceDataDir!!.listFiles()
        if (cycles != null) {
            for (cycle in cycles) {
                FileUtils.removeDir(cycle)
            }
        }
    }

    companion object {
        const val ACTION_GET_AFD = "flightintel.intent.action.GET_AFD"
        const val ACTION_CHECK_AFD = "flightintel.intent.action.CHECK_AFD"
        const val ACTION_GET_CHARTS = "flightintel.intent.action.GET_CHARTS"
        const val ACTION_CHECK_CHARTS = "flightintel.intent.action.CHECK_CHARTS"
        const val ACTION_DELETE_CHARTS = "flightintel.intent.action.DELETE_CHARTS"
        const val ACTION_COUNT_CHARTS = "flightintel.intent.action.COUNT_CHARTS"
        const val CYCLE_NAME = "CYCLE_NAME"
        const val TPP_VOLUME = "TPP_VOLUME"
        const val PDF_NAME = "PDF_NAME"
        const val PDF_PATH = "PDF_PATH"
        const val PDF_NAMES = "PDF_NAMES"
        const val PDF_COUNT = "PDF_COUNT"
        const val DOWNLOAD_IF_MISSING = "DOWNLOAD_IF_MISSING"
    }
}