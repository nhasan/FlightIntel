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
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch

class ProgChartService : NoaaService("progchart", PROGCHART_CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_PROGCHART) {
                    val type = intent.getStringExtra(TYPE)
                    if (type == TYPE_GRAPHIC) {
                        getProgChartImage(intent)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun getProgChartImage(intent: Intent) {
        val action = intent.action
        val code = intent.getStringExtra(IMAGE_CODE)
        val imageName = "$code.gif"
        val imageFile = getDataFile(imageName)
        if (!imageFile.exists()) {
            try {
                val path = "/data/products/progs/$imageName"
                fetchFromNoaa(path, null, imageFile, false)
            } catch (e: Exception) {
                showToast(this, "Unable to fetch PROGCHART image: ${e.message}")
            }
        }

        // Broadcast the result
        sendImageResultIntent(action, code, imageFile)
    }
}
