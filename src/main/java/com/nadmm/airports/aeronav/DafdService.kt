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
package com.nadmm.airports.aeronav

import android.content.Intent
Bundle
import android.os.IBinder
import java.io.File

class DafdService : AeroNavService("dafd") {

    override suspend fun onHandleIntent(intent: Intent?) {
        intent?.action?.let { action ->
            if (action == ACTION_GET_AFD) {
                getAfd(intent)
            } else if (action == ACTION_CHECK_AFD) {
                checkAfd(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun getAfd(intent: Intent) {
        val afdCycle = intent.getStringExtra(CYCLE_NAME) ?: return
        val pdfName = intent.getStringExtra(PDF_NAME) ?: return
        val cycleDir = getCycleDir(afdCycle)
        val pdfFile = File(cycleDir, pdfName)
        if (!pdfFile.exists()) {
            val path = String.format("/afd/%s/%s", afdCycle, pdfName)
            fetch(path, pdfFile)
        }

        Events.post(Bundle().apply {
            putString("ACTION", intent.action)
            putString("PATH", pdfFile.absolutePath)
        })
    }

    private suspend fun checkAfd(intent: Intent) {
        val afdCycle = intent.getStringExtra(CYCLE_NAME) ?: return
        val pdfName = intent.getStringExtra(PDF_NAME) ?: return
        val cycleDir = getCycleDir(afdCycle)
        val pdfFile = File(cycleDir, pdfName)

        Events.post(Bundle().apply {
            putString("ACTION", intent.action)
            putString        Events.post(Intent(intent.action).apply {
            putExtra("PATH", if (pdfFile.exists()) pdfFile.absolutePath else "")
        })
    }
}
