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
import java.io.File

class DafdService : AeroNavService("dafd") {
    override fun onHandleIntent(intent: Intent?) {
        val action = intent!!.action
        if (action == ACTION_GET_AFD) {
            getAfd(intent)
        } else if (action == ACTION_CHECK_AFD) {
            checkAfd(intent)
        }
    }

    private fun getAfd(intent: Intent?) {
        intent ?: return
        val afdCycle = intent.getStringExtra(CYCLE_NAME) ?: return
        val pdfName = intent.getStringExtra(PDF_NAME) ?: return
        val cycleDir = getCycleDir(afdCycle)
        val pdfFile = File(cycleDir, pdfName)
        if (!pdfFile.exists()) {
            val path = String.format("/afd/%s/%s", afdCycle, pdfName)
            fetch(path, pdfFile)
        }
        sendResult(ACTION_GET_AFD, afdCycle, pdfFile)
    }

    private fun checkAfd(intent: Intent?) {
        intent ?: return
        val afdCycle = intent.getStringExtra(CYCLE_NAME) ?: return
        val pdfName = intent.getStringExtra(PDF_NAME) ?: return
        val cycleDir = getCycleDir(afdCycle)
        val pdfFile = File(cycleDir, pdfName)
        sendResult(ACTION_CHECK_AFD, afdCycle, pdfFile)
    }
}
