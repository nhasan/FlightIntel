/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2021 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.aeronav

import android.annotation.SuppressLint
import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.Application
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Dtpp
import com.nadmm.airports.data.DatabaseManager.DtppCycle
import com.nadmm.airports.utils.CursorAsyncTask
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ChartsDownloadFragment : FragmentBase() {
    private var mTppCycle: String? = null
    private var mTppVolume: String? = null
    private var mCursor: Cursor? = null
    private lateinit var mFilter: IntentFilter
    private lateinit var mReceiver: BroadcastReceiver
    private var mSelectedRow: View? = null
    private var mProgressBar: ProgressBar? = null
    private lateinit var mOnClickListener: View.OnClickListener
    private var mExpired = false
    private var mIsOk = false
    private var mStop = false
    private val mVolumeRowMap = HashMap<String?, View>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFilter = IntentFilter()
        mFilter.addAction(AeroNavService.ACTION_GET_CHARTS)
        mFilter.addAction(AeroNavService.ACTION_CHECK_CHARTS)
        mFilter.addAction(AeroNavService.ACTION_COUNT_CHARTS)
        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                intent.action?.let { action ->
                    when (action) {
                        AeroNavService.ACTION_GET_CHARTS -> onChartDownload()
                        AeroNavService.ACTION_CHECK_CHARTS -> onChartDelete()
                        AeroNavService.ACTION_COUNT_CHARTS -> onChartCount(intent)
                    }
                }
            }
        }
        mOnClickListener = View.OnClickListener { v: View ->
            if (mSelectedRow == null) {
                mStop = false
                val total = v.getTag(R.id.DTPP_CHART_TOTAL) as Int
                val avail = v.getTag(R.id.DTPP_CHART_AVAIL) as Int
                if (avail < total) {
                    if (mIsOk && !mExpired) {
                        confirmStartDownload(v)
                    } else {
                        UiUtils.showToast(activity, "Cannot start download")
                    }
                } else {
                    confirmChartDelete(v)
                }
            } else if (v === mSelectedRow) {
                confirmStopDownload()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflate<View>(R.layout.charts_download_view)
        return createContentView(v)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.IO).launch {
            val result = doQuery()
            showChartInfo(result)
        }
    }

    override fun onResume() {
        activity?.let { activity ->
            val bm = LocalBroadcastManager.getInstance(activity)
            bm.registerReceiver(mReceiver, mFilter)
        }
        super.onResume()
    }

    override fun onPause() {
        activity?.let { activity ->
            val bm = LocalBroadcastManager.getInstance(activity)
            bm.unregisterReceiver(mReceiver)
            finishOperation()
        }
        super.onPause()
    }

    private fun confirmStartDownload(v: View) {
        activity?.let { activity ->
            val total = v.getTag(R.id.DTPP_CHART_TOTAL) as Int
            val avail = v.getTag(R.id.DTPP_CHART_AVAIL) as Int
            val tppVolume = v.getTag(R.id.DTPP_VOLUME_NAME) as String
            val builder = AlertDialog.Builder(
                activity
            )
            builder.setTitle("Start Download")
            builder.setMessage(
                String.format(
                    Locale.US,
                    "Do you want to download %d charts for %s volume?",
                    total - avail, tppVolume
                )
            )
            builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                startChartDownload(v)
            }
            builder.setNegativeButton("No", null)
            builder.show()
        }
    }

    private fun startChartDownload(v: View) {
        mSelectedRow = v
        val tppVolume = v.getTag(R.id.DTPP_VOLUME_NAME) as String
        CoroutineScope(Dispatchers.IO).launch {
            val result = doQueryVolume(tppVolume)
            downloadCharts(result)
        }
    }

    private fun confirmChartDelete(v: View) {
        activity?.let { activity ->
            val avail = v.getTag(R.id.DTPP_CHART_AVAIL) as Int
            val tppVolume = v.getTag(R.id.DTPP_VOLUME_NAME) as String
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Confirm Delete")
            builder.setMessage(
                String.format(
                    Locale.US,
                    "Are you sure you want to delete all %d charts for %s volume?",
                    avail, tppVolume
                )
            )
            builder.setPositiveButton(
                android.R.string.ok
            ) { _: DialogInterface?, _: Int -> startChartDelete(v) }
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.show()
        }
    }

    private fun startChartDelete(v: View) {
        mSelectedRow = v
        val tppVolume = v.getTag(R.id.DTPP_VOLUME_NAME) as String
        CoroutineScope(Dispatchers.IO).launch {
            val result = doQueryDelete(tppVolume)
            deleteCharts(result)
        }
    }

    private fun confirmStopDownload() {
        activity?.let { activity ->
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Stop Download")
            builder.setMessage("Do you want to stop the chart download?")
            builder.setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                mStop = true
            }
            builder.setNegativeButton("No", null)
            builder.show()
        }
    }

    private fun doQuery(): Array<Cursor> {
        val db = getDatabase(DatabaseManager.DB_DTPP)
        var builder = SQLiteQueryBuilder()
        builder.tables = DtppCycle.TABLE_NAME
        val c1 = builder.query(
            db, arrayOf("*"),
            null, null, null, null, null, null
        )
        builder = SQLiteQueryBuilder()
        builder.tables = Dtpp.TABLE_NAME
        val c2 = builder.query(
            db, arrayOf(
                Dtpp.TPP_VOLUME,
                "count(DISTINCT " + Dtpp.PDF_NAME + ") AS total"
            ),
            Dtpp.USER_ACTION + "!='D'",
            null, Dtpp.TPP_VOLUME, null, null, null
        )
        return arrayOf(c1, c2)
    }

    @SuppressLint("SetTextI18n")
    private fun showChartInfo(result: Array<Cursor>) {
        val c = result[0]
        if (!c.moveToFirst()) {
            activity?.let { activity ->
                UiUtils.showToast(activity, "d-TPP database is not installed")
                activity.finish()
            }
            return
        }
        mTppCycle = c.getString(c.getColumnIndex(DtppCycle.TPP_CYCLE))
        supportActionBar?.let { actionbar ->
            actionbar.subtitle = String.format("AeroNav Cycle %s", mTppCycle)
        }
        val expiry = c.getString(c.getColumnIndex(DtppCycle.TO_DATE))
        val df = SimpleDateFormat("HHmm'Z' MM/dd/yy", Locale.US)
        df.timeZone = TimeZone.getTimeZone("UTC")
        val endDate: Date? = try {
            df.parse(expiry)
        } catch (ignored: ParseException) {
            Date()
        }

        // Determine if chart cycle has expired
        val now = Date()
        if (now.time > endDate.time) {
            mExpired = true
        }
        var tv = findViewById<TextView>(R.id.charts_cycle_expiry)
        if (mExpired) {
            tv!!.text = String.format(
                Locale.US, "This chart cycle has expired on %s",
                TimeUtils.formatDateTime(activityBase, endDate.time)
            )
        } else {
            tv!!.text = String.format(
                Locale.US, "This chart cycle expires on %s",
                TimeUtils.formatDateTime(activityBase, endDate.time)
            )
        }
        tv = findViewById(R.id.charts_download_msg)
        tv!!.text = """
            Each TPP volume is about 150-250MB in size and may take 15-30 mins to download. The Instrument Procedure charts are in PDF format and stored on the external SD card storage. These are not the sectional charts. Press 'Back' button to stop a running download.
            
            All charts for a cycle are automatically deleted at the end of that cycle
            """.trimIndent()
        val msg: String
        if (!Application.sDonationDone) {
            msg = "This function is only available after a donation"
            mIsOk = false
        } else if (mExpired) {
            msg = "Chart cycle has expired"
            mIsOk = false
        } else if (!NetworkUtils.isNetworkAvailable(activity)) {
            msg = "Not connected to the internet"
            mIsOk = false
        } else if (NetworkUtils.canDownloadData(activityBase)) {
            msg = "Connected to an unmetered network"
            mIsOk = true
        } else {
            msg = "Connected to a metered network"
            mIsOk = false
        }
        tv = findViewById(R.id.charts_download_warning)
        tv!!.text = msg
        val d = UiUtils.getDefaultTintedDrawable(
            activity,
            if (mIsOk) R.drawable.ic_check else R.drawable.ic_highlight_remove
        )
        UiUtils.setTextViewDrawable(tv, d)
        val layout = findViewById<LinearLayout>(R.id.vol_chart_details)
        c = result[1]
        if (c!!.moveToFirst()) {
            do {
                val tppVolume = c.getString(c.getColumnIndex(Dtpp.TPP_VOLUME))
                val total = c.getInt(c.getColumnIndex("total"))
                addTppVolumeRow(layout, tppVolume, total)
            } while (c.moveToNext())
        }

        // Close all cursors here
        for (cursor in result) {
            cursor!!.close()
        }
        setFragmentContentShown(true)
    }

    private fun doQueryVolume(volume: String): Cursor {
        mTppVolume = volume
        val db = getDatabase(DatabaseManager.DB_DTPP)
        val builder = SQLiteQueryBuilder()
        builder.tables = Dtpp.TABLE_NAME
        val c = builder.query(
            db, arrayOf(Dtpp.PDF_NAME),
            Dtpp.TPP_VOLUME + "=? AND " + Dtpp.USER_ACTION + "!=?", arrayOf(mTppVolume, "D"),
            Dtpp.PDF_NAME + "," + Dtpp.TPP_VOLUME,
            null, null
        )
        return c
    }

    private fun onChartDownload() {
        if (mCursor != null) {
            mProgressBar!!.progress = mCursor!!.position
            if (!mStop && mCursor!!.moveToNext()) {
                nextChart
            } else {
                getChartCount(mTppCycle, mTppVolume)
                finishOperation()
            }
        }
    }

    private fun downloadCharts(c: Cursor?) {
        mCursor = c
        mCursor!!.moveToFirst()
        mProgressBar = mSelectedRow!!.findViewById(R.id.progress)
        mProgressBar.setMax(mCursor!!.count)
        mProgressBar.setProgress(0)
        mProgressBar.setVisibility(View.VISIBLE)
        nextChart
    }

    private val nextChart: Unit
        get() {
            if (activity != null) {
                val pdfName = mCursor!!.getString(mCursor!!.getColumnIndex(Dtpp.PDF_NAME))
                val pdfNames = ArrayList<String>()
                pdfNames.add(pdfName)
                val service = Intent(activity, DtppService::class.java)
                service.action = AeroNavService.ACTION_GET_CHARTS
                service.putExtra(AeroNavService.CYCLE_NAME, mTppCycle)
                service.putExtra(AeroNavService.TPP_VOLUME, mTppVolume)
                service.putExtra(AeroNavService.PDF_NAMES, pdfNames)
                activity!!.startService(service)
            }
        }

    private fun doQueryDelete(volume: String): Cursor {
        mTppVolume = volume
        val db = getDatabase(DatabaseManager.DB_DTPP)
        val builder = SQLiteQueryBuilder()
        builder.tables = Dtpp.TABLE_NAME
        val c = builder.query(
            db, arrayOf(Dtpp.PDF_NAME),
            Dtpp.TPP_VOLUME + "=? AND " + Dtpp.USER_ACTION + "!=?", arrayOf(mTppVolume, "D"),
            Dtpp.PDF_NAME + "," + Dtpp.TPP_VOLUME,
            null, null
        )
        return c
    }

    private fun onChartDelete() {
        if (mCursor != null) {
            mProgressBar!!.progress = mCursor!!.position
            if (!mStop && mCursor!!.moveToNext()) {
                deleteNextChart()
            } else {
                getChartCount(mTppCycle, mTppVolume)
                finishOperation()
            }
        }
    }

    private fun deleteCharts(c: Cursor?) {
        mCursor = c
        mCursor!!.moveToFirst()
        mProgressBar = mSelectedRow!!.findViewById(R.id.progress)
        mProgressBar.setMax(mCursor!!.count)
        mProgressBar.setProgress(0)
        mProgressBar.setVisibility(View.VISIBLE)
        deleteNextChart()
    }

    private fun deleteNextChart() {
        if (activity != null) {
            val pdfName = mCursor!!.getString(mCursor!!.getColumnIndex(Dtpp.PDF_NAME))
            val pdfNames = ArrayList<String>()
            pdfNames.add(pdfName)
            val service = Intent(activity, DtppService::class.java)
            service.action = AeroNavService.ACTION_DELETE_CHARTS
            service.putExtra(AeroNavService.CYCLE_NAME, mTppCycle)
            service.putExtra(AeroNavService.TPP_VOLUME, mTppVolume)
            service.putExtra(AeroNavService.PDF_NAMES, pdfNames)
            activity!!.startService(service)
        }
    }

    private fun finishOperation() {
        mTppVolume = null
        mSelectedRow = null
        if (mProgressBar != null) {
            mProgressBar!!.visibility = View.GONE
            mProgressBar = null
        }
        if (mCursor != null) {
            mCursor!!.close()
            mCursor = null
        }
    }

    private fun getChartCount(tppCycle: String?, tppVolume: String?) {
        if (activity != null) {
            val service = Intent(activity, DtppService::class.java)
            service.action = AeroNavService.ACTION_COUNT_CHARTS
            service.putExtra(AeroNavService.CYCLE_NAME, tppCycle)
            service.putExtra(AeroNavService.TPP_VOLUME, tppVolume)
            activity!!.startService(service)
        }
    }

    private fun addTppVolumeRow(layout: LinearLayout?, tppVolume: String, total: Int) {
        if (layout!!.childCount > 0) {
            addSeparator(layout)
        }
        val row = inflate<RelativeLayout>(R.layout.list_item_with_progressbar)
        var tv = row.findViewById<TextView>(R.id.item_label)
        tv.text = tppVolume
        tv = row.findViewById(R.id.item_value)
        tv.text = String.format(Locale.US, "%d charts", total)
        row.setTag(R.id.DTPP_VOLUME_NAME, tppVolume)
        row.setTag(R.id.DTPP_CHART_TOTAL, total)
        showStatus(row, 0, total)
        layout.addView(
            row, RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        mVolumeRowMap[tppVolume] = row
        getChartCount(mTppCycle, tppVolume)
    }

    private fun onChartCount(intent: Intent) {
        val tppVolume = intent.getStringExtra(AeroNavService.TPP_VOLUME)
        val avail = intent.getIntExtra(AeroNavService.PDF_COUNT, 0)
        val row = mVolumeRowMap[tppVolume]
        if (row != null) {
            if (activity != null) {
                row.setOnClickListener(mOnClickListener)
                val background = UiUtils.getSelectableItemBackgroundResource(activity)
                row.setBackgroundResource(background)
                row.setTag(R.id.DTPP_CHART_AVAIL, avail)
                val total = row.getTag(R.id.DTPP_CHART_TOTAL) as Int
                showStatus(row, avail, total)
            }
        }
    }

    private fun showStatus(row: View, avail: Int, total: Int) {
        val tv = row.findViewById<TextView>(R.id.item_label)
        if (avail == total) {
            UiUtils.setTextViewDrawable(tv, R.drawable.ic_check_box)
        } else {
            UiUtils.setTextViewDrawable(tv, R.drawable.ic_check_box_outline_blank)
        }
    }
}