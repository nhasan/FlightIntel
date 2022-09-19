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
package com.nadmm.airports.tfr

import android.app.IntentService
import java.io.File
import android.text.format.DateUtils
import com.nadmm.airports.utils.SystemUtils
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.*

abstract class TfrServiceBase : IntentService(SERVICE_NAME) {
    private var mDataDir: File? = null

    override fun onCreate() {
        super.onCreate()
        mDataDir = SystemUtils.getExternalDir(this, SERVICE_NAME)

        // Remove any old files from cache first
        cleanupCache(mDataDir)
    }

    private fun cleanupCache(dir: File?) {
        // Delete all files that are older
        val now = Date()
        val files = dir!!.listFiles()
        for (file in files!!) {
            val age = now.time - file.lastModified()
            if (age > 5 * DateUtils.MINUTE_IN_MILLIS) {
                file.delete()
            }
        }
    }

    protected fun getFile(name: String): File {
        return File(mDataDir, name)
    }

    protected fun makeResultIntent(action: String?): Intent {
        val intent = Intent()
        intent.action = action
        return intent
    }

    protected fun sendResultIntent(intent: Intent?) {
        val bm = LocalBroadcastManager.getInstance(this)
        bm.sendBroadcast(intent!!)
    }

    companion object {
        private const val SERVICE_NAME = "tfr"
    }
}