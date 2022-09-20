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
import com.nadmm.airports.tfr.TfrList.Tfr
import com.nadmm.airports.utils.NetworkUtils.doHttpsGet
import com.nadmm.airports.utils.UiUtils.showToast

class TfrImageService : TfrServiceBase() {
    override fun onHandleIntent(intent: Intent?) {
        val action = intent!!.action
        if (action == ACTION_GET_TFR_IMAGE) {
            getTfrImage(intent)
        }
    }

    private fun getTfrImage(intent: Intent?) {
        val tfr = intent!!.getSerializableExtra(TFR_ENTRY) as Tfr?
        var notamId = tfr!!.notamId
        val start = notamId!!.indexOf(' ')
        if (start > 0) {
            notamId = notamId.substring(start + 1)
        }
        notamId = notamId.replace("/", "_")
        val name = "sect_$notamId.gif"
        val imageFile = getFile(name)
        if (!imageFile.exists()) {
            try {
                doHttpsGet(this, TFR_HOST, "$TFR_PATH/$name", null, imageFile, null, null, null)
            } catch (e: Exception) {
                showToast(this, "TFR: " + e.message)
            }
        }
        val result = makeResultIntent(intent.action)
        if (imageFile.exists()) {
            result.putExtra(TFR_IMAGE_PATH, imageFile.absolutePath)
        }
        sendResultIntent(result)
    }

    companion object {
        //http://tfr.faa.gov/save_maps/sect_2_8597.gif
        const val TFR_HOST = "tfr.faa.gov"
        const val TFR_PATH = "/save_maps"
        const val ACTION_GET_TFR_IMAGE = "flightintel.tfr.action.GET_TFR_IMAGE"
        const val TFR_ENTRY = "TFR_ENTRY"
        const val TFR_IMAGE_PATH = "TFR_IMAGE_PATH"
    }
}