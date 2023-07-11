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
package com.nadmm.airports

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nadmm.airports.utils.SystemUtils
import java.io.File
import java.util.Date

abstract class WorkerBase(appContext: Context, workerParams: WorkerParameters,
                          name: String, age: Long):
    CoroutineWorker(appContext, workerParams) {

    private val dataDir: File
    private val maxAge: Long
    private val workerName: String

    init {
        dataDir = SystemUtils.getExternalDir(appContext, name)
        maxAge = age
        workerName = name

        cleanupCache()
    }

    protected fun dataFile(filename: String): File {
        return File(dataDir, "${workerName}_${filename}")
    }

    private fun cleanupCache() {
        val now = Date()
        val files = dataDir.listFiles()
        if (files != null) {
            for (file in files) {
                val age = now.time - file.lastModified()
                if (age > maxAge) {
                    file.delete()
                }
            }
        }
    }
}