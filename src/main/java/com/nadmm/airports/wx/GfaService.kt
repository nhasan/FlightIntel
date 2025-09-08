/*
 * FlightIntel for Pilots
 *
 * Copyright 2021-2025 Nadeem Hasan <nhasan@nadmm.com>
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
import android.util.Log
import androidx.core.os.bundleOf
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch

class GfaService : NoaaService("gfa", CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action
            serviceScope.launch {
                val type = intent.getStringExtra(TYPE)
                if (action == ACTION_GET_GFA) {
                    if (type == TYPE_GRAPHIC) {
                        fetchAndSendGfaImage(intent)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun fetchAndSendGfaImage(intent: Intent) {
        // Get request parameters
        val action = intent.action
        val imgType = intent.getStringExtra(IMAGE_TYPE)
        val code = intent.getStringExtra(IMAGE_CODE)
        Log.d(TAG, "Fetching GFA image: $imgType, code: $code")

        val imageName = "${imgType}_${code}.png"
        val imageFile = wxCache.getFile(imageName)
        if (!imageFile.exists()) {
            try {
                val path = "/data/products/gfa/$imageName"
                fetchFromNoaa(path, null, imageFile, false)
            } catch (e: Exception) {
                showToast(this, "Unable to fetch gfa image: ${e.message}")
            }
        }

        val result = bundleOf(
            ACTION to action,
            TYPE to TYPE_GRAPHIC,
            IMAGE_CODE to code,
            RESULT to if (imageFile.exists()) imageFile.absolutePath else ""
        )
        Events.post(result)
    }

    companion object {
        private const val CACHE_MAX_AGE = 30 * DateUtils.MINUTE_IN_MILLIS
        private val TAG: String = GfaService::class.java.getSimpleName()
    }
}
