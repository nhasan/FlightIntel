/*
 * FlightIntel for Pilots
 *
 * Copyright 2016 Nadeem Hasan <nhasan@nadmm.com>
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
import android.text.format.DateUtils
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.utils.ClassBUtils
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.UiUtils
import java.io.File
import java.util.*

class ClassBService : AeroNavService("classb") {
    override fun onCreate() {
        super.onCreate()
        cleanupCache(serviceDataDir!!, DateUtils.DAY_IN_MILLIS)
    }

    private fun cleanupCache(dir: File, maxAge: Long) {
        // Delete all files that are older
        val now = Date()
        val files = dir.listFiles()
        for (file in files) {
            val age = now.time - file.lastModified()
            if (age > maxAge) {
                file.delete()
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val action = intent!!.action
        if (action == ACTION_GET_CLASSB_GRAPHIC) {
            getClassBGraphic(intent)
        }
    }

    private fun getClassBGraphic(intent: Intent?) {
        val facility = intent!!.getStringExtra(Airports.FAA_CODE)
        val classBFilename = ClassBUtils.getClassBFilename(facility)
        val pdfFile = File(serviceDataDir, classBFilename)
        if (!pdfFile.exists()) {
            try {
                NetworkUtils.doHttpsGet(
                    this,
                    FAA_HOST,
                    CLASS_B_PATH + "/" + classBFilename,
                    pdfFile
                )
            } catch (e: Exception) {
                UiUtils.showToast(this, "Error: " + e.message)
            }
        }
        val extras = Bundle()
        extras.putString(Airports.FAA_CODE, facility)
        extras.putString(PDF_PATH, pdfFile.absolutePath)
        sendResult(intent.action!!, extras)
    }

    companion object {
        const val ACTION_GET_CLASSB_GRAPHIC = "flightintel.intent.action.ACTION_GET_CLASSB_GRAPHIC"
        const val PDF_PATH = "PDF_PATH"
        private const val FAA_HOST = "www.faa.gov"
        private const val CLASS_B_PATH =
            "/air_traffic/flight_info/aeronav/digital_products/vfr_class_b/media"
    }
}