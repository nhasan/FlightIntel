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
import androidx.core.os.bundleOf
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch

class WindsAloftService : NoaaService("fb", FB_CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_FB) {
                    fetchWindsAloftText(intent)
                }
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun fetchWindsAloftText(intent: Intent) {
        val action = intent.action
        val code = intent.getStringExtra(TEXT_CODE)
        val type = intent.getStringExtra(TEXT_TYPE)
        val filename = "F${type}_fbwind_low_${code}.txt"
        val file = wxCache.getFile(filename)
        if (!file.exists()) {
            try {
                val path = "/data/products/fbwind/$filename"
                fetch(AWC_HOST, path, null, file, false)
            } catch (e: Exception) {
                showToast(this, "Unable to fetch FB text: ${e.message}")
            }
        }

        val result = bundleOf(
            ACTION to action,
            TYPE to TYPE_TEXT,
            RESULT to if (file.exists()) file.absolutePath else ""
        )
        Events.post(result)
    }

    companion object {
        private const val FB_CACHE_MAX_AGE = DateUtils.HOUR_IN_MILLIS / 2
    }
}
