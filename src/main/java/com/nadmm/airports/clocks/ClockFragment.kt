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

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.ListMenuFragment
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class ClockFragment : FragmentBase() {
    private val mHandler = Handler()
    private val mTimeFormat: DateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val mDateFormat: DateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private var mRunnable: Runnable? = null
    private var mHome: String? = null
    private var mHomeTzId: String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflate<View>(R.layout.clocks_clock_view)
        return createContentView(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mHome = activityBase.prefHomeAirport
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                doQuery()
            }
            setCursor(result?.get(0))
        }
        arguments?.getString(ListMenuFragment.SUBTITLE_TEXT)
            ?.let { supportActionBar?.subtitle = it }
        setFragmentContentShown(true)
    }

    override fun onPause() {
        super.onPause()
        if (mRunnable != null) {
            // Stop updates
            mHandler.removeCallbacks(mRunnable!!)
        }
    }

    override fun onResume() {
        super.onResume()
        activityBase.setDrawerIndicatorEnabled(false)
        if (mRunnable != null) {
            // Restart updates
            updateTime()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateTime() {
        val now = Date()
        val utcTz = TimeZone.getTimeZone("GMT")
        mTimeFormat.timeZone = utcTz
        mDateFormat.timeZone = utcTz
        var tv = findViewById<TextView>(R.id.utc_time_value)
        tv!!.text = mTimeFormat.format(now) + " UTC"
        tv = findViewById(R.id.utc_date_value)
        tv!!.text = mDateFormat.format(now)
        val localTz = TimeZone.getDefault()
        mTimeFormat.timeZone = localTz
        mDateFormat.timeZone = localTz
        tv = findViewById(R.id.local_time_value)
        tv!!.text = (mTimeFormat.format(now)
                + " " + localTz.getDisplayName(localTz.inDaylightTime(now), TimeZone.SHORT))
        tv = findViewById(R.id.local_date_value)
        tv!!.text = mDateFormat.format(now)
        if (mHomeTzId != null && mHomeTzId!!.isNotEmpty()) {
            val homeTz = TimeZone.getTimeZone(mHomeTzId)
            mTimeFormat.timeZone = homeTz
            mDateFormat.timeZone = homeTz
            tv = findViewById(R.id.home_time_value)
            tv!!.text = (mTimeFormat.format(now)
                    + " " + homeTz.getDisplayName(homeTz.inDaylightTime(now), TimeZone.SHORT))
            tv = findViewById(R.id.home_date_value)
            tv!!.text = mDateFormat.format(now)
        }
        scheduleUpdate()
    }

    private fun scheduleUpdate() {
        if (mRunnable == null) {
            mRunnable = Runnable { updateTime() }
        }
        // Reschedule at the next second boundary
        val now = Date().time
        val delay = 1000 - now % 1000
        mHandler.postDelayed(mRunnable!!, delay)
    }

    private fun doQuery(): Array<Cursor?>? {
        if (mHome != null) {
            val db = getDatabase(DatabaseManager.DB_FADDS)
            val builder = SQLiteQueryBuilder()
            builder.tables = Airports.TABLE_NAME
            val c = builder.query(
                db,
                arrayOf(
                    Airports.SITE_NUMBER,
                    Airports.TIMEZONE_ID
                ),
                Airports.FAA_CODE + "=? OR " + Airports.ICAO_CODE + "=?",
                arrayOf(mHome!!, mHome!!),
                null,
                null,
                null,
                null
            )
            return arrayOf(c)
        }
        return null
    }

    private fun setCursor(c: Cursor?) {
        if (c != null && c.moveToFirst()) {
            mHomeTzId = c.getString(c.getColumnIndex(Airports.TIMEZONE_ID))
        }
        if (mHomeTzId.isNullOrBlank()) {
            var v = findViewById<View>(R.id.home_time_label)
            v!!.visibility = View.GONE
            v = findViewById(R.id.home_time_layout)
            v!!.visibility = View.GONE
        }
        updateTime()
    }

}