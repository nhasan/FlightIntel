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
import android.util.Log
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.launch

class IcingService : NoaaService("icing", CACHE_MAX_AGE) {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = intent.action

            serviceScope.launch {
                if (action == ACTION_GET_ICING) {
                    getIcingImage(intent)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun getIcingImage(intent: Intent) {
        val type = intent.getStringExtra(TYPE)
        if (type == TYPE_GRAPHIC) {
            val action = intent.action
            val imgType = intent.getStringExtra(IMAGE_TYPE)
            val code = intent.getStringExtra(IMAGE_CODE)
            val fileName = "${imgType}_${code}_sev.gif"
            Log.d(TAG, "getIcing: action=${action}, type=$type, fileName=$fileName")
            val imageFile = wxCache.getFile(fileName)
            if (!imageFile.exists()) {
                try {
                    val path = "/data/products/icing/${fileName}"
                    fetchFromNoaa(path, null, imageFile, false)
                } catch (e: Exception) {
                    showToast(this, "Unable to fetch icing image: " + e.javaClass.simpleName)
                }
            }

            // Broadcast the result
            sendImageResultIntent(action, code, imageFile)
        }
    }

    companion object {
        private val TAG = IcingService::class.java.simpleName
        private const val CACHE_MAX_AGE = 30 * DateUtils.MINUTE_IN_MILLIS
    }
}
