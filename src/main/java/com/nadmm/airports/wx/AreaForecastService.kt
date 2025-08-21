/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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

class AreaForecastService : NoaaService("fa", FA_CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_FA) {
                    intent.getStringExtra(TEXT_CODE)?.let { code ->
                        getFaText(code)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun getFaText(code: String) {
        val file = getDataFile(code)
        if (!file.exists()) {
            try {
                val path = "/data/products/fa/$code"
                fetch(AWC_HOST, path, null, file, false)
            } catch (e: Exception) {
                showToast(this, "Unable to fetch FA text: ${e.message}")
            }
        }

        // Broadcast the result
        sendFileResultIntent(ACTION_GET_FA, code, file)
    }

    companion object {
        private const val FA_CACHE_MAX_AGE = DateUtils.MINUTE_IN_MILLIS *30
    }
}
