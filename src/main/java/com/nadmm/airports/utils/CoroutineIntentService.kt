/*
 * FlightIntel for Pilots
 *
 * Copyright 2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File

abstract class CoroutineIntentService(protected val serviceName: String) : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val channel = Channel<Pair<Intent?, Int>>(Channel.UNLIMITED)

    protected var localDataDir: File? = null

    override fun onCreate() {
        super.onCreate()
        localDataDir = SystemUtils.getExternalDir(this, serviceName)
        scope.launch {
            for ((intent, startId) in channel) {
                onHandleIntent(intent)
                stopSelf(startId)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        channel.trySend(intent to startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        channel.close()
        super.onDestroy()
    }

    open suspend fun onHandleIntent(intent: Intent?) {}

    object Events {
        private val _events = MutableSharedFlow<Any>()
        val events = _events.asSharedFlow()

        suspend fun post(event: Any) {
            _events.emit(event)
        }
    }
}
