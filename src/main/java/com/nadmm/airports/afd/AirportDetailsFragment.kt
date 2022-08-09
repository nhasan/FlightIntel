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

package com.nadmm.airports.afd

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.PreferencesActivity
import com.nadmm.airports.R
import com.nadmm.airports.aeronav.AeroNavService
import com.nadmm.airports.aeronav.ClassBService
import com.nadmm.airports.aeronav.DafdService
import com.nadmm.airports.aeronav.DtppActivity
import com.nadmm.airports.data.DatabaseManager.*
import com.nadmm.airports.dof.NearbyObstaclesFragment
import com.nadmm.airports.notams.AirportNotamActivity
import com.nadmm.airports.tfr.TfrListActivity
import com.nadmm.airports.utils.*
import com.nadmm.airports.wx.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

class AirportDetailsFragment : FragmentBase() {

    private val mAwosViews = HashSet<LinearLayout>()
    private val mRunwayViews = HashSet<TextView>()
    private val mBcastReceiver: BroadcastReceiver
    private val mBcastFilter: IntentFilter = IntentFilter()
    private lateinit var mLocation: Location
    private var mDeclination: Float = 0.toFloat()
    private lateinit var mIcaoCode: String
    private var mFAACode: String? = null
    private var mRadius: Int = 0
    private lateinit var mHome: String
    private lateinit var mSiteNumber: String

    init {
        mBcastFilter.run {
            addAction(NoaaService.ACTION_GET_METAR)
            addAction(AeroNavService.ACTION_GET_AFD)
            addAction(ClassBService.ACTION_GET_CLASSB_GRAPHIC)
        }
        mBcastReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                handleBroadcast(intent)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.airport_detail_view, container, false)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setActionBarTitle("Airport Details", "")

        mRadius = activityBase.prefNearbyRadius
        mHome = activityBase.prefHomeAirport

        arguments?.getString(Airports.SITE_NUMBER)?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(it)
                }
                showDetails(result)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mAwosViews.clear()
    }

    override fun onResume() {
        super.onResume()

        activity?.let {
            val bm = LocalBroadcastManager.getInstance(it)
            bm.registerReceiver(mBcastReceiver, mBcastFilter)
        }
    }

    override fun onPause() {
        super.onPause()

        activity?.let {
            val bm = LocalBroadcastManager.getInstance(it)
            bm.unregisterReceiver(mBcastReceiver)
        }
    }

    override fun isRefreshable(): Boolean {
        return true
    }

    override fun requestDataRefresh() {
        requestMetars(true)
    }

    private fun handleBroadcast(intent: Intent) {
        when (intent.action) {
            MetarService.ACTION_GET_METAR -> {
                val metar = intent.getSerializableExtra(NoaaService.RESULT) as? Metar
                if (metar?.rawText != null) {
                    showWxInfo(metar)
                    if (isRefreshing) {
                        isRefreshing = false
                    }
                }
            }
            AeroNavService.ACTION_GET_AFD -> {
                val path = intent.getStringExtra(AeroNavService.PDF_PATH)
                if (path != null) {
                    SystemUtils.startPDFViewer(activity, path)
                }
            }
            ClassBService.ACTION_GET_CLASSB_GRAPHIC -> {
                val path = intent.getStringExtra(ClassBService.PDF_PATH)
                if (path != null) {
                    SystemUtils.startPDFViewer(activity, path)
                }
            }
        }
    }

    private fun getAfdPage(afdCycle: String, pdfName: String) {
        val service = Intent(activity, DafdService::class.java)
        service.action = AeroNavService.ACTION_GET_AFD
        service.putExtra(AeroNavService.CYCLE_NAME, afdCycle)
        service.putExtra(AeroNavService.PDF_NAME, pdfName)
        activityBase.startService(service)
    }

    private fun getClassBGraphic(faaCode: String) {
        val service = Intent(activity, ClassBService::class.java)
        service.action = ClassBService.ACTION_GET_CLASSB_GRAPHIC
        service.putExtra(Airports.FAA_CODE, faaCode)
        activityBase.startService(service)
    }

    private fun showDetails(result: Array<Cursor?>) {
        val apt = result[0] ?: return

        showAirportTitle(apt)

        showCommunicationsDetails(result)
        showRunwayDetails(result)
        showRemarks(result)
        showAwosDetails(result)
        showHomeDistance(result)
        showNearbyFacilities()
        showNotamAndTfr()
        showCharts(result)
        showOperationsDetails(result)
        showAeroNavDetails(result)
        showServicesDetails(result)
        showOtherDetails()

        requestMetars(false)

        result.forEach { it?.close() }

        setFragmentContentShown(true)
    }

    private fun showCommunicationsDetails(result: Array<Cursor?>) {
        val apt = result[0] ?: return

        val layout = findViewById<LinearLayout>(R.id.detail_comm_layout) ?: return

        val ctaf = apt.getString(apt.getColumnIndexOrThrow(Airports.CTAF_FREQ))
        addRow(layout, "CTAF", ctaf.ifEmpty { "None" })
        val unicom = apt.getString(apt.getColumnIndexOrThrow(Airports.UNICOM_FREQS))
        if (unicom.isNotEmpty()) {
            addRow(layout, "Unicom", unicom)
        }

        val twr3 = result[4]
        if (twr3 != null && twr3.moveToFirst()) {
            val freqMap = HashMap<String, ArrayList<Float>>()
            do {
                val freqUse = twr3.getString(
                        twr3.getColumnIndexOrThrow(Tower3.MASTER_AIRPORT_FREQ_USE))
                val value = twr3.getString(
                        twr3.getColumnIndexOrThrow(Tower3.MASTER_AIRPORT_FREQ))
                when {
                    freqUse.contains("LCL/P") -> addFrequencyToMap(freqMap, "Tower", value)
                    freqUse.contains("GND/P") -> addFrequencyToMap(freqMap, "Ground", value)
                    freqUse.contains("ATIS") -> addFrequencyToMap(freqMap, "ATIS", value)
                }
            } while (twr3.moveToNext())

            for (key in freqMap.keys) {
                val freqs = freqMap[key]
                // Do not show here if multiple frequencies are listed
                if (freqs?.size == 1) {
                    addRow(layout, key, FormatUtils.formatFreq(freqs[0]))
                }
            }
        }

        addClickableRow(layout, "More...", CommunicationsFragment::class.java, arguments)
    }

    private fun addFrequencyToMap(freqMap: HashMap<String, ArrayList<Float>>,
                                  key: String, value: String) {
        val freqs = freqMap[key] ?: ArrayList()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c in '0'..'9' || c == '.') {
                ++i
                continue
            }
            break
        }
        val freq = value.substring(0, i).toFloat()
        if (freq <= 136 && !freqs.contains(freq)) {
            // Add VHF frequencies only
            freqs.add(freq)
        }
        freqMap[key] = freqs
    }

    private fun showRunwayDetails(result: Array<Cursor?>) {
        val rwyLayout = findViewById<LinearLayout>(R.id.detail_rwy_layout) ?: return
        val heliLayout = findViewById<LinearLayout>(R.id.detail_heli_layout) ?: return
        var rwyNum = 0
        var heliNum = 0

        val rwy = result[1]
        if (rwy != null && rwy.moveToFirst()) {
            do {
                val rwyId = rwy.getString(rwy.getColumnIndexOrThrow(Runways.RUNWAY_ID))
                if (rwyId.startsWith("H")) {
                    // This is a helipad
                    addRunwayRow(heliLayout, rwy)
                    ++heliNum
                } else {
                    // This is a runway
                    addRunwayRow(rwyLayout, rwy)
                    ++rwyNum
                }
            } while (rwy.moveToNext())
        }

        if (rwyNum == 0) {
            // No runways so remove the section
            val tv: TextView? = findViewById(R.id.detail_rwy_label)
            tv?.visibility = View.GONE
            rwyLayout.visibility = View.GONE
        }
        if (heliNum == 0) {
            // No helipads so remove the section
            val tv: TextView? = findViewById(R.id.detail_heli_label)
            tv?.visibility = View.GONE
            heliLayout.visibility = View.GONE
        }
    }

    private fun showRemarks(result: Array<Cursor?>) {
        var row = 0
        val label = findViewById<TextView>(R.id.detail_remarks_label)
        val layout = findViewById<LinearLayout>(R.id.detail_remarks_layout)
        val rmk = result[2]
        if (rmk?.moveToFirst() == true) {
            do {
                val remark = rmk.getString(rmk.getColumnIndexOrThrow(Remarks.REMARK_TEXT))
                addBulletedRow(layout!!, remark)
                ++row
            } while (rmk.moveToNext())
        }

        val twr1 = result[3]
        val twr7 = result[5]
        if (twr1 != null && twr1.moveToFirst()) {
            val facilityType = twr1.getString(twr1.getColumnIndexOrThrow(Tower1.FACILITY_TYPE))
            if (facilityType == "NON-ATCT" && twr7 != null && twr7.count == 0) {
                // Show remarks, if any, since there are no frequencies listed
                val twr6 = result[6]
                if (twr6 != null && twr6.moveToFirst()) {
                    do {
                        val remark = twr6.getString(twr6.getColumnIndexOrThrow(Tower6.REMARK_TEXT))
                        addBulletedRow(layout!!, remark)
                        ++row
                    } while (twr6.moveToNext())
                }
            }
        }

        if (row == 0) {
            label?.visibility = View.GONE
            layout?.visibility = View.GONE
        }
    }

    private fun showAwosDetails(result: Array<Cursor?>) {
        val layout = findViewById<LinearLayout>(R.id.detail_awos_layout) ?: return
        val awos1 = result[7]
        if (awos1?.moveToFirst() == true) {
            do {
                if (awos1.position == 5) {
                    break
                }
                var icaoCode = awos1.getString(awos1.getColumnIndexOrThrow(Wxs.STATION_ID))
                val sensorId = awos1.getString(awos1.getColumnIndexOrThrow(Awos1.WX_SENSOR_IDENT))
                if (icaoCode == null || icaoCode.isEmpty()) {
                    icaoCode = "K$sensorId"
                }
                val type = awos1.getString(awos1.getColumnIndexOrThrow(Awos1.WX_SENSOR_TYPE))
                var freq = awos1.getString(awos1.getColumnIndexOrThrow(Awos1.STATION_FREQUENCY))
                if (freq.isNullOrBlank()) {
                    freq = awos1.getString(awos1.getColumnIndexOrThrow(Awos1.SECOND_STATION_FREQUENCY))
                }
                val phone = awos1.getString(awos1.getColumnIndexOrThrow(Awos1.STATION_PHONE_NUMBER)) ?: ""
                val name = awos1.getString(awos1.getColumnIndexOrThrow(Wxs.STATION_NAME)) ?: ""
                val distance = awos1.getFloat(awos1.getColumnIndexOrThrow("DISTANCE"))
                val bearing = awos1.getFloat(awos1.getColumnIndexOrThrow("BEARING"))

                val extras = Bundle().apply {
                    putString(NoaaService.STATION_ID, icaoCode)
                    putString(Awos1.WX_SENSOR_IDENT, sensorId)
                    putString(Airports.SITE_NUMBER, mSiteNumber)
                }

                val runnable = Runnable {
                    cacheMetars()
                    val intent = Intent(activity, WxDetailActivity::class.java)
                    intent.putExtras(extras)
                    startActivity(intent)
                }
                addAwosRow(layout, icaoCode, name, type, freq, phone, distance,
                        bearing, runnable)
            } while (awos1.moveToNext())

            if (!awos1.isAfterLast) {
                val intent = Intent(activity, NearbyWxActivity::class.java)
                intent.putExtra(LocationColumns.LOCATION, mLocation)
                intent.putExtra(LocationColumns.RADIUS, mRadius)
                addClickableRow(layout, "More...", intent)
            }
        } else {
            addRow(layout, "No Wx stations found nearby.")
        }
    }

    private fun showHomeDistance(result: Array<Cursor?>) {
        val layout = findViewById<LinearLayout>(R.id.detail_home_layout) ?: return
        val home: Cursor? = result[14]
        if (home == null) {
            val runnable = Runnable {
                if (activity != null) {
                    val prefs = Intent(activity, PreferencesActivity::class.java)
                    startActivity(prefs)
                    activity?.finish()
                }
            }
            addClickableRow(layout, "Tap here to set home airport", runnable)
        } else if (home.moveToFirst()) {
            val siteNumber = home.getString(home.getColumnIndexOrThrow(Airports.SITE_NUMBER))
            if (siteNumber == mSiteNumber) {
                addRow(layout, "This is your home airport")
            } else {
                val lat = home.getDouble(home.getColumnIndexOrThrow(
                        Airports.REF_LATTITUDE_DEGREES))
                val lon = home.getDouble(home.getColumnIndexOrThrow(
                        Airports.REF_LONGITUDE_DEGREES))
                val results = FloatArray(3)
                Location.distanceBetween(lat, lon,
                        mLocation.latitude, mLocation.longitude, results)
                val distance = (results[0] / GeoUtils.METERS_PER_NAUTICAL_MILE).roundToInt()
                val initialBearing = ((results[1] + mDeclination + 360f) % 360).roundToInt()
                val finalBearing = ((results[2] + mDeclination + 360f) % 360).roundToInt()

                val strDistance = FormatUtils.formatNauticalMiles(distance.toFloat())
                val strBearing = GeoUtils.getCardinalDirection(initialBearing.toFloat())
                addRow(layout, "Distance from $mHome", "$strDistance $strBearing")
                addRow(layout, "Initial bearing",
                        FormatUtils.formatDegrees(initialBearing) + " M")
                if (abs(finalBearing - initialBearing) >= 10) {
                    addRow(layout, "Final bearing",
                            FormatUtils.formatDegrees(finalBearing) + " M")
                }
            }
        } else {
            addRow(layout, "Home airport '$mHome' not found")
        }
    }

    private fun showNearbyFacilities() {
        val layout = findViewById<LinearLayout>(R.id.detail_nearby_layout) ?: return

        arguments?.putParcelable(LocationColumns.LOCATION, mLocation)
        addClickableRow(layout, "Airports", NearbyAirportsFragment::class.java, arguments)
        addClickableRow(layout, "FSS outlets", NearbyFssFragment::class.java, arguments)
        addClickableRow(layout, "Navaids", NearbyNavaidsFragment::class.java, arguments)
        addClickableRow(layout, "Obstacles", NearbyObstaclesFragment::class.java, arguments)
    }

    private fun showNotamAndTfr() {
        val layout = findViewById<LinearLayout>(R.id.detail_notam_faa_layout) ?: return
        var intent = Intent(activity, AirportNotamActivity::class.java)
        intent.putExtra(Airports.SITE_NUMBER, mSiteNumber)
        addClickableRow(layout, "View NOTAMs", intent)
        intent = Intent(activity, TfrListActivity::class.java)
        addClickableRow(layout, "View TFRs", intent)
    }

    private fun showCharts(result: Array<Cursor?>) {
        val apt = result[0] ?: return

        val layout = findViewById<LinearLayout>(R.id.detail_charts_layout) ?: return
        var sectional: String? = apt.getString(apt.getColumnIndexOrThrow(Airports.SECTIONAL_CHART))
        if (sectional == null || sectional.isEmpty()) {
            sectional = "N/A"
        }
        val lat = apt.getString(apt.getColumnIndexOrThrow(Airports.REF_LATTITUDE_DEGREES))
        val lon = apt.getString(apt.getColumnIndexOrThrow(Airports.REF_LONGITUDE_DEGREES))
        if (lat.isNotEmpty() && lon.isNotEmpty()) {
            // Link to the sectional at VFRMAP if location is available
            var uri = Uri.parse("http://vfrmap.com/?type=vfrc&lat=$lat&lon=$lon&zoom=10")
            var intent = Intent(Intent.ACTION_VIEW, uri)
            addClickableRow(layout, "$sectional Sectional VFR", null, intent)
            uri = Uri.parse("http://vfrmap.com/?type=ifrlc&lat=$lat&lon=$lon&zoom=10")
            intent = Intent(Intent.ACTION_VIEW, uri)
            addClickableRow(layout, "Low-altitude IFR", intent)
            uri = Uri.parse("http://vfrmap.com/?type=ehc&lat=$lat&lon=$lon&zoom=10")
            intent = Intent(Intent.ACTION_VIEW, uri)
            addClickableRow(layout, "High-altitude IFR", intent)
        } else {
            addRow(layout, "Sectional chart", sectional)
        }

        val classb = result[15]
        if (classb != null && classb.moveToFirst()) {
            val seen = HashSet<String>()
            do {
                val faaCode = classb.getString(classb.getColumnIndexOrThrow(Airports.FAA_CODE))
                val classBName = ClassBUtils.getClassBName(faaCode)
                if (!seen.contains(classBName)) {
                    val row = addClickableRow(layout, "$classBName Class B airspace", "")
                    row.tag = faaCode
                    row.setOnClickListener { v ->
                        val faaCode1 = v.tag as String
                        getClassBGraphic(faaCode1)
                    }
                    seen.add(classBName)
                }
            } while (classb.moveToNext())
        }
    }

    private fun showOperationsDetails(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        val layout = findViewById<LinearLayout>(R.id.detail_operations_layout)
        val use = apt.getString(apt.getColumnIndexOrThrow(Airports.FACILITY_USE))
        addRow(layout!!, "Operation", DataUtils.decodeFacilityUse(use))
        val faaCode = apt.getString(apt.getColumnIndexOrThrow(Airports.FAA_CODE))
        addRow(layout, "FAA code", faaCode)
        val timezoneId = apt.getString(apt.getColumnIndexOrThrow(Airports.TIMEZONE_ID))
        if (timezoneId.isNotBlank()) {
            val tz = TimeZone.getTimeZone(timezoneId)
            addRow(layout, "Local time zone", TimeUtils.getTimeZoneAsString(tz))
        }
        val activation = apt.getString(apt.getColumnIndexOrThrow(Airports.ACTIVATION_DATE))
        if (activation.isNotBlank()) {
            addRow(layout, "Activation date", activation)
        }
        val twr8 = result[9]
        if (twr8 != null && twr8.moveToFirst()) {
            val airspace = twr8.getString(twr8.getColumnIndexOrThrow(Tower8.AIRSPACE_TYPES))
            val value = DataUtils.decodeAirspace(airspace)
            val hours = twr8.getString(twr8.getColumnIndexOrThrow(Tower8.AIRSPACE_HOURS))
            addRow(layout, "Airspace", value, hours)
        }
        val tower = apt.getString(apt.getColumnIndexOrThrow(Airports.TOWER_ON_SITE))
        addRow(layout, "Control tower", if (tower == "Y") "Yes" else "No")

        val twr5: Cursor? = result[16]
        if (twr5 != null && twr5.moveToFirst()) {
            val radarList = HashSet<String>()
            for (i: Int in 1..4) {
                val towerRadar = twr5.getString(twr5.getColumnIndexOrThrow("TOWER_RADAR_TYPE_$i"))
                if (towerRadar.isNotBlank()) {
                    radarList.add(towerRadar)
                }
            }

            if (radarList.isNotEmpty()) {
                addRow(layout, "Tower radar", radarList.joinToString())
            }
        }

        val windIndicator = apt.getString(apt.getColumnIndexOrThrow(Airports.WIND_INDICATOR))
        addRow(layout, "Wind indicator", DataUtils.decodeWindIndicator(windIndicator))
        val circle = apt.getString(apt.getColumnIndexOrThrow(Airports.SEGMENTED_CIRCLE))
        addRow(layout, "Segmented circle", if (circle == "Y") "Yes" else "No")
        val beacon = apt.getString(apt.getColumnIndexOrThrow(Airports.BEACON_COLOR))
        addRow(layout, "Beacon", DataUtils.decodeBeacon(beacon))
        var lighting = apt.getString(apt.getColumnIndexOrThrow(Airports.LIGHTING_SCHEDULE))
        if (lighting.isNotBlank()) {
            addRow(layout, "Airport lighting", lighting)
        }
        lighting = apt.getString(apt.getColumnIndexOrThrow(Airports.BEACON_LIGHTING_SCHEDULE))
        if (lighting.isNotBlank()) {
            addRow(layout, "Beacon lighting", lighting)
        }

        val landingFee = apt.getString(apt.getColumnIndexOrThrow(Airports.LANDING_FEE))
        addRow(layout, "Landing fee", if (landingFee == "Y") "Yes" else "No")
        var dir = apt.getString(apt.getColumnIndexOrThrow(Airports.MAGNETIC_VARIATION_DIRECTION))
        if (dir.isNotBlank()) {
            val variation = apt.getInt(apt.getColumnIndexOrThrow(Airports.MAGNETIC_VARIATION_DEGREES))
            val year = apt.getString(apt.getColumnIndexOrThrow(Airports.MAGNETIC_VARIATION_YEAR))
            if (year.isNotBlank()) {
                addRow(layout, "Magnetic variation", "$variation\u00B0 $dir ($year)")
            } else {
                addRow(layout, "Magnetic variation", "$variation\u00B0 $dir")
            }
        } else {
            val variation = GeoUtils.getMagneticDeclination(mLocation).roundToInt()
            dir = if (variation >= 0) "W" else "E"
            addRow(layout, "Magnetic variation", "${abs(variation)}\u00B0 $dir (actual)")
        }
        val intlEntry = apt.getString(apt.getColumnIndexOrThrow(Airports.INTL_ENTRY_AIRPORT))
        if (intlEntry == "Y") {
            addRow(layout, "International entry", "Yes")
        }
        val customs = apt.getString(apt.getColumnIndexOrThrow(Airports.CUSTOMS_LANDING_RIGHTS_AIRPORT))
        if (customs == "Y") {
            addRow(layout, "Customs landing rights", "Yes")
        }
        val jointUse = apt.getString(apt.getColumnIndexOrThrow(Airports.CIVIL_MILITARY_JOINT_USE))
        if (jointUse == "Y") {
            addRow(layout, "Civil/military joint use", "Yes")
        }
        val militaryRights = apt.getString(apt.getColumnIndexOrThrow(Airports.MILITARY_LANDING_RIGHTS))
        if (militaryRights != null && militaryRights == "Y") {
            addRow(layout, "Military landing rights", "Yes")
        }
        val medical = apt.getString(apt.getColumnIndexOrThrow(Airports.MEDICAL_USE))
        if (medical == "Y") {
            addRow(layout, "Medical use", "Yes")
        }
    }

    private fun showAeroNavDetails(result: Array<Cursor?>) {
        val layout = findViewById<LinearLayout>(R.id.detail_aeronav_layout) ?: return
        val apt = result[0] ?: return
        val siteNumber = apt.getString(apt.getColumnIndexOrThrow(Airports.SITE_NUMBER))
        val cycle = result[12]
        if (cycle != null && cycle.moveToFirst()) {
            val afdCycle = cycle.getString(cycle.getColumnIndexOrThrow(DafdCycle.AFD_CYCLE))
            val dafd = result[13]
            if (dafd != null && dafd.moveToFirst()) {
                val pdfName = dafd.getString(dafd.getColumnIndexOrThrow(Dafd.PDF_NAME))
                val row = addClickableRow(layout, "d-CS page", "")
                row.setTag(R.id.DAFD_CYCLE, afdCycle)
                row.setTag(R.id.DAFD_PDF_NAME, pdfName)
                row.setOnClickListener { v ->
                    val afdCycle1 = v.getTag(R.id.DAFD_CYCLE) as String
                    val pdfName1 = v.getTag(R.id.DAFD_PDF_NAME) as String
                    getAfdPage(afdCycle1, pdfName1)
                }
            } else {
                addRow(layout, "d-CS page is not available for this airport")
            }
        } else {
            addRow(layout, "d-CS data not found")
        }
        val dtpp: Cursor? = result[11]
        if (dtpp != null && dtpp.moveToFirst()) {
            val intent = Intent(activity, DtppActivity::class.java)
            intent.putExtra(Airports.SITE_NUMBER, siteNumber)
            addClickableRow(layout, "Instrument procedures", intent)
        } else {
            addRow(layout, "No instrument procedures available")
        }
    }

    private fun showServicesDetails(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        val layout = findViewById<LinearLayout>(R.id.detail_services_layout)
        var fuelTypes = DataUtils.decodeFuelTypes(
                apt.getString(apt.getColumnIndexOrThrow(Airports.FUEL_TYPES)))
        if (fuelTypes.isBlank()) {
            fuelTypes = "No"
        }
        addRow(layout!!, "Fuel available", fuelTypes)
        var repair = apt.getString(apt.getColumnIndexOrThrow(Airports.AIRFRAME_REPAIR_SERVICE)) ?: ""
        if (repair.isBlank()) {
            repair = "No"
        }
        addRow(layout, "Airframe repair", repair)
        repair = apt.getString(apt.getColumnIndexOrThrow(Airports.POWER_PLANT_REPAIR_SERVICE)) ?: ""
        if (repair.isBlank()) {
            repair = "No"
        }
        addRow(layout, "Power plant repair", repair)
        addClickableRow(layout, "Other services", ServicesFragment::class.java, arguments)
    }

    private fun showOtherDetails() {
        val layout = findViewById<LinearLayout>(R.id.detail_other_layout) ?: return
        addClickableRow(layout, "Ownership and contact", OwnershipFragment::class.java, arguments)
        addClickableRow(layout, "Aircraft operations", AircraftOpsFragment::class.java, arguments)
        addClickableRow(layout, "Additional remarks", RemarksFragment::class.java, arguments)
        addClickableRow(layout, "Attendance", AttendanceFragment::class.java, arguments)
        addClickableRow(layout, "Sunrise and sunset", AlmanacFragment::class.java, arguments)
    }

    private fun addAwosRow(layout: LinearLayout, id: String, name: String, type: String,
                           freq: String?, phone: String, distance: Float, bearing: Float,
                           runnable: Runnable) {
        val label1 = if (name.isNotBlank()) "$id - $name" else id

        val f = freq?.toFloatOrNull() ?: 0f
        val value1 = if (f > 0) FormatUtils.formatFreq(f) else ""

        val sb = StringBuilder()
        sb.append(type)
        if (mIcaoCode == id) {
            sb.append(", On-site")
        } else {
            sb.append(", ")
            sb.append(FormatUtils.formatNauticalMiles(distance))
            sb.append(" ")
            sb.append(GeoUtils.getCardinalDirection(bearing))
        }
        val label2 = sb.toString()

        val row = addClickableRow(layout, label1, value1, label2, phone, runnable)
        var tv = row.findViewById<TextView>(R.id.item_label)
        tv.tag = id
        // Make phone number clickable
        tv = row.findViewById(R.id.item_extra_value)
        if (!tv.text.isNullOrBlank()) {
            makeClickToCall(tv)
        }
        if (row is LinearLayout) {
            mAwosViews.add(row)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addRunwayRow(layout: LinearLayout, c: Cursor) {
        val siteNumber = c.getString(c.getColumnIndexOrThrow(Runways.SITE_NUMBER))
        val runwayId = c.getString(c.getColumnIndexOrThrow(Runways.RUNWAY_ID))
        val length = c.getInt(c.getColumnIndexOrThrow(Runways.RUNWAY_LENGTH))
        val width = c.getInt(c.getColumnIndexOrThrow(Runways.RUNWAY_WIDTH))
        val surfaceType = c.getString(c.getColumnIndexOrThrow(Runways.SURFACE_TYPE))
        val baseId = c.getString(c.getColumnIndexOrThrow(Runways.BASE_END_ID))
        val reciprocalId = c.getString(c.getColumnIndexOrThrow(Runways.RECIPROCAL_END_ID))
        val baseRP = c.getString(c.getColumnIndexOrThrow(Runways.BASE_END_RIGHT_TRAFFIC))
        val reciprocalRP = c.getString(
                c.getColumnIndexOrThrow(Runways.RECIPROCAL_END_RIGHT_TRAFFIC))

        var rp: String? = null
        if (baseRP == "Y" && reciprocalRP == "Y") {
            rp = " (RP)"
        } else if (baseRP == "Y") {
            rp = " (RP $baseId)"
        } else if (reciprocalRP == "Y") {
            rp = " (RP $reciprocalId)"
        }

        var heading = c.getInt(c.getColumnIndexOrThrow(Runways.BASE_END_HEADING))
        heading = if (heading > 0) {
            GeoUtils.applyDeclination(heading.toLong(), mDeclination).toInt()
        } else {
            // Actual heading is not available, try to deduce it from runway id
            DataUtils.getRunwayHeading(runwayId)
        }

        val row = inflate<RelativeLayout>(R.layout.runway_detail_item)

        var tv = row.findViewById<TextView>(R.id.runway_id)
        tv?.text = runwayId
        activity?.let { UiUtils.setRunwayDrawable(it, tv, runwayId, length, heading) }

        if (rp != null) {
            tv = row.findViewById(R.id.runway_rp)
            tv?.text = rp
            tv?.visibility = View.VISIBLE
        }

        val runwayLength = FormatUtils.formatFeet(length.toFloat())
        val runwayWidth = FormatUtils.formatFeet(width.toFloat())
        tv = row.findViewById(R.id.runway_size)
        tv?.text = "$runwayLength x $runwayWidth"

        tv = row.findViewById(R.id.runway_surface)
        tv?.text = DataUtils.decodeSurfaceType(surfaceType)

        if (!runwayId.startsWith("H")) {
            // Save the textview and runway info for later use
            tv = row.findViewById(R.id.runway_wind_info)
            val tag = Bundle()
            tag.putString(Runways.BASE_END_ID, baseId)
            tag.putString(Runways.RECIPROCAL_END_ID, reciprocalId)
            tag.putInt(Runways.BASE_END_HEADING, heading)
            tv?.tag = tag
            mRunwayViews.add(tv!!)
        }

        val args = Bundle()
        args.putString(Runways.SITE_NUMBER, siteNumber)
        args.putString(Runways.RUNWAY_ID, runwayId)
        addClickableRow(layout, row, RunwaysFragment::class.java, args)
    }

    private fun cacheMetars() {
        requestMetars(NoaaService.ACTION_CACHE_METAR)
    }

    private fun requestMetars(force: Boolean) {
        val cacheOnly = !NetworkUtils.canDownloadData(activityBase)
        requestMetars(NoaaService.ACTION_GET_METAR, force, cacheOnly)
    }

    private fun requestMetars(action: String, force: Boolean = false, cacheOnly: Boolean = false) {
        if (mAwosViews.isEmpty()) {
            return
        }

        activity?.let {
            val stationIds = ArrayList<String>()
            for (row in mAwosViews) {
                val tv = row.findViewById<TextView>(R.id.item_label)
                val stationId = tv.tag as String
                stationIds.add(stationId)
            }
            val service = Intent(activity, MetarService::class.java)
            service.action = action
            service.putExtra(NoaaService.STATION_IDS, stationIds)
            service.putExtra(NoaaService.TYPE, NoaaService.TYPE_TEXT)
            if (force) {
                service.putExtra(NoaaService.FORCE_REFRESH, true)
            } else if (cacheOnly) {
                service.putExtra(NoaaService.CACHE_ONLY, true)
            }
            it.startService(service)
        }
    }

    private fun showWxInfo(metar: Metar) {
        if (metar.stationId == null) {
            return
        }

        if (metar.isValid
                && mIcaoCode == metar.stationId
                && WxUtils.isWindAvailable(metar)) {
            showRunwayWindInfo(metar)
        }

        for (row in mAwosViews) {
            var tv = row.findViewById<TextView>(R.id.item_label)
            val icaoCode = tv.tag as String
            if (icaoCode == metar.stationId) {
                WxUtils.setColorizedWxDrawable(tv, metar, mDeclination)
                tv = row.findViewById(R.id.item_extra_label2)
                tv.visibility = View.VISIBLE

                val info = StringBuilder()
                info.append(metar.flightCategory)

                if (metar.wxList.isNotEmpty()) {
                    for (wx in metar.wxList) {
                        if (wx.symbol != "NSW") {
                            info.append(", ")
                            info.append(wx.toString().lowercase(Locale.US))
                        }
                    }
                }

                if (metar.visibilitySM < java.lang.Float.MAX_VALUE) {
                    info.append(", ")
                    info.append(FormatUtils.formatStatuteMiles(metar.visibilitySM))
                }

                if (metar.windSpeedKnots < Integer.MAX_VALUE) {
                    if (metar.windSpeedKnots == 0) {
                        info.append(", winds calm")
                    } else {
                        info.append(", ${metar.windSpeedKnots} knots ")
                        info.append(GeoUtils.getCardinalDirection(metar.windDirDegrees.toFloat()))
                        if (metar.windGustKnots < Integer.MAX_VALUE) {
                            info.append(" gusting")
                        }
                    }
                }

                info.append(", ")
                var sky = WxUtils.getCeiling(metar.skyConditions)
                var skyCover = sky.skyCover
                if (skyCover == "OVX") {
                    info.append("Ceiling indefinite")
                } else if (skyCover != "NSC") {
                    val ceiling = sky.cloudBaseAGL
                    info.append("Ceiling ")
                    info.append(FormatUtils.formatFeet(ceiling.toFloat()))
                } else {
                    if (metar.skyConditions.isNotEmpty()) {
                        sky = metar.skyConditions[0]
                        skyCover = sky.skyCover
                        if (skyCover == "CLR" || skyCover == "SKC") {
                            info.append("Sky clear")
                        } else if (skyCover != "SKM") {
                            info.append(skyCover)
                            info.append(" ")
                            info.append(FormatUtils.formatFeet(sky.cloudBaseAGL.toFloat()))
                        }
                    }
                }

                tv.text = info.toString()
                break
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showRunwayWindInfo(metar: Metar) {
        for (tv in mRunwayViews) {
            val tag = tv.tag as Bundle
            var id = tag.getString(Runways.BASE_END_ID)
            var rwyHeading = tag.getInt(Runways.BASE_END_HEADING).toLong()
            val windDir = GeoUtils.applyDeclination(metar.windDirDegrees.toLong(), mDeclination)
            val headWind = WxUtils.getHeadWindComponent(metar.windSpeedKnots.toDouble(),
                    windDir.toDouble(), rwyHeading.toDouble())

            if (headWind < 0) {
                // If this is a tail wind, use the other end
                id = tag.getString(Runways.RECIPROCAL_END_ID)
                rwyHeading = (rwyHeading + 180) % 360
            }
            var crossWind = WxUtils.getCrossWindComponent(metar.windSpeedKnots.toDouble(),
                    windDir.toDouble(), rwyHeading.toDouble())
            val side = if (crossWind >= 0) "right" else "left"
            crossWind = abs(crossWind)
            val windInfo = StringBuilder()
            if (crossWind > 0) {
                windInfo.append("$crossWind ${if (crossWind>1) "knots" else "knot"} $side x-wind")
            } else {
                windInfo.append("no x-wind")
            }
            if (metar.windGustKnots < Integer.MAX_VALUE) {
                val gustFactor = round((metar.windGustKnots - metar.windSpeedKnots) / 2.0)
                windInfo.append(", $gustFactor knots gust factor")
            }
            tv.text = "Rwy $id: $windInfo"
            tv.visibility = View.VISIBLE
        }
    }

    private fun doQuery(siteNumber: String): Array<Cursor?> {
        val cursors = arrayOfNulls<Cursor>(17)

        mSiteNumber = siteNumber
        var db = getDatabase(DB_FADDS)

        val apt = getAirportDetails(mSiteNumber) ?: return cursors
        cursors[0] = apt

        val faaCode = apt.getString(apt.getColumnIndexOrThrow(Airports.FAA_CODE))
        val lat = apt.getDouble(apt.getColumnIndexOrThrow(Airports.REF_LATTITUDE_DEGREES))
        val lon = apt.getDouble(apt.getColumnIndexOrThrow(Airports.REF_LONGITUDE_DEGREES))
        val elevMsl = apt.getInt(apt.getColumnIndexOrThrow(Airports.ELEVATION_MSL))
        mFAACode = apt.getString(apt.getColumnIndexOrThrow(Airports.FAA_CODE))
        mIcaoCode = apt.getString(apt.getColumnIndexOrThrow(Airports.ICAO_CODE)) ?: ""
        if (mIcaoCode.isEmpty()) {
            mIcaoCode = "K" + apt.getString(apt.getColumnIndexOrThrow(Airports.FAA_CODE))
        }

        val location = Location("")
        location.latitude = lat
        location.longitude = lon
        location.altitude = elevMsl.toDouble()

        mDeclination = GeoUtils.getMagneticDeclination(location)
        mLocation = location

        var builder = SQLiteQueryBuilder()
        builder.tables = Runways.TABLE_NAME
        cursors[1] = builder.query(db, arrayOf(Runways.SITE_NUMBER, Runways.RUNWAY_ID,
                Runways.RUNWAY_LENGTH, Runways.RUNWAY_WIDTH, Runways.SURFACE_TYPE,
                Runways.BASE_END_HEADING, Runways.BASE_END_ID, Runways.RECIPROCAL_END_ID,
                Runways.BASE_END_RIGHT_TRAFFIC, Runways.RECIPROCAL_END_RIGHT_TRAFFIC),
                "${Runways.SITE_NUMBER} = ? AND ${Runways.RUNWAY_LENGTH} > 0",
                arrayOf(mSiteNumber), null, null, null, null)

        builder = SQLiteQueryBuilder()
        builder.tables = Remarks.TABLE_NAME
        cursors[2] = builder.query(db, arrayOf(Remarks.REMARK_TEXT),
                "${Runways.SITE_NUMBER} = ? AND ${Remarks.REMARK_NAME} " +
                        "in ('E147', 'A3', 'A24', 'A70', 'A75', 'A82')",
                arrayOf(mSiteNumber), null, null, null, null)

        builder = SQLiteQueryBuilder()
        builder.tables = Tower1.TABLE_NAME
        cursors[3] = builder.query(db, arrayOf("*"),
                "${Tower1.SITE_NUMBER} = ?",
                arrayOf(mSiteNumber), null, null, null, null)

        builder = SQLiteQueryBuilder()
        builder.tables = Tower3.TABLE_NAME
        cursors[4] = builder.query(db, arrayOf("*"),
                "${Tower3.FACILITY_ID} = ?",
                arrayOf(faaCode), null, null, null, null)

        builder = SQLiteQueryBuilder()
        builder.tables = Tower7.TABLE_NAME
        val c = builder.query(db, arrayOf("*"),
                "${Tower7.SATELLITE_AIRPORT_SITE_NUMBER} = ?",
                arrayOf(mSiteNumber), null, null, null, null)
        cursors[5] = c

        if (!c.moveToFirst()) {
            builder = SQLiteQueryBuilder()
            builder.tables = Tower6.TABLE_NAME
            cursors[6] = builder.query(db, arrayOf("*"),
                    "${Tower6.FACILITY_ID} = ?",
                    arrayOf(faaCode), null, null, Tower6.ELEMENT_NUMBER, null)
        }

        cursors[7] = NearbyWxCursor(db, mLocation, mRadius)

        builder = SQLiteQueryBuilder()
        builder.tables = Aff3.TABLE_NAME
        cursors[8] = builder.query(db, arrayOf("*"),
                "${Aff3.IFR_FACILITY_ID} = ?",
                arrayOf(faaCode), null, null, null, null)

        builder = SQLiteQueryBuilder()
        builder.tables = Tower8.TABLE_NAME
        cursors[9] = builder.query(db, arrayOf("*"),
                "${Tower8.FACILITY_ID} = ?",
                arrayOf(faaCode), null, null, null, null)

        builder = SQLiteQueryBuilder()
        builder.tables = Attendance.TABLE_NAME
        cursors[10] = builder.query(db,
                arrayOf(Attendance.ATTENDANCE_SCHEDULE),
                "${Attendance.SITE_NUMBER} = ?",
                arrayOf(mSiteNumber), null, null, Attendance.SEQUENCE_NUMBER, null)

        db = getDatabase(DB_DTPP)
        builder = SQLiteQueryBuilder()
        builder.tables = Dtpp.TABLE_NAME
        cursors[11] = builder.query(db, arrayOf("*"),
                "${Dtpp.FAA_CODE} = ?",
                arrayOf(faaCode), null, null, null, null)

        db = getDatabase(DB_DAFD)
        builder = SQLiteQueryBuilder()
        builder.tables = DafdCycle.TABLE_NAME
        cursors[12] = builder.query(db, arrayOf("*"),
                null, null, null, null, null, null)

        builder = SQLiteQueryBuilder()
        builder.tables = Dafd.TABLE_NAME
        cursors[13] = builder.query(db, arrayOf("*"),
                "${Dafd.FAA_CODE} = ?",
                arrayOf(faaCode), null, null, null, null)

        db = getDatabase(DB_FADDS)
        if (mHome.isNotEmpty()) {
            builder = SQLiteQueryBuilder()
            builder.tables = Airports.TABLE_NAME
            cursors[14] = builder.query(db,
                    arrayOf(Airports.SITE_NUMBER,
                            Airports.REF_LATTITUDE_DEGREES,
                            Airports.REF_LONGITUDE_DEGREES),
                    "${Airports.FAA_CODE} = ? OR ${Airports.ICAO_CODE} = ?",
                    arrayOf(mHome, mHome), null, null, null, null)
        }

        // Get nearby Class B airports
        val selection = " AND ${Airports.FAA_CODE} IN (${ClassBUtils.getClassBFacilityList()})"
        cursors[15] = NearbyAirportsCursor(db, mLocation, 50, selection)

        builder = SQLiteQueryBuilder()
        builder.tables = Tower5.TABLE_NAME
        cursors[16] = builder.query(db, arrayOf("*"),
                "${Tower5.FACILITY_ID} = ?", arrayOf(faaCode),
                null, null, null, null)
        return cursors
    }

}
