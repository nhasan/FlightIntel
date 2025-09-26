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

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.nadmm.airports.utils.NetworkUtils.doHttpsGet
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.util.zip.GZIPInputStream

abstract class NoaaService(protected val name: String, protected val maxAgeMillis: Long) : Service() {
    private val serviceJob = SupervisorJob() // Use SupervisorJob for better error handling in children
    // Coroutine scope tied to Dispatchers.IO for background work
    protected val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO + CoroutineName(name))

    private var _wxCache: WxCache? = null
    protected val wxCache get() = _wxCache!!

    override fun onCreate() {
        super.onCreate()

        _wxCache = WxCache(this, name, maxAgeMillis)

        Log.d(TAG, "onCreate() called for $name service")
    }

    override fun onBind(intent: Intent): IBinder? {
        // We are not using a bound service, so return null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() called for $name service")
        // Cancel all coroutines when the service is destroyed
        serviceJob.cancel() // This will cancel all coroutines launched in serviceScope
    }

    protected fun fetchFromNoaa(path: String, query: String?, file: File, compressed: Boolean=false): Boolean {
        return fetch(AWC_HOST, path, query, file, compressed)
    }

    protected fun fetch(host: String, path: String, query: String?, file: File, compressed: Boolean=false): Boolean {
        return doHttpsGet(
            this, host, path, query, file, null, null,
            if (compressed) GZIPInputStream::class.java else null
        )
    }

    object Events {
        private val _events = MutableSharedFlow<Bundle>()
        val events = _events.asSharedFlow()

        suspend fun post(result: Bundle) {
            _events.emit(result)
        }
    }

    companion object {
        private val TAG: String = NoaaService::class.java.simpleName

        protected const val AWC_HOST: String = "aviationweather.gov"

        const val STATION_ID: String = "STATION_ID"
        const val STATION_IDS: String = "STATION_IDS"
        const val CACHE_ONLY: String = "CACHE_ONLY"
        const val FORCE_REFRESH: String = "FORCE_REFRESH"
        const val RADIUS_SM: String = "RADIUS_SM"
        const val LOCATION: String = "LOCATION"
        const val IMAGE_TYPE: String = "IMAGE_TYPE"
        const val IMAGE_CODE: String = "IMAGE_CODE"
        const val TEXT_TYPE: String = "TEXT_TYPE"
        const val TEXT_CODE: String = "TEXT_CODE"
        const val RESULT: String = "RESULT"

        const val TYPE: String = "TYPE"
        const val TYPE_TEXT: String = "TYPE_TEXT"
        const val TYPE_GRAPHIC: String = "TYPE_GRAPHIC"
        const val ACTION: String = "ACTION"
        const val ACTION_GET_METAR: String = "flightintel.intent.wx.action.GET_METAR"
        const val ACTION_CACHE_METAR: String = "flightintel.intent.wx.action.CACHE_METAR"
        const val ACTION_GET_TAF: String = "flightintel.intent.wx.action.GET_TAF"
        const val ACTION_GET_PIREP: String = "flightintel.intent.wx.action.GET_PIREP"
        const val ACTION_GET_AIRSIGMET: String = "flightintel.intent.wx.action.GET_AIRSIGMET"
        const val ACTION_GET_PROGCHART: String = "flightintel.intent.wx.action.GET_PROGCHART"
        const val ACTION_GET_ICING: String = "flightintel.intent.action.wx.GET_ICING"
        const val ACTION_GET_FA: String = "flightintel.intent.action.wx.GET_FA"
        const val ACTION_GET_FB: String = "flightintel.intent.action.wx.GET_FB"
        const val ACTION_GET_GFA: String = "flightintel.intent.action.wx.GET_GFA"
    }
}
