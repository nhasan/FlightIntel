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

import android.content.Intent
import com.nadmm.airports.utils.NetworkUtils.doHttpsGet
import com.nadmm.airports.utils.UiUtils.showToast

class TfrService : TfrServiceBase() {
    private val mParser: TfrParser = TfrParser()

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            val action = it.action
            if (action == ACTION_GET_TFR_LIST) {
                getTfrList(it)
            }
        }
    }

    private fun getTfrList(intent: Intent) {
        val force = intent.getBooleanExtra(FORCE_REFRESH, false)
        val tfrFile = getFile(TFR_CACHE_NAME)
        if (force || !tfrFile.exists()) {
            try {
                doHttpsGet(this, TFR_HOST, TFR_PATH, TFR_QUERY, tfrFile, null, null, null)
            } catch (e: Exception) {
                showToast(this, "TFR: " + e.message)
            }
        }
        val tfrList = TfrList()
        mParser.parse(tfrFile, tfrList)
        tfrList.entries.sort()
        val result = makeResultIntent(intent.action)
        result.putExtra(TFR_LIST, tfrList)
        sendResultIntent(result)
    }

    companion object {
        const val TFR_HOST = "api.flightintel.com"
        const val TFR_PATH = "/data/tfr_list.xml"
        const val TFR_QUERY = ""
        const val ACTION_GET_TFR_LIST = "flightintel.tfr.action.GET_TFR_LIST"
        const val FORCE_REFRESH = "FORCE_REFRESH"
        const val TFR_LIST = "TFR_LIST"
        private const val TFR_CACHE_NAME = "tfr.xml"
    }

}