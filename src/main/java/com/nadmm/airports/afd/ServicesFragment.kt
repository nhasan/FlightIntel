/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2021 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.afd

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.notams.AirportNotamActivity
import com.nadmm.airports.utils.DataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ServicesFragment : FragmentBase() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.services_detail_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActionBarTitle("Services", "")

        arguments?.getString(Airports.SITE_NUMBER)?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(it)
                }
                showDetails(result)
            }
        }
    }
    private fun showDetails(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        showAirportTitle(apt)
        showAirportServices(apt)
        showFaaServices(apt)
        showFssServices(apt)
        setFragmentContentShown(true)
    }

    private fun showAirportServices( apt: Cursor ) {
        val services = ArrayList<String>()
        val other = apt.getString(apt.getColumnIndex(Airports.OTHER_SERVICES))
        if (other.isNotEmpty()) {
            services.addAll(DataUtils.decodeServices(other))
        }
        val storage = apt.getString(apt.getColumnIndex(Airports.STORAGE_FACILITY))
        if (storage.isNotEmpty()) {
            services.addAll(DataUtils.decodeStorage(storage))
        }
        val bottOxygen = apt.getString(apt.getColumnIndex(Airports.BOTTLED_O2_AVAILABLE))
        if (bottOxygen == "Y") {
            services.add("Bottled Oxygen")
        }
        val bulkOxygen = apt.getString(apt.getColumnIndex(Airports.BULK_O2_AVAILABLE))
        if (bulkOxygen == "Y") {
            services.add("Bulk Oxygen")
        }
        if (services.isNotEmpty()) {
            val chips = findViewById<ChipGroup>(R.id.airport_services_chips)
            for (service in services) {
                val chip = Chip(activity)
                chip.text = service
                chip.setTextAppearance(R.style.TextSmall)
                chips!!.addView(chip)
            }
        } else {
            val tv = findViewById<TextView>(R.id.airport_services_label)
            tv!!.visibility = View.GONE
            val layout = findViewById<LinearLayout>(R.id.airport_services_chips)
            layout!!.visibility = View.GONE
        }
    }

    private fun showFaaServices(apt: Cursor) {
        val layout = findViewById<LinearLayout>(R.id.faa_services_layout)
        val faaRegion = apt.getString(apt.getColumnIndex(Airports.REGION_CODE))
        if (faaRegion.isNotEmpty()) {
            addRow(layout!!, "FAA region", DataUtils.decodeFaaRegion(faaRegion))
        }
        val artccId = apt.getString(apt.getColumnIndex(Airports.BOUNDARY_ARTCC_ID))
        val artccName = apt.getString(apt.getColumnIndex(Airports.BOUNDARY_ARTCC_NAME))
        addRow(layout!!, "ARTCC", "$artccId ($artccName)")
        val siteNumber = apt.getString(apt.getColumnIndex(Airports.SITE_NUMBER))
        val notamFacility = apt.getString(apt.getColumnIndex(Airports.NOTAM_FACILITY_ID))
        val intent = Intent(activity, AirportNotamActivity::class.java)
        intent.putExtra(Airports.SITE_NUMBER, siteNumber)
        addClickableRow(layout, "NOTAM facility", notamFacility, intent)
        val notamD = apt.getString(apt.getColumnIndex(Airports.NOTAM_D_AVAILABLE))
        addRow(layout, "NOTAM D available", if (notamD == "Y") "Yes" else "No")
    }

    private fun showFssServices( apt: Cursor ) {
        val layout = findViewById<LinearLayout>(R.id.fss_services_layout)
        val fssId = apt.getString(apt.getColumnIndex(Airports.FSS_ID))
        val fssName = apt.getString(apt.getColumnIndex(Airports.FSS_NAME))
        addRow(layout!!, "Flight service", "$fssId ($fssName)")
        var fssPhone = apt.getString(apt.getColumnIndex(Airports.FSS_LOCAL_PHONE))
        if (fssPhone.isEmpty()) {
            fssPhone = apt.getString(apt.getColumnIndex(Airports.FSS_TOLLFREE_PHONE))
        }
        addPhoneRow(layout, "FSS phone", fssPhone)
        val state = apt.getString(apt.getColumnIndex(Airports.ASSOC_STATE))
        if (state != "AK") {
            addPhoneRow(layout, "TIBS", "1-877-4TIBS-WX")
            addPhoneRow(layout, "Clearance delivery", "1-888-766-8287")
            val faaRegion = apt.getString(apt.getColumnIndex(Airports.REGION_CODE))
            if (faaRegion == "AEA") {
                addPhoneRow(layout, "DC SFRA & FRZ", "1-866-225-7410")
            }
            addPhoneRow(layout, "Lifeguard flights", "1-877-LIF-GRD3")
        }
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val c = getAirportDetails(siteNumber)
        return arrayOf(c)
    }

}
