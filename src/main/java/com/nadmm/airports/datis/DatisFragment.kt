/*
 * FlightIntel for Pilots
 *
 * Copyright 2023 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.datis

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.utils.DataUtils
import com.nadmm.airports.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DatisFragment : FragmentBase() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.datis_detail_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = resources.getString(R.string.datis_details)
        setActionBarTitle(title)
        arguments?.let { bundle ->
            val siteNumber = bundle.getString(DatabaseManager.Airports.SITE_NUMBER)
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(siteNumber!!)
                }
                result[0]?.let { apt ->
                    showAirportTitle(apt)
                }
                fetchDatis()
            }
        }
    }

    override fun isRefreshable(): Boolean {
        return true
    }

    override fun requestDataRefresh() {
        fetchDatis()
    }

    private fun fetchDatis() {
        val icaoCode = arguments?.getString(DatabaseManager.Airports.ICAO_CODE) ?: return
        val appContext = activityBase.applicationContext
        val workRequest = DatisWorker.enqueueWork(appContext, icaoCode)
        WorkManager.getInstance(appContext).getWorkInfoByIdLiveData(workRequest.id)
            .observe(viewLifecycleOwner) { workInfo ->
                if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val datisPath = workInfo.outputData.getString(DatisWorker.DATIS_PATH)
                    val datisList = DatisParser.parse(datisPath!!)
                    showDatis(datisList)
                }
            }
    }

    private fun showDatis(datisList: ArrayList<Datis>) {
        for (datis in datisList) {
            if (datis.atisType == COMBINED) {
                findViewById<TextView>(R.id.datis_combined_label)?.visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.datis_combined_details)?.let { layout ->
                    layout.removeAllViews()
                    showDatisEntry(layout, datis)
                }
            } else if (datis.atisType == ARRIVAL) {
                findViewById<TextView>(R.id.datis_arrival_label)?.visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.datis_arrival_details)?.let { layout ->
                    layout.removeAllViews()
                    showDatisEntry(layout, datis)
                }
            } else if (datis.atisType == DEPARTURE) {
                findViewById<TextView>(R.id.datis_departure_label)?.visibility = View.VISIBLE
                findViewById<LinearLayout>(R.id.datis_departure_details)?.let { layout ->
                    layout.removeAllViews()
                    showDatisEntry(layout, datis)
                }
            }
        }
        isRefreshing = false
        setFragmentContentShown(true)
    }

    private fun showDatisEntry(layout: LinearLayout, datis: Datis) {
        val code = DataUtils.getPhoneticAlphabet(datis.atisCode).uppercase()
        val issued = TimeUtils.formatDateTime(
            activityBase, datis.issuedTimestamp.toEpochSecond() * 1000)
        addTwoLineRow(layout, "Information $code is current", "Issued at $issued")
        addRow(layout, datis.atisBody)
        layout.visibility = View.VISIBLE
        android.R.layout.simple_list_item_1
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val result = arrayOfNulls<Cursor>(9)
        val apt = getAirportDetails(siteNumber)
        result[0] = apt
        return result
    }

    companion object {
        const val COMBINED = "C"
        const val ARRIVAL = "A"
        const val DEPARTURE = "D"
    }
}