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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkRequest
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

    protected fun getDataFile(filename: String): File {
        return File(dataDir, "${workerName}_${filename}")
    }

    private fun cleanupCache() {
        val age = Date().time - maxAge
        dataDir.listFiles()
            ?.filter { file -> file.lastModified() < age }
            ?.forEach { file -> file.delete() }
    }

    companion object {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        inline fun <reified T: ListenableWorker> enqueueWork(appContext: Context, workData: Data)
                : WorkRequest {
            return OneTimeWorkRequestBuilder<T>()
                .setInputData(workData)
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()
                .also {
                    WorkManager.getInstance(appContext).enqueue(it)
                }
        }
    }

}