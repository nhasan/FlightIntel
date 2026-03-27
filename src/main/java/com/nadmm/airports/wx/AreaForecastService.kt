/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2026 Nadeem Hasan <nhasan@nadmm.com>
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
import android.os.Bundle
import android.text.format.DateUtils
import com.nadmm.airports.utils.UiUtils.showToast

class AreaForecastService : NoaaService("fa", FA_CACHE_MAX_AGE) {

    override suspend fun onHandleIntent(intent: Intent?) {
        intent?.let {
            val action = intent.action

            if (action == ACTION_GET_FA) {
                getFaText(intent)
            }
        }
    }

    private suspend fun getFaText(intent: Intent) {
        val action = intent.action
        val code = intent.getStringExtra(TEXT_CODE) ?: return
        val file = wxCache.getFile(code)
        if (!file.exists()) {
            try {
                val query = "region=$code"
                fetchFromNoaa("/api/data/areafcst", query, file)
            } catch (e: Exception) {
                showToast(this, e.message)
            }
        }

        val result = Bundle().apply {
            putString(ACTION, action)
            putString(TYPE, TYPE_TEXT)
            putString(RESULT, if (file.exists()) file.absolutePath else "")
        }
        Events.post(result)
    }

    companion object {
        private const val FA_CACHE_MAX_AGE = DateUtils.MINUTE_IN_MILLIS *30
    }
}
