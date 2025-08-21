/*
 * FlightIntel for Pilots
 *
 * Copyright 2017-2025 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.wx

import android.content.Intent
import android.text.format.DateUtils
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch
import java.io.File

class WindsAloftService : NoaaService2("fb", FB_CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_FB) {
                    val code = intent.getStringExtra(TEXT_CODE)
                    val type = intent.getStringExtra(TEXT_TYPE)
                    val file = fetchWindsAloftText(code, type)
                    sendFileResultIntent(action, code, file)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun fetchWindsAloftText(code: String?, type: String?): File {
        val filename = "F${type}_fbwind_low_${code}.txt"
        val file = getDataFile(filename)
        if (!file.exists()) {
            try {
                val path = "/data/products/fbwind/$filename"
                fetch(AWC_HOST, path, null, file, false)
            } catch (e: Exception) {
                showToast(this, "Unable to fetch FB text: ${e.message}")
            }
        }
        return file
    }

    companion object {
        private const val FB_CACHE_MAX_AGE = DateUtils.HOUR_IN_MILLIS / 2
    }
}
