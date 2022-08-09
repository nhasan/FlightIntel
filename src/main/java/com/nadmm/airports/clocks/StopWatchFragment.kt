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
import android.widget.LinearLayout
import android.widget.TextView
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.ListMenuFragment
import com.nadmm.airports.R
import com.nadmm.airports.clocks.StopWatchService.StopWatchBinder
import java.util.*

class StopWatchFragment : FragmentBase(), StopWatchService.OnTickHandler {
    private val _blinkDelay = 500
    private var mBtnAction: Button? = null
    private var mBtnReset: Button? = null
    private var mBtnLeg: Button? = null
    private var mTimeMinutes: TextView? = null
    private var mTimeColon: TextView? = null
    private var mTimeSeconds: TextView? = null
    private var mTimeTenths: TextView? = null
    private var mLegsLayout: LinearLayout? = null
    private var mService: StopWatchService? = null
    private val mConnection = StopWatchConnection()
    private val mHandler = Handler()
    private val mBlink = Runnable { blink() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = activityBase
        val service = Intent(activity, StopWatchService::class.java)
        activity.startService(service)
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.clocks_stopwatch_view, container, false)
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
        val service = Intent(activity, StopWatchService::class.java)
        activity.bindService(service, mConnection, 0)
    }

    override fun onPause() {
        super.onPause()
        setKeepScreenOn(false)
        requireActivity().unbindService(mConnection)
        mHandler.removeCallbacks(mBlink)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mTimeMinutes = findViewById(R.id.stopwatch_mins)
        mTimeColon = findViewById(R.id.stopwatch_colon)
        mTimeSeconds = findViewById(R.id.stopwatch_secs)
        mTimeTenths = findViewById(R.id.stopwatch_tenths)
        mBtnAction = findViewById(R.id.stopwatch_action)
        mBtnAction!!.setOnClickListener { actionPressed() }
        mBtnReset = findViewById(R.id.stopwatch_reset)
        mBtnReset!!.setOnClickListener { resetPressed() }
        mBtnLeg = findViewById(R.id.stopwatch_leg)
        mBtnLeg!!.setOnClickListener { legPressed() }
        mLegsLayout = findViewById(R.id.legs_view)

        arguments?.getString(ListMenuFragment.SUBTITLE_TEXT)
            ?.let { supportActionBar?.subtitle = it }
        setFragmentContentShown(true)
    }

    private fun actionPressed() {
        if (!mService!!.isRunning) {
            mService!!.startTimimg()
            setKeepScreenOn(true)
        } else {
            mService!!.stopTimimg()
        }
        updateUiState()
    }

    private fun resetPressed() {
        setKeepScreenOn(false)
        mService!!.reset()
        updateUiState()
    }

    private fun legPressed() {
        mService!!.addLeg()
        showLegs()
    }

    override fun onTick(millis: Long) {
        showElapsedTime()
    }

    private fun setKeepScreenOn(keepScreenOn: Boolean) {
        if (view != null) {
            view?.keepScreenOn = keepScreenOn
        }
    }

    private fun blink() {
        mHandler.postDelayed(mBlink, _blinkDelay.toLong())
        val visible =
            if (mTimeSeconds!!.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
        mTimeMinutes!!.visibility = visible
        mTimeColon!!.visibility = visible
        mTimeSeconds!!.visibility = visible
        mTimeTenths!!.visibility = visible
    }

    private fun startBlink() {
        mHandler.postDelayed(mBlink, _blinkDelay.toLong())
    }

    private fun stopBlink() {
        mTimeMinutes!!.visibility = View.VISIBLE
        mTimeColon!!.visibility = View.VISIBLE
        mTimeSeconds!!.visibility = View.VISIBLE
        mTimeTenths!!.visibility = View.VISIBLE
        mHandler.removeCallbacks(mBlink)
    }

    private fun updateUiState() {
        if (mService != null && mService!!.isRunning) {
            mBtnAction!!.setText(R.string.pause)
            mBtnReset!!.visibility = View.GONE
            mBtnLeg!!.visibility = View.VISIBLE
            stopBlink()
        } else {
            mBtnReset!!.visibility = if (mService!!.elapsedTime > 0) View.VISIBLE else View.GONE
            mBtnLeg!!.visibility = View.GONE
            if (mService!!.elapsedTime > 0) {
                mBtnAction!!.setText(R.string.resume)
                mBtnReset!!.visibility = View.VISIBLE
                startBlink()
            } else {
                mBtnAction!!.setText(R.string.start)
                mBtnReset!!.visibility = View.GONE
                stopBlink()
            }
        }
        showElapsedTime()
        showLegs()
    }

    private fun showElapsedTime() {
        val time = formatElapsedTime(mService!!.elapsedTime)
        mTimeMinutes!!.text = time.substring(0, 2)
        mTimeSeconds!!.text = time.substring(3, 5)
        mTimeTenths!!.text = time.substring(5)
    }

    private fun formatElapsedTime(millis: Long): String {
        val hrs = millis / DateUtils.HOUR_IN_MILLIS
        val mins = millis % DateUtils.HOUR_IN_MILLIS / DateUtils.MINUTE_IN_MILLIS
        val secs = millis % DateUtils.MINUTE_IN_MILLIS / DateUtils.SECOND_IN_MILLIS
        val tenths = millis % DateUtils.SECOND_IN_MILLIS / (DateUtils.SECOND_IN_MILLIS / 10)
        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d.%01d", hrs, mins, secs, tenths)
        } else {
            String.format(Locale.US, "%02d:%02d.%01d", mins, secs, tenths)
        }
    }

    private fun showLegs() {
        val legs = mService!!.legs
        if (legs != null && legs.size > 0) {
            var count = mLegsLayout!!.childCount
            while (count < legs.size) {
                val leg = legs[count]
                val prev = if (count == 0) 0 else legs[count - 1]
                addLeg(++count, leg, prev)
            }
            mLegsLayout!!.visibility = View.VISIBLE
        } else {
            mLegsLayout!!.removeAllViews()
            mLegsLayout!!.visibility = View.GONE
        }
    }

    private fun addLeg(count: Int, leg: Long, prev: Long) {
        val delta = leg - prev
        val view = inflate<View>(R.layout.leg_item_view)
        var tv = view.findViewById<TextView>(R.id.leg_label)
        tv.text = String.format(Locale.US, "Leg %d", count)
        tv = view.findViewById(R.id.leg_delta)
        tv.text = formatElapsedTime(delta)
        tv = view.findViewById(R.id.leg_total)
        tv.text = formatElapsedTime(leg)
        mLegsLayout!!.addView(view, 0)
    }

    private inner class StopWatchConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as StopWatchBinder
            mService = binder.service
            mService?.setOnTickHandler(this@StopWatchFragment)
            updateUiState()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService?.setOnTickHandler(null)
            mService = null
        }
    }
}