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
package com.nadmm.airports.aeronav

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.utils.DataUtils.decodeChartCode
import com.nadmm.airports.utils.DataUtils.decodeUserAction
import com.nadmm.airports.utils.NetworkUtils.checkNetworkAndDownload
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.utils.UiUtils.setDefaultTintedTextViewDrawable
import com.nadmm.airports.utils.UiUtils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DtppFragment : FragmentBase() {
    private val dtppRowMap = HashMap<String?, View>()
    private var pendingCharts = ArrayList<String?>()
    private var tppCycle: String? = null
    private var tppVolume: String? = null
    private var expired = false
    private lateinit var mOnClickListener: View.OnClickListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mOnClickListener = View.OnClickListener { v: View ->
            val path = v.getTag(R.id.DTPP_PDF_PATH) as String?
            if (path == null) {
                val pdfName = v.getTag(R.id.DTPP_PDF_NAME) as String
                getTppChart(pdfName)
            } else {
                startPdfViewer(path)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dtpp_detail_view, container, false)
        val btnDownload = view.findViewById<Button>(R.id.btnDownload)
        btnDownload.setOnClickListener { aptCharts }
        val btnDelete = view.findViewById<Button>(R.id.btnDelete)
        btnDelete.setOnClickListener { checkDelete() }
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(DatabaseManager.Airports.SITE_NUMBER)?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(it)
                }
                showDetails(result)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                DtppService.Events.events.collect { result ->
                    handleDtppResult(result)
                }
            }
        }
    }

    private fun showDtppSummary(result: Array<Cursor?>) {
        val topLayout = findViewById<LinearLayout>(R.id.dtpp_detail_layout)
        val cycle = result[1] ?: return
        cycle.moveToFirst()
        tppCycle = cycle.getString(cycle.getColumnIndexOrThrow(DatabaseManager.DtppCycle.TPP_CYCLE))
        val to = cycle.getString(cycle.getColumnIndexOrThrow(DatabaseManager.DtppCycle.TO_DATE))
        val item = inflate<RelativeLayout>(R.layout.grouped_detail_item)
        val tv = item.findViewById<TextView>(R.id.group_name)
        tv.visibility = View.GONE
        val layout = item.findViewById<LinearLayout>(R.id.group_details)
        addRow(layout, "Cycle", tppCycle)

        // Parse chart cycle effective dates
        val df = SimpleDateFormat("HHmm'Z' MM/dd/yy", Locale.US)
        df.timeZone = TimeZone.getTimeZone("UTC")
        var toDate: Date? = null
        try {
            toDate = df.parse(to)
        } catch (_: ParseException) {
        }
        if (toDate != null) {
            addRow(
                layout, "Valid", TimeUtils.formatDateTime(
                    activityBase,
                    toDate.time
                )
            )
            val dtpp = result[2]
            dtpp!!.moveToFirst()
            val tppVolume = dtpp.getString(0)
            addRow(layout, "Volume", tppVolume)

            // Determine if chart cycle has expired
            val now = Date()
            if (now.time > toDate.time) {
                expired = true
                addRow(layout, "WARNING: This chart cycle has expired.")
            }
        }
        topLayout!!.addView(
            item, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun showDtppCharts(result: Array<Cursor?>) {
        val topLayout = findViewById<LinearLayout>(R.id.dtpp_detail_layout)
        var index = 3
        while (index < result.size) {
            showChartGroup(topLayout, result[index])
            ++index
        }
        showOtherCharts(topLayout)

        // Check the chart availability
        val pdfNames = ArrayList(dtppRowMap.keys)
        checkTppCharts(pdfNames, false)
    }

    private fun showChartGroup(layout: LinearLayout?, c: Cursor?) {
        if (c!!.moveToFirst()) {
            val chartCode = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Dtpp.CHART_CODE))
            val item = inflate<RelativeLayout>(R.layout.grouped_detail_item)
            val tv = item.findViewById<TextView>(R.id.group_name)
            tv.text = decodeChartCode(chartCode)
            val group = item.findViewById<LinearLayout>(R.id.group_details)
            do {
                val chartName = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Dtpp.CHART_NAME))
                val pdfName = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Dtpp.PDF_NAME))
                val userAction = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Dtpp.USER_ACTION))
                val faanfd18 = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Dtpp.FAANFD18_CODE))
                addChartRow(group, chartCode, chartName, pdfName, userAction, faanfd18)
            } while (c.moveToNext())
            layout!!.addView(
                item, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showOtherCharts(layout: LinearLayout?) {
        val item = inflate<RelativeLayout>(R.layout.grouped_detail_item)
        val tv = item.findViewById<TextView>(R.id.group_name)
        val group = item.findViewById<LinearLayout>(R.id.group_details)
        tv.text = "Other"
        addChartRow(group, "", "Legends & General Information", "frntmatter.pdf", "", "")
        layout!!.addView(
            item, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun addChartRow(
        layout: LinearLayout, chartCode: String, chartName: String,
        pdfName: String, userAction: String, faanfd18: String
    ) {
        val row: View = if (userAction.isNotEmpty()) {
            addRow(layout, chartName, decodeUserAction(userAction))
        } else {
            addRow(layout, chartName, faanfd18)
        }
        if (userAction != "D") {
            row.setOnClickListener(mOnClickListener)
            row.setTag(R.id.DTPP_CHART_CODE, chartCode)
            row.setTag(R.id.DTPP_PDF_NAME, pdfName)
            showChartAvailability(row, false)
            dtppRowMap[pdfName] = row
        }
        row.setTag(R.id.DTPP_USER_ACTION, userAction)
    }

    private fun checkTppCharts(pdfNames: ArrayList<String?>, download: Boolean) {
        pendingCharts = pdfNames
        val service = makeServiceIntent(AeroNavService.ACTION_CHECK_CHARTS)
        service.putExtra(AeroNavService.DOWNLOAD_IF_MISSING, download)
        requireActivity().startService(service)
    }

    private fun getTppChart(pdfName: String) {
        pendingCharts.add(pdfName)
        val service = makeServiceIntent(AeroNavService.ACTION_GET_CHARTS)
        requireActivity().startService(service)
    }

    private fun deleteCharts() {
        for (pdfName in dtppRowMap.keys) {
            val v = dtppRowMap[pdfName]
            if (v != null) {
                val userAction = v.getTag(R.id.DTPP_USER_ACTION) as String?
                val chartCode = v.getTag(R.id.DTPP_CHART_CODE) as String?
                val path = v.getTag(R.id.DTPP_PDF_PATH) as String?
                if (chartCode != null) {
                    if (userAction != "D" && chartCode.isNotEmpty() && path != null) {
                        pendingCharts.add(pdfName)
                    }
                }
            }
        }
        val service = makeServiceIntent(AeroNavService.ACTION_DELETE_CHARTS)
        requireActivity().startService(service)
    }

    private fun makeServiceIntent(action: String): Intent {
        val service = Intent(activity, DtppService::class.java)
        service.action = action
        service.putExtra(AeroNavService.CYCLE_NAME, tppCycle)
        service.putExtra(AeroNavService.TPP_VOLUME, tppVolume)
        service.putExtra(AeroNavService.PDF_NAMES, pendingCharts)
        return service
    }// This PDF is not available on the device

    // Skip deleted and downloaded charts
    private val missingCharts: Unit
        get() {
            val pdfNames = ArrayList<String?>()
            for (pdfName in dtppRowMap.keys) {
                val v = dtppRowMap[pdfName]
                if (v != null) {
                    val userAction = v.getTag(R.id.DTPP_USER_ACTION) as String
                    val path = v.getTag(R.id.DTPP_PDF_PATH) as String?
                    // Skip deleted and downloaded charts
                    if (userAction != "D" && path == null) {
                        // This PDF is not available on the device
                        pdfNames.add(pdfName)
                    }
                }
            }
            showToast(
                requireActivity(), String.format(
                    Locale.US,
                    "Downloading %d charts in the background", pdfNames.size
                ), Toast.LENGTH_LONG
            )
            checkTppCharts(pdfNames, true)
        }

    private fun handleDtppResult(result: DtppService.Result) {
        with (result) {
            val view = dtppRowMap[pdfName] ?: return

            if (pdfPath.isNotEmpty()) {
                showChartAvailability(view, true)
                view.setTag(R.id.DTPP_PDF_PATH, pdfPath)
                if (action == AeroNavService.ACTION_GET_CHARTS) {
                    startPdfViewer(pdfPath)
                }
            } else {
                showChartAvailability(view, false)
                view.setTag(R.id.DTPP_PDF_PATH, null)
            }

            pendingCharts.remove(pdfName)
            if (pendingCharts.isEmpty()) {
                updateButtonState()
            }
        }
    }

    private fun showChartAvailability(view: View, available: Boolean) {
        val tv = view.findViewById<TextView>(R.id.item_label)
        if (available) {
            setDefaultTintedTextViewDrawable(
                tv, R.drawable.ic_outline_check_box_24
            )
        } else {
            setDefaultTintedTextViewDrawable(
                tv, R.drawable.ic_outline_check_box_outline_blank_24
            )
        }
    }

    private fun updateButtonState() {
        // Check if we have all the charts for this airport
        var all = true
        var none = true
        for (key in dtppRowMap.keys) {
            val v = dtppRowMap[key]
            if (v != null) {
                val path = v.getTag(R.id.DTPP_PDF_PATH) as String?
                val chartCode = v.getTag(R.id.DTPP_CHART_CODE) as String?
                if (chartCode != null) {
                    if (chartCode.isNotEmpty()) {
                        if (path == null) {
                            all = false
                        } else {
                            none = false
                        }
                    }
                }
            }
        }
        val btnDownload = findViewById<Button>(R.id.btnDownload)
        if (all) {
            btnDownload!!.visibility = View.GONE
        } else {
            btnDownload!!.visibility = View.VISIBLE
        }
        val btnDelete = findViewById<Button>(R.id.btnDelete)
        if (none) {
            btnDelete!!.visibility = View.GONE
        } else {
            btnDelete!!.visibility = View.VISIBLE
        }
    }

    private val aptCharts: Unit
        get() {
            checkNetworkAndDownload(requireActivity()) { missingCharts }
        }

    private fun startPdfViewer(path: String) {
        if (expired) {
            showToast(requireActivity(), "WARNING: This chart has expired!", Toast.LENGTH_LONG)
        }
        SystemUtils.startPDFViewer(activity, path)
    }

    private fun checkDelete() {
        if (activity != null) {
            val builder = AlertDialog.Builder(requireActivity())
            builder.setMessage("Delete all downloaded charts for this airport?")
                .setPositiveButton("Yes") { _: DialogInterface?, _: Int -> deleteCharts() }
                .setNegativeButton("No") { _: DialogInterface?, _: Int -> }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val result = arrayOfNulls<Cursor>(11)
        var index = 0
        val apt = getAirportDetails(siteNumber)
        result[index++] = apt
        val faaCode = apt!!.getString(apt.getColumnIndexOrThrow(DatabaseManager.Airports.FAA_CODE))
        val db = getDatabase(DatabaseManager.DB_DTPP)
        var builder = SQLiteQueryBuilder()
        builder.tables = DatabaseManager.DtppCycle.TABLE_NAME
        var c = builder.query(
            db, arrayOf("*"),
            null, null, null, null, null, null
        )
        result[index++] = c
        builder = SQLiteQueryBuilder()
        builder.tables = DatabaseManager.Dtpp.TABLE_NAME
        c = builder.query(
            db,
            arrayOf(DatabaseManager.Dtpp.TPP_VOLUME),
            DatabaseManager.Dtpp.FAA_CODE + "=?",
            arrayOf(faaCode),
            DatabaseManager.Dtpp.TPP_VOLUME,
            null,
            null,
            null
        )
        result[index++] = c
        c.moveToFirst()
        tppVolume = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Dtpp.TPP_VOLUME))
        for (chartCode in arrayOf(
            "APD", "MIN", "STAR", "IAP",
            "DP", "DPO", "LAH", "HOT"
        )) {
            builder = SQLiteQueryBuilder()
            builder.tables = DatabaseManager.Dtpp.TABLE_NAME
            c = builder.query(
                db,
                arrayOf("*"),
                DatabaseManager.Dtpp.FAA_CODE + "=? AND " + DatabaseManager.Dtpp.CHART_CODE + "=?",
                arrayOf(faaCode, chartCode),
                null,
                null,
                null,
                null
            )
            result[index++] = c
        }
        return result
    }

    private fun showDetails(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        try {
            showAirportTitle(apt)
            showDtppSummary(result)
            showDtppCharts(result)
            setFragmentContentShown(true)
        } catch (_: Exception) {
        }
    }
}