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

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.ListMenuFragment
import com.nadmm.airports.R
import com.nadmm.airports.clocks.CountDownService.CountDownBinder
import java.util.*

class CountDownFragment : FragmentBase(), CountDownService.OnTickHandler {
    private val _blinkDelay = 500
    private val _countdownMode = 1
    private val _editMode = 2

    private var mBtnAction: Button? = null
    private var mBtnReset: Button? = null
    private var mBtnRestart: Button? = null
    private var mBtnMinsPlus: ImageButton? = null
    private var mBtnMinsMinus: ImageButton? = null
    private var mBtnSecsPlus: ImageButton? = null
    private var mBtnSecsMinus: ImageButton? = null
    private var mTimeMinutes: TextView? = null
    private var mTimeColon: TextView? = null
    private var mTimeSeconds: TextView? = null
    private var mTimeTenths: TextView? = null
    private var mLastMillis: Long = 0
    private var mRemainMillis: Long = 0
    private var mMode = _editMode
    private var mService: CountDownService? = null
    private val mConnection = CountDownConnection()
    private val mHandler = Handler()
    private val mBlink = Runnable { blink() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity: Activity? = activity
        val service = Intent(activity, CountDownService::class.java)
        activity!!.startService(service)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.clocks_countdown_view, container, false)
        return createContentView(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mService != null) {
            mService!!.stopSelf()
        }
    }

    override fun onResume() {
        super.onResume()
        val activity = activityBase
        activity.setDrawerIndicatorEnabled(false)
        val service = Intent(activity, CountDownService::class.java)
        activity.bindService(service, mConnection, 0)
    }

    override fun onPause() {
        super.onPause()
        setKeepScreenOn(false)
        val activity: Activity? = activity
        activity!!.unbindService(mConnection)
        stopBlink()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mTimeMinutes = findViewById(R.id.countdown_mins)
        mTimeColon = findViewById(R.id.countdown_colon)
        mTimeSeconds = findViewById(R.id.countdown_secs)
        mTimeTenths = findViewById(R.id.countdown_tenths)
        mBtnMinsPlus = findViewById(R.id.countdown_mins_plus)
        mBtnMinsPlus!!.setOnClickListener {
            mRemainMillis += DateUtils.MINUTE_IN_MILLIS
            if (mRemainMillis >= DateUtils.HOUR_IN_MILLIS) {
                mRemainMillis -= DateUtils.HOUR_IN_MILLIS
            }
            updateUiState()
        }
        mBtnMinsMinus = findViewById(R.id.countdown_mins_minus)
        mBtnMinsMinus!!.setOnClickListener {
            mRemainMillis -= DateUtils.MINUTE_IN_MILLIS
            if (mRemainMillis < 0) {
                mRemainMillis += DateUtils.HOUR_IN_MILLIS
            }
            updateUiState()
        }
        mBtnSecsPlus = findViewById(R.id.countdown_secs_plus)
        mBtnSecsPlus!!.setOnClickListener {
            if (mRemainMillis % DateUtils.MINUTE_IN_MILLIS
                / DateUtils.SECOND_IN_MILLIS < 59
            ) {
                mRemainMillis += DateUtils.SECOND_IN_MILLIS
            } else {
                mRemainMillis = (mRemainMillis / DateUtils.MINUTE_IN_MILLIS
                        * DateUtils.MINUTE_IN_MILLIS)
            }
            updateUiState()
        }
        mBtnSecsMinus = findViewById(R.id.countdown_secs_minus)
        mBtnSecsMinus!!.setOnClickListener {
            if (mRemainMillis % DateUtils.MINUTE_IN_MILLIS > 0) {
                mRemainMillis -= DateUtils.SECOND_IN_MILLIS
            } else {
                mRemainMillis = ((mRemainMillis / DateUtils.MINUTE_IN_MILLIS + 1)
                        * DateUtils.MINUTE_IN_MILLIS) - DateUtils.SECOND_IN_MILLIS
            }
            updateUiState()
        }
        mBtnAction = findViewById(R.id.countdown_action)
        mBtnAction!!.setOnClickListener { actionPressed() }
        mBtnReset = findViewById(R.id.countdown_reset)
        mBtnReset!!.setOnClickListener { resetPressed() }
        mBtnRestart = findViewById(R.id.countdown_restart)
        mBtnRestart!!.setOnClickListener { restartPressed() }

        arguments?.getString(ListMenuFragment.SUBTITLE_TEXT)
            ?.let { supportActionBar?.subtitle = it }
        setFragmentContentShown(true)
    }

    override fun onTick(millis: Long) {
        mRemainMillis = millis
        showRemainingTime()
        if (millis == 0L) {
            showRemainingTime()
            mHandler.postDelayed({ updateUiState() }, DateUtils.SECOND_IN_MILLIS)
        }
    }

    private fun actionPressed() {
        if (!mService!!.isRunning) {
            if (mService!!.isReset) {
                mLastMillis = mRemainMillis
            }
            mService!!.startCountDown(mRemainMillis)
            setKeepScreenOn(true)
        } else {
            mService!!.stopCountDown()
        }
        updateUiState()
    }

    private fun resetPressed() {
        if (mMode == _countdownMode) {
            mService!!.resetCountDown()
            mRemainMillis = mLastMillis
            setKeepScreenOn(false)
        } else {
            mRemainMillis = 0
        }
        updateUiState()
    }

    private fun restartPressed() {
        setKeepScreenOn(false)
        mRemainMillis = mLastMillis
        mService!!.startCountDown(mRemainMillis)
        updateUiState()
    }

    private fun setKeepScreenOn(keepScreenOn: Boolean) {
        if (view != null) {
            view?.keepScreenOn = keepScreenOn
        }
    }

    private fun startBlink() {
        mHandler.postDelayed(mBlink, _blinkDelay.toLong())
    }

    private fun stopBlink() {
        mHandler.removeCallbacks(mBlink)
        setTimeVisibility(View.VISIBLE)
    }

    private fun blink() {
        mHandler.postDelayed(mBlink, _blinkDelay.toLong())
        val visibility =
            if (mTimeSeconds!!.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
        setTimeVisibility(visibility)
    }

    private fun setTimeVisibility(visibility: Int) {
        mTimeMinutes!!.visibility = visibility
        mTimeColon!!.visibility = visibility
        mTimeSeconds!!.visibility = visibility
        mTimeTenths!!.visibility = visibility
    }

    private fun showRemainingTime() {
        val time = formatElapsedTime(mRemainMillis)
        mTimeMinutes!!.text = time.substring(0, 2)
        mTimeSeconds!!.text = time.substring(3, 5)
        mTimeTenths!!.text = time.substring(5)
    }

    private fun formatElapsedTime(millis: Long): String {
        val mins = millis / DateUtils.MINUTE_IN_MILLIS
        val secs = millis % DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS
        val tenths = millis % DateUtils.SECOND_IN_MILLIS / (DateUtils.SECOND_IN_MILLIS / 10)
        return String.format(Locale.US, "%02d:%02d.%01d", mins, secs, tenths)
    }

    private fun setCountdownMode() {
        mMode = _countdownMode
        mBtnMinsPlus!!.visibility = View.INVISIBLE
        mBtnMinsMinus!!.visibility = View.INVISIBLE
        mBtnSecsPlus!!.visibility = View.INVISIBLE
        mBtnSecsMinus!!.visibility = View.INVISIBLE
    }

    private fun setEditMode() {
        mMode = _editMode
        mBtnMinsPlus!!.visibility = View.VISIBLE
        mBtnMinsMinus!!.visibility = View.VISIBLE
        mBtnSecsPlus!!.visibility = View.VISIBLE
        mBtnSecsMinus!!.visibility = View.VISIBLE
    }

    private fun updateUiState() {
        showRemainingTime()
        if (mService!!.isRunning) {
            stopBlink()
            setCountdownMode()
            mBtnAction!!.setText(R.string.pause)
            mBtnAction!!.visibility = View.VISIBLE
            mBtnAction!!.isEnabled = true
            mBtnReset!!.visibility = View.GONE
            mBtnRestart!!.visibility = View.GONE
        } else if (mService!!.isReset) {
            stopBlink()
            setEditMode()
            mBtnAction!!.setText(R.string.start)
            mBtnAction!!.visibility = View.VISIBLE
            mBtnAction!!.isEnabled = mRemainMillis > 0
            mBtnReset!!.visibility = if (mRemainMillis > 0) View.VISIBLE else View.GONE
            mBtnRestart!!.visibility = View.GONE
        } else if (mService!!.isFinished) {
            startBlink()
            setCountdownMode()
            mBtnAction!!.visibility = View.GONE
            mBtnReset!!.visibility = View.VISIBLE
            mBtnRestart!!.visibility = View.VISIBLE
        } else {
            startBlink()
            setCountdownMode()
            mBtnAction!!.setText(R.string.resume)
            mBtnAction!!.visibility = View.VISIBLE
            mBtnAction!!.isEnabled = true
            mBtnReset!!.visibility = if (mRemainMillis > 0) View.VISIBLE else View.GONE
            mBtnRestart!!.visibility = View.GONE
        }
    }

    private inner class CountDownConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as CountDownBinder
            mService = binder.service
            mService?.setOnTickHandler(this@CountDownFragment)
            updateUiState()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService?.setOnTickHandler(null)
            mService = null
        }
    }
}