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
import android.os.CountDownTimer
import android.os.IBinder

class CountDownService : Service() {
    private val _tickMillis: Long = 100

    enum class State { Paused, Running, Finished, Reset }

    private val mBinder: IBinder = CountDownBinder()
    private var mClient: OnTickHandler? = null
    private var mTimer: CountDownTimer? = null
    private var mState = State.Reset

    interface OnTickHandler {
        fun onTick(millis: Long)
    }

    inner class CountDownBinder : Binder() {
        val service: CountDownService
            get() = this@CountDownService
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    fun setOnTickHandler(client: OnTickHandler?) {
        mClient = client
    }

    fun startCountDown(millis: Long) {
        mTimer = object : CountDownTimer(millis, _tickMillis) {
            override fun onTick(millisRemain: Long) {
                notifyClient(millisRemain)
            }

            override fun onFinish() {
                mState = State.Finished
                notifyClient(0)
            }
        }
        mTimer?.start()
        mState = State.Running
    }

    fun stopCountDown() {
        if (mTimer != null) {
            mTimer!!.cancel()
            mTimer = null
        }
        mState = State.Paused
    }

    fun resetCountDown() {
        mState = State.Reset
    }

    private fun notifyClient(millisRemain: Long) {
        if (mClient != null) {
            mClient!!.onTick(millisRemain)
        }
    }

    val isRunning: Boolean
        get() = mState == State.Running
    val isFinished: Boolean
        get() = mState == State.Finished
    val isReset: Boolean
        get() = mState == State.Reset
}