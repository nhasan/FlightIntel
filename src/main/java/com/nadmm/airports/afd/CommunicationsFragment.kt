/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2023 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager.Aff3
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.AtcPhones
import com.nadmm.airports.data.DatabaseManager.DB_FADDS
import com.nadmm.airports.data.DatabaseManager.Tower1
import com.nadmm.airports.data.DatabaseManager.Tower2
import com.nadmm.airports.data.DatabaseManager.Tower3
import com.nadmm.airports.data.DatabaseManager.Tower4
import com.nadmm.airports.data.DatabaseManager.Tower6
import com.nadmm.airports.data.DatabaseManager.Tower7
import com.nadmm.airports.data.DatabaseManager.Tower9
import com.nadmm.airports.utils.DataUtils
import com.nadmm.airports.utils.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TreeMap

class CommunicationsFragment : FragmentBase() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.comm_detail_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setActionBarTitle("Communications", "")

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
        showAirportFrequencies(result)
        showAtcHours(result)
        showAtcPhones(result)
        showAirportPhones(apt)
        showFssServices(apt)
        showRemarks(result)
        setFragmentContentShown(true)
    }

    private fun showAirportFrequencies(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        val layout = findViewById<LinearLayout>(R.id.airport_comm_details) ?: return
        var towerRadioCall = ""
        var apchRadioCall = ""
        var depRadioCall = ""
        val map: MutableMap<String, ArrayList<Pair<String, String>>> = TreeMap()
        val ctaf = apt.getString(apt.getColumnIndexOrThrow(Airports.CTAF_FREQ))
        if (ctaf.isNotEmpty()) {
            addFrequencyToMap(map, "CTAF", ctaf, "")
        }
        val unicom = apt.getString(apt.getColumnIndexOrThrow(Airports.UNICOM_FREQS))
        if (unicom.isNotEmpty()) {
            addFrequencyToMap(map, "Unicom", unicom, "")
        }
        val twr1 = result[1]
        if (twr1 != null && twr1.moveToFirst()) {
            towerRadioCall = twr1.getString(twr1.getColumnIndexOrThrow(Tower1.RADIO_CALL_TOWER))
            apchRadioCall = try {
                twr1.getString(twr1.getColumnIndexOrThrow(Tower1.RADIO_CALL_APCH_PRIMARY))
            } catch (_: Exception) {
                twr1.getString(twr1.getColumnIndexOrThrow(Tower1.RADIO_CALL_APCH))
            }
            depRadioCall = try {
                twr1.getString(twr1.getColumnIndexOrThrow(Tower1.RADIO_CALL_DEP_PRIMARY))
            } catch (_: Exception) {
                twr1.getString(twr1.getColumnIndexOrThrow(Tower1.RADIO_CALL_DEP))
            }
        }
        val twr3 = result[2]
        if (twr3 != null && twr3.moveToFirst()) {
            do {
                val freqUse = twr3.getString(twr3.getColumnIndexOrThrow(Tower3.MASTER_AIRPORT_FREQ_USE))
                val freq = twr3.getString(twr3.getColumnIndexOrThrow(Tower3.MASTER_AIRPORT_FREQ))
                processFrequency(map, towerRadioCall, apchRadioCall, depRadioCall, freqUse, freq)
            } while (twr3.moveToNext())
        }
        val twr7 = result[4]
        if (twr7 != null && twr7.moveToFirst()) {
            do {
                val freqUse = twr7.getString(twr7.getColumnIndexOrThrow(Tower7.SATELLITE_AIRPORT_FREQ_USE))
                val freq = twr7.getString(twr7.getColumnIndexOrThrow(Tower7.SATELLITE_AIRPORT_FREQ))
                processFrequency(map, towerRadioCall, apchRadioCall, depRadioCall, freqUse, freq)
            } while (twr7.moveToNext())
        }
        val aff3 = result[5]
        if (aff3 != null && aff3.moveToFirst()) {
            do {
                val artcc = aff3.getString(aff3.getColumnIndexOrThrow(Aff3.ARTCC_ID))
                val freq = aff3.getString(aff3.getColumnIndexOrThrow(Aff3.SITE_FREQUENCY))
                val alt = aff3.getString(aff3.getColumnIndexOrThrow(Aff3.FREQ_ALTITUDE))
                var extra = "($alt altitude)"
                val type = aff3.getString(aff3.getColumnIndexOrThrow(Aff3.FACILITY_TYPE))
                if (type != "ARTCC") {
                    extra = (aff3.getString(aff3.getColumnIndexOrThrow(Aff3.SITE_LOCATION))
                            + " " + type + " " + extra)
                }
                addFrequencyToMap(map, DataUtils.decodeArtcc(artcc), freq, extra)
            } while (aff3.moveToNext())
        }
        if (map.isNotEmpty()) {
            val tv = findViewById<TextView>(R.id.airport_comm_label)
            tv!!.visibility = View.VISIBLE
            layout.visibility = View.VISIBLE
            var lastKey: String? = null
            for (key in map.keys) {
                for (pair in map[key]!!) {
                    if (key != lastKey) {
                        addRow(layout, key, pair.first, pair.second)
                        lastKey = key
                    } else {
                        addRow(layout, "", pair.first, pair.second)
                    }
                }
            }
        }
    }

    private fun processFrequency(map: MutableMap<String, ArrayList<Pair<String, String>>>,
                                 towerRadioCall: String, apchRadioCall: String,
                                 depRadioCall: String, freqUse: String, freqInfo: String) {
        var freq = freqInfo
        var found = false
        var extra = ""
        var i = freq.indexOf(';')
        if (i >= 0 && i < freq.length) {
            extra = freq.substring(i + 1)
            freq = freq.substring(0, i)
        }
        i = 0
        while (i < freq.length) {
            val c = freq[i]
            if (c in '0'..'9' || c == '.') {
                ++i
                continue
            }
            freq = freq.substring(0, i)
            break
        }
        if (freqUse.contains("LCL") || freqUse.contains("LC/P")) {
            addFrequencyToMap(map, "$towerRadioCall Tower", freq, extra)
            found = true
        }
        if (freqUse.contains("GND")) {
            addFrequencyToMap(map, "$towerRadioCall Ground", freq, extra)
            found = true
        }
        if (freqUse.contains("APCH") || freqUse.contains("ARRIVAL")) {
            addFrequencyToMap(map, "$apchRadioCall Approach", freq, extra)
            found = true
        }
        if (freqUse.contains("DEP")) {
            addFrequencyToMap(map, "$depRadioCall Departure", freq, extra)
            found = true
        }
        if (freqUse.contains("CD") || freqUse.contains("CLNC")) {
            addFrequencyToMap(map, "Clearance Delivery", freq, extra)
            found = true
        }
        if (freqUse.contains("CLASS B")) {
            addFrequencyToMap(map, "Class B", freq, extra)
            found = true
        }
        if (freqUse.contains("CLASS C")) {
            addFrequencyToMap(map, "Class C", freq, extra)
            found = true
        }
        if (freqUse.contains("ATIS")) {
            if (freqUse.contains("D-ATIS")) {
                addFrequencyToMap(map, "D-ATIS", freq, extra)
            } else {
                addFrequencyToMap(map, "ATIS", freq, extra)
            }
            found = true
        }
        if (freqUse.contains("RADAR") || freqUse.contains("RDR")) {
            addFrequencyToMap(map, "Radar", freq, extra)
            found = true
        }
        if (freqUse.contains("TRSA")) {
            addFrequencyToMap(map, "TRSA", freq, extra)
            found = true
        }
        if (freqUse.contains("TAXI CLNC")) {
            addFrequencyToMap(map, "Taxi Clearance", freq, extra)
            found = true
        }
        if (freqUse.contains("EMERG")) {
            addFrequencyToMap(map, "Emergency", freq, extra)
            found = true
        }
        if (freqUse.contains("VFR-ADV")) {
            addFrequencyToMap(map, "VFR Advisory", freq, extra)
            found = true
        }
        if (freqUse.contains("VFR ADZY")) {
            addFrequencyToMap(map, "VFR Advisory", freq, extra)
            found = true
        }
        if (freqUse.contains("SFA")) {
            addFrequencyToMap(map, "Single Freq Approach", freq, extra)
            found = true
        }
        if (freqUse.contains("PTC")) {
            addFrequencyToMap(map, "PTC", freq, extra)
            found = true
        }
        if (freqUse.contains("PAR")) {
            addFrequencyToMap(map, "PAR", freq, extra)
            found = true
        }
        if (freqUse.contains("AFIS")) {
            addFrequencyToMap(map, "AFIS", freq, extra)
            found = true
        }
        if (freqUse.contains("PMSV")) {
            addFrequencyToMap(map, "Operations", freq, extra)
            found = true
        }
        if (freqUse.contains("IC")) {
            addFrequencyToMap(map, "IC", freq, extra)
            found = true
        }
        if (!found) {
            // Not able to recognize any token so show it as is
            addFrequencyToMap(map, freqUse, freq, extra)
        }
    }

    private fun showAtcHours(result: Array<Cursor?>) {
        val tv = findViewById<TextView>(R.id.atc_hours_label) ?: return
        val layout = findViewById<LinearLayout>(R.id.atc_hours_details) ?: return
        val hoursMap = TreeMap<String, String>()
        val twr2 = result[13]
        if ((twr2 != null) && twr2.moveToFirst()) {
            do {
                val primaryAppHours = twr2.getString(
                        twr2.getColumnIndexOrThrow(Tower2.PRIMARY_APPROACH_HOURS))
                if (primaryAppHours.isNotEmpty()) {
                    hoursMap["Primary approach"] = primaryAppHours
                }
                val secondaryAppHours = twr2.getString(
                        twr2.getColumnIndexOrThrow(Tower2.SECONDARY_APPROACH_HOURS))
                if (secondaryAppHours.isNotEmpty()) {
                    hoursMap["Secondary approach"] = secondaryAppHours
                }
                val primaryDepHours = twr2.getString(
                        twr2.getColumnIndexOrThrow(Tower2.PRIMARY_DEPARTURE_HOURS))
                if (primaryDepHours.isNotEmpty()) {
                    hoursMap["Primary departure"] = primaryDepHours
                }
                val secondaryDepHours = twr2.getString(
                        twr2.getColumnIndexOrThrow(Tower2.SECONDARY_DEPARTURE_HOURS))
                if (secondaryDepHours.isNotEmpty()) {
                    hoursMap["Secondary departure"] = secondaryDepHours
                }
                val towerHours = twr2.getString(
                        twr2.getColumnIndexOrThrow(Tower2.CONTROL_TOWER_HOURS))
                if (towerHours.isNotEmpty()) {
                    hoursMap["Control tower"] = towerHours
                }
            } while (twr2.moveToNext())
        }
        val twr9 = result[12]
        if ((twr9 != null) && twr9.moveToFirst()) {
            val atisHours = twr9.getString(twr9.getColumnIndexOrThrow(Tower9.ATIS_HOURS))
            if (atisHours != null && atisHours.isNotEmpty()) {
                hoursMap["ATIS"] = atisHours
            }
        }
        if (hoursMap.isNotEmpty()) {
            tv.visibility = View.VISIBLE
            layout.visibility = View.VISIBLE
            for (key in hoursMap.keys) {
                addRow(layout, key, formatHours(hoursMap[key]))
            }
        }
    }

    private fun showAtcPhones(result: Array<Cursor?>) {
        val layout = findViewById<LinearLayout>(R.id.atc_phones_details) ?: return
        val main = result[6]
        if ((main != null) && main.moveToFirst()) {
            val phone = main.getString(main.getColumnIndexOrThrow(AtcPhones.DUTY_OFFICE_PHONE))
            addPhoneRow(layout, "Command center", phone)
        }
        val region = result[7]
        if ((region != null) && region.moveToFirst()) {
            val facility = region.getString(region.getColumnIndexOrThrow(AtcPhones.FACILITY_ID))
            val phone = region.getString(region.getColumnIndexOrThrow(AtcPhones.DUTY_OFFICE_PHONE))
            addPhoneRow(layout, DataUtils.decodeFaaRegion(facility) + " region", phone)
        }
        val artcc = result[8]
        if ((artcc != null) && artcc.moveToFirst()) {
            val facility = artcc.getString(artcc.getColumnIndexOrThrow(AtcPhones.FACILITY_ID))
            var phone = artcc.getString(artcc.getColumnIndexOrThrow(AtcPhones.DUTY_OFFICE_PHONE))
            addPhoneRow(layout, DataUtils.decodeArtcc(facility), phone!!)
            phone = artcc.getString(artcc.getColumnIndexOrThrow(AtcPhones.BUSINESS_PHONE))
            if (phone != null && phone.isNotEmpty()) {
                addPhoneRow(layout, "", phone)
            }
        }
        var tracon = result[9]
        if ((tracon != null) && tracon.moveToFirst()) {
            val faaCode = tracon.getString(tracon.getColumnIndexOrThrow(AtcPhones.FACILITY_ID))
            val type = tracon.getString(tracon.getColumnIndexOrThrow(AtcPhones.FACILITY_TYPE))
            val name = DataUtils.getAtcFacilityName("$faaCode:$type")
            var phone = tracon.getString(tracon.getColumnIndexOrThrow(AtcPhones.DUTY_OFFICE_PHONE))
            addPhoneRow(layout, "$name TRACON", phone!!)
            phone = tracon.getString(tracon.getColumnIndexOrThrow(AtcPhones.BUSINESS_PHONE))
            if (phone != null && phone.isNotEmpty()) {
                addPhoneRow(layout, "", phone)
            }
        }
        tracon = result[10]
        if ((tracon != null) && tracon.moveToFirst()) {
            val faaCode = tracon.getString(tracon.getColumnIndexOrThrow(AtcPhones.FACILITY_ID))
            val type = tracon.getString(tracon.getColumnIndexOrThrow(AtcPhones.FACILITY_TYPE))
            val name = DataUtils.getAtcFacilityName("$faaCode:$type")
            var phone = tracon.getString(tracon.getColumnIndexOrThrow(AtcPhones.DUTY_OFFICE_PHONE))
            addPhoneRow(layout, "$name TRACON", phone!!)
            phone = tracon.getString(tracon.getColumnIndexOrThrow(AtcPhones.BUSINESS_PHONE))
            if (phone != null && phone.isNotEmpty()) {
                addPhoneRow(layout, "", phone)
            }
        }
        val atct = result[11]
        if ((atct != null) && atct.moveToFirst()) {
            val tower1 = result[1]
            val name = tower1!!.getString(tower1.getColumnIndexOrThrow(Tower1.RADIO_CALL_TOWER))
            var phone = atct.getString(atct.getColumnIndexOrThrow(AtcPhones.DUTY_OFFICE_PHONE))
            addPhoneRow(layout, "$name Tower", phone!!)
            phone = atct.getString(atct.getColumnIndexOrThrow(AtcPhones.BUSINESS_PHONE))
            if (phone != null && phone.isNotEmpty()) {
                addPhoneRow(layout, "", phone)
            }
        }
        val twr9 = result[12]
        if ((twr9 != null) && twr9.moveToFirst()) {
            do {
                val atisPhone = twr9.getString(twr9.getColumnIndexOrThrow(Tower9.ATIS_PHONE))
                if (atisPhone.isNotEmpty()) {
                    addPhoneRow(layout, "ATIS", atisPhone)
                }
            } while (twr9.moveToNext())
        }
    }

    private fun showAirportPhones(apt: Cursor)
    {
        val layout = findViewById<LinearLayout>(R.id.apt_phones_details) ?: return
        val owner = apt.getString(apt.getColumnIndexOrThrow(Airports.OWNER_PHONE))
        val manager = apt.getString(apt.getColumnIndexOrThrow(Airports.MANAGER_PHONE))
        if (manager.isNotBlank())
            addPhoneRow(layout, "Manager", manager)
        if (owner.isNotBlank() && manager != owner)
            addPhoneRow(layout, "Owner", owner)
    }

    private fun showFssServices(apt: Cursor) {
        val layout = findViewById<LinearLayout>(R.id.fss_services_layout) ?: return
        val fssId = apt.getString(apt.getColumnIndexOrThrow(Airports.FSS_ID))
        val fssName = apt.getString(apt.getColumnIndexOrThrow(Airports.FSS_NAME))
        addRow(layout, "Flight service", "$fssId ($fssName)")
        var fssPhone = apt.getString(apt.getColumnIndexOrThrow(Airports.FSS_LOCAL_PHONE))
        if (fssPhone.isEmpty()) {
            fssPhone = apt.getString(apt.getColumnIndexOrThrow(Airports.FSS_TOLLFREE_PHONE))
        }
        addPhoneRow(layout, "FSS phone", fssPhone)
        val state = apt.getString(apt.getColumnIndexOrThrow(Airports.ASSOC_STATE))
        if (state != "AK") {
            addPhoneRow(layout, "TIBS", "1-877-4TIBS-WX")
            addPhoneRow(layout, "Clearance delivery", "1-888-766-8287")
            val faaRegion = apt.getString(apt.getColumnIndexOrThrow(Airports.REGION_CODE))
            if (faaRegion == "AEA") {
                addPhoneRow(layout, "DC SFRA & FRZ", "1-866-225-7410")
            }
            addPhoneRow(layout, "Lifeguard flights", "1-877-LIF-GRD3")
        }
    }

    private fun showRemarks(result: Array<Cursor?>) {
        val layout = findViewById<LinearLayout>(R.id.comm_remarks_layout) ?: return
        val twr6 = result[3]
        if (twr6 != null && twr6.moveToFirst()) {
            do {
                val remark = twr6.getString(twr6.getColumnIndexOrThrow(Tower6.REMARK_TEXT))
                addBulletedRow(layout, remark)
            } while (twr6.moveToNext())
        }
        val twr9 = result[12]
        if (twr9 != null && twr9.moveToFirst()) {
            do {
                val remark = twr9.getString(twr9.getColumnIndexOrThrow(Tower9.ATIS_PURPOSE))
                if (remark.isNotEmpty()) {
                    addBulletedRow(layout, remark)
                }
            } while (twr9.moveToNext())
        }
        val twr4 = result[14]
        if (twr4 != null && twr4.moveToFirst()) {
            var services = ""
            do {
                if (services.isNotEmpty()) {
                    services = "$services, "
                }
                services += twr4.getString(
                        twr4.getColumnIndexOrThrow(Tower4.MASTER_AIRPORT_SERVICES)).trim { it <= ' ' }
            } while (twr4.moveToNext())
            addBulletedRow(layout, "Services to satellite airports: $services")
        }
        addBulletedRow(layout, "Facilities can be contacted by phone through the"
                + " regional duty officer during non-business hours.")
    }

    private fun formatHours(hours: String?): String {
        return if (hours == "24") "24 Hr" else hours!!
    }

    private fun addFrequencyToMap(map: MutableMap<String, ArrayList<Pair<String, String>>>,
                                  key: String, freq: String, extra: String) {
        var list = map[key]
        if (list == null) {
            list = ArrayList()
        }
        list.add(Pair.create(FormatUtils.formatFreq(freq.toFloat()), extra.trim { it <= ' ' }))
        map[key] = list
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val result = arrayOfNulls<Cursor>(15)
        val apt = getAirportDetails(siteNumber)
        result[0] = apt
        val db = getDatabase(DB_FADDS)
        val builder = SQLiteQueryBuilder()
        builder.tables = Tower1.TABLE_NAME
        var c = builder.query(db, arrayOf("*"),
                Tower1.SITE_NUMBER + "=?", arrayOf(siteNumber), null, null, null, null)
        result[1] = c
        val faaCode = apt!!.getString(apt.getColumnIndexOrThrow(Airports.FAA_CODE))
        builder.tables = Tower3.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                Tower3.FACILITY_ID + "=?", arrayOf(faaCode), null, null, Tower3.MASTER_AIRPORT_FREQ_USE, null)
        result[2] = c
        builder.tables = Tower6.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                Tower3.FACILITY_ID + "=?", arrayOf(faaCode), null, null, null, null)
        result[3] = c
        builder.tables = Tower7.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                Tower7.SATELLITE_AIRPORT_SITE_NUMBER + "=?", arrayOf(siteNumber), null, null, null, null)
        result[4] = c
        builder.tables = Aff3.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                Aff3.IFR_FACILITY_ID + "=?", arrayOf(faaCode), null, null, null, null)
        result[5] = c
        builder.tables = AtcPhones.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                "(" + AtcPhones.FACILITY_TYPE + "=? AND " + AtcPhones.FACILITY_ID + "=?)", arrayOf("MAIN", "MAIN"), null, null, null, null)
        result[6] = c
        val faaRegion = apt.getString(apt.getColumnIndexOrThrow(Airports.REGION_CODE))
        builder.tables = AtcPhones.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                "(" + AtcPhones.FACILITY_TYPE + "=? AND " + AtcPhones.FACILITY_ID + "=?)", arrayOf("REGION", faaRegion), null, null, null, null)
        result[7] = c
        val artccId = apt.getString(apt.getColumnIndexOrThrow(Airports.BOUNDARY_ARTCC_ID))
        builder.tables = AtcPhones.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                "(" + AtcPhones.FACILITY_TYPE + "=? AND " + AtcPhones.FACILITY_ID + "=?)", arrayOf("ARTCC", artccId), null, null, null, null)
        result[8] = c
        val twr1 = result[1]
        if (twr1!!.moveToFirst()) {
            var apchName = twr1.getString(twr1.getColumnIndexOrThrow(
                    Tower1.RADIO_CALL_APCH_PRIMARY))
            var apchId = DataUtils.getAtcFacilityId(apchName)
            if (apchId == null) {
                apchName = twr1.getString(twr1.getColumnIndexOrThrow(
                        Tower1.RADIO_CALL_APCH_SECONDARY))
                apchId = DataUtils.getAtcFacilityId(apchName)
            }
            if (apchId != null) {
                builder.tables = AtcPhones.TABLE_NAME
                c = builder.query(db, arrayOf("*"),
                        "(" + AtcPhones.FACILITY_TYPE + "=? AND " + AtcPhones.FACILITY_ID + "=?)", arrayOf(apchId[1], apchId[0]), null, null, null, null)
                result[9] = c
            }
            var depName = twr1.getString(twr1.getColumnIndexOrThrow(
                    Tower1.RADIO_CALL_DEP_PRIMARY))
            var depId = DataUtils.getAtcFacilityId(depName)
            if (depId == null) {
                depName = twr1.getString(twr1.getColumnIndexOrThrow(
                        Tower1.RADIO_CALL_DEP_SECONDARY))
                depId = DataUtils.getAtcFacilityId(depName)
            }
            if (depId != null) {
                builder.tables = AtcPhones.TABLE_NAME
                c = builder.query(db, arrayOf("*"),
                        "(" + AtcPhones.FACILITY_TYPE + "=? AND " + AtcPhones.FACILITY_ID + "=?)", arrayOf(depId[0], depId[1]), null, null, null, null)
                result[10] = c
            }
        }
        builder.tables = AtcPhones.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                "(" + AtcPhones.FACILITY_TYPE + "=? AND " + AtcPhones.FACILITY_ID + "=?)", arrayOf("ATCT", faaCode), null, null, null, null)
        result[11] = c
        builder.tables = Tower9.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                Tower9.FACILITY_ID + "=?", arrayOf(faaCode), null, null, Tower9.ATIS_SERIAL_NO, null)
        result[12] = c
        builder.tables = Tower2.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                Tower2.FACILITY_ID + "=?", arrayOf(faaCode), null, null, null, null)
        result[13] = c
        builder.tables = Tower4.TABLE_NAME
        c = builder.query(db, arrayOf("*"),
                Tower4.FACILITY_ID + "=?", arrayOf(faaCode), null, null, null, null)
        result[14] = c
        return result
    }
}