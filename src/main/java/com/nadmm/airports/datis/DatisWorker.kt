/*
 * FlightIntel for Pilots
 *
 * Copyright 2023 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.datis

import android.content.Context
import android.text.format.DateUtils
import android.widget.Toast
import androidx.work.WorkInfo
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.nadmm.airports.WorkerBase
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class DatisWorker(appContext: Context, workerParams: WorkerParameters)
    : WorkerBase(appContext, workerParams, WORKER_NAME, CACHE_MAX_AGE) {

    private val context = appContext

    override suspend fun doWork(): Result {
        return try {
            val icaoLocation = inputData.getString(ICAO_LOCATION)
            val force = inputData.getBoolean(FORCE_REFRESH, false)
            val datisFile = getDataFile("$icaoLocation.json")

            if (force || !datisFile.exists()) {
                val url = URL("https://api.flightintel.com/datis/${icaoLocation}")
                withContext(Dispatchers.IO) {
                    NetworkUtils.doHttpGet(context, url, datisFile, null, null, null)
                }
            }
            if (datisFile.exists() && datisFile.length() > 0) {
                val outputData = workDataOf(
                    ICAO_LOCATION to icaoLocation,
                    DATIS_PATH to datisFile.absolutePath
                )
                Result.success(outputData)
            } else
                throw Exception("Unable to get d-ATIS info")
        } catch (e: Exception) {
            UiUtils.showToast(context, e.message, Toast.LENGTH_LONG)
            Result.failure()
        }
    }

    companion object {
        private const val ICAO_LOCATION = "ICAO_LOCATION"
        private const val FORCE_REFRESH = "FORCE_REFRESH"
        private const val DATIS_PATH = "DATIS_PATH"
        private const val WORKER_NAME = "datis"
        private const val CACHE_MAX_AGE = 5 * DateUtils.MINUTE_IN_MILLIS

        fun getDatis(workInfo: WorkInfo): DatisList? {
            val path = workInfo.outputData.getString(DATIS_PATH) ?: return null
            return DatisParser.parse(File(path))
        }

        fun enqueueWork(appContext: Context, icaoLocation: String, force: Boolean)
                : WorkRequest {
            val workData = workDataOf(
                ICAO_LOCATION to icaoLocation,
                FORCE_REFRESH to force
            )
            return enqueueWork<DatisWorker>(appContext, workData)
        }
    }
}
