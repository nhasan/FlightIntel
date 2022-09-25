/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2022 Nadeem Hasan <nhasan@nadmm.com>
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
import java.io.File

class DtppService : AeroNavService(DTPP) {
    override fun onHandleIntent(intent: Intent?) {
        when (intent!!.action) {
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

    private fun getCharts(intent: Intent?) {
        val action = intent!!.action
        val tppCycle = intent.getStringExtra(CYCLE_NAME)
        val tppVolume = intent.getStringExtra(TPP_VOLUME)
        val pdfNames = intent.getStringArrayListExtra(PDF_NAMES)
        val dir = getVolumeDir(tppCycle, tppVolume!!)
        for (pdfName in pdfNames!!) {
            val pdfFile: File = if (pdfName == "legendAD.pdf" || pdfName == "frntmatter.pdf") {
                File(getCycleDir(tppCycle!!), pdfName)
            } else {
                File(dir, pdfName)
            }
            if (!pdfFile.exists()) {
                val download = intent.getBooleanExtra(DOWNLOAD_IF_MISSING, true)
                if (download) {
                    downloadChart(tppCycle, pdfFile)
                }
            }
            sendResult(action!!, tppCycle!!, pdfFile)
        }
    }

    private fun deleteCharts(intent: Intent?) {
        val tppCycle = intent!!.getStringExtra(CYCLE_NAME)
        val tppVolume = intent.getStringExtra(TPP_VOLUME)
        val pdfNames = intent.getStringArrayListExtra(PDF_NAMES)
        val dir = getVolumeDir(tppCycle, tppVolume!!)
        for (pdfName in pdfNames!!) {
            val pdfFile = File(dir, pdfName)
            if (pdfFile.exists()) {
                pdfFile.delete()
            }
            sendResult(ACTION_CHECK_CHARTS, tppCycle!!, pdfFile)
        }
    }

    private fun countCharts(intent: Intent?) {
        val tppCycle = intent!!.getStringExtra(CYCLE_NAME)
        val tppVolume = intent.getStringExtra(TPP_VOLUME)
        val dir = getVolumeDir(tppCycle, tppVolume!!)
        val files = dir.list()
        val count = files?.size ?: 0
        val extras = Bundle()
        extras.putString(CYCLE_NAME, tppCycle)
        extras.putString(TPP_VOLUME, tppVolume)
        extras.putInt(PDF_COUNT, count)
        sendResult(ACTION_COUNT_CHARTS, extras)
    }

    private fun downloadChart(tppCycle: String?, pdfFile: File) {
        val path: String = if (pdfFile.name == "legendAD.pdf") {
            "/content/aeronav/online/pdf_files/legendAD.pdf"
        } else {
            String.format("/d-tpp/%s/%s", tppCycle, pdfFile.name)
        }
        fetch(path, pdfFile)
    }

    private fun getVolumeDir(cycle: String?, tppVolume: String): File {
        val dir = File(getCycleDir(cycle!!), tppVolume)
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }

    companion object {
        private const val DTPP = "dtpp"
    }
}