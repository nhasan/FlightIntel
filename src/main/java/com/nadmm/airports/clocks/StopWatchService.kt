/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2022 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.clocks

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.SystemClock

class StopWatchService : Service() {
    private val _delayMillis: Long = 100

    enum class State { Paused, Running, Reset }

    private val mBinder: IBinder = StopWatchBinder()
    private val mHandler = Handler()
    private var mClient: OnTickHandler? = null
    private var mStartMillis: Long = 0
    private var mLastMillis: Long = 0
    private var mLastDuration: Long = 0
    private var mState = State.Reset
    var legs: ArrayList<Long>? = null
    private val mTicker = Runnable {
        mLastMillis = SystemClock.elapsedRealtime()
        scheduleNextUpdate()
        notifyClient()
    }

    interface OnTickHandler {
        fun onTick(millis: Long)
    }

    inner class StopWatchBinder : Binder() {
        val service: StopWatchService
            get() = this@StopWatchService
    }

    override fun onCreate() {
        super.onCreate()
        legs = ArrayList()
        reset()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    fun setOnTickHandler(client: OnTickHandler?) {
        mClient = client
    }

    fun startTimimg() {
        if (mState == State.Paused) {
            mState = State.Running
            mLastDuration = elapsedTime
            mStartMillis = SystemClock.elapsedRealtime()
            mLastMillis = mStartMillis
            scheduleNextUpdate()
        }
    }

    fun stopTimimg() {
        if (mState == State.Running) {
            removeUpdate()
            mState = State.Paused
            mLastMillis = SystemClock.elapsedRealtime()
            notifyClient()
        }
    }

    fun reset() {
        mState = State.Paused
        mStartMillis = 0
        mLastMillis = 0
        mLastDuration = 0
        legs!!.clear()
    }

    fun addLeg() {
        legs!!.add(elapsedTime)
    }

    val isRunning: Boolean
        get() = mState == State.Running

    private fun scheduleNextUpdate() {
        mHandler.postDelayed(mTicker, _delayMillis)
    }

    private fun removeUpdate() {
        mHandler.removeCallbacks(mTicker)
    }

    private fun notifyClient() {
        if (mClient != null) {
            mClient!!.onTick(elapsedTime)
        }
    }

    val elapsedTime: Long
        get() = mLastMillis - mStartMillis + mLastDuration
}