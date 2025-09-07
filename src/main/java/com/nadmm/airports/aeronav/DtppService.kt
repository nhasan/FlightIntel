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

import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

class DtppService : AeroNavService("dtpp") {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            intent?.action?.let { action ->
                when (action) {
                    ACTION_GET_CHARTS -> {
                        getCharts(intent)
                    }

                    ACTION_CHECK_CHARTS -> {
                        getCharts(intent)
                    }

                    ACTION_DELETE_CHARTS -> {
                        deleteCharts(intent)
                    }

                    ACTION_COUNT_CHARTS -> {
                        countCharts(intent)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun getCharts(intent: Intent) {
        val action = intent.action ?: return
        val tppCycle = intent.getStringExtra(CYCLE_NAME) ?: return
        val tppVolume = intent.getStringExtra(TPP_VOLUME) ?: return
        val pdfNames = intent.getStringArrayListExtra(PDF_NAMES) ?: return
        val dir = getVolumeDir(tppCycle, tppVolume)
        for (pdfName in pdfNames) {
            val pdfFile: File = if (pdfName == "legendAD.pdf" || pdfName == "frntmatter.pdf") {
                File(getCycleDir(tppCycle), pdfName)
            } else {
                File(dir, pdfName)
            }
            if (!pdfFile.exists()) {
                val download = intent.getBooleanExtra(DOWNLOAD_IF_MISSING, true)
                if (download) {
                    downloadChart(tppCycle, pdfFile)
                }
            }

            Events.post(Result(
                    action = action,
                    cycle = tppCycle,
                    volume = tppVolume,
                    pdfName = pdfName,
                    pdfPath = if (pdfFile.exists()) pdfFile.absolutePath else ""
                )
            )
        }
    }

    private suspend fun deleteCharts(intent: Intent) {
        val tppCycle = intent.getStringExtra(CYCLE_NAME) ?: return
        val tppVolume = intent.getStringExtra(TPP_VOLUME) ?: return
        val pdfNames = intent.getStringArrayListExtra(PDF_NAMES) ?: return
        val dir = getVolumeDir(tppCycle, tppVolume)
        for (pdfName in pdfNames) {
            val pdfFile = File(dir, pdfName)
            if (pdfFile.exists()) {
                pdfFile.delete()
            }

            Events.post(Result(
                    action = ACTION_CHECK_CHARTS,
                    cycle = tppCycle,
                    volume = tppVolume
                )
            )
        }
    }

    private suspend fun countCharts(intent: Intent) {
        val tppCycle = intent.getStringExtra(CYCLE_NAME) ?: return
        val tppVolume = intent.getStringExtra(TPP_VOLUME) ?: return
        val dir = getVolumeDir(tppCycle, tppVolume)
        val files = dir.list()
        val count = files?.size ?: 0

        Events.post(Result(
                action = ACTION_COUNT_CHARTS,
                cycle = tppCycle,
                volume = tppVolume,
                count = count
            )
        )
    }

    private fun downloadChart(tppCycle: String?, pdfFile: File) {
        val path = "/d-tpp/$tppCycle/${pdfFile.name}"
        fetch(path, pdfFile)
    }

    private fun getVolumeDir(cycle: String?, tppVolume: String): File {
        val dir = File(getCycleDir(cycle!!), tppVolume)
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }

    data class Result(
        val action: String,
        val cycle: String,
        val volume: String,
        val pdfName: String = "",
        val pdfPath: String = "",
        val count: Int = 0
    )

    object Events {
        private val _events = MutableSharedFlow<Result>()
        val events = _events.asSharedFlow()

        suspend fun post(result: Result)  {
            _events.emit(result)
        }
    }
}