/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2019 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.*
import com.nadmm.airports.utils.CursorAsyncTask
import com.nadmm.airports.utils.DataUtils
import com.nadmm.airports.utils.FormatUtils
import com.nadmm.airports.utils.UiUtils

abstract class FragmentBase : Fragment(), IRefreshable {

    lateinit var activityBase: ActivityBase
        private set
    private var mTask: CursorAsyncTask<*>? = null

    private val mOnRowClickListener = { v : View ->
        val tag = v.tag
        if (tag is Intent) {
            startActivity(tag)
        } else if (tag is Runnable) {
            tag.run()
        }
    }

    private val mOnPhoneClickListener = { v : View ->
        val tv = v as TextView
        val action = tv.tag as String
        val phone = DataUtils.decodePhoneNumber(tv.text.toString())
        val intent = Intent(action, Uri.parse("tel:$phone"))
        startActivity(intent)
    }

    var isRefreshing: Boolean
        get() = activityBase.isRefreshing
        set(refreshing) {
            activityBase.isRefreshing = refreshing
        }

    val dbManager: DatabaseManager
        get() = activityBase.dbManager

    protected val supportActionBar: ActionBar?
        get() = activityBase.supportActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityBase = activity as ActivityBase
    }

    override fun onPause() {
        isRefreshing = false
        if (mTask != null) {
            mTask!!.cancel(true)
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        activityBase.onFragmentStarted(this)
    }

    override fun isRefreshable(): Boolean {
        return false
    }

    override fun requestDataRefresh() {}

    fun getDatabase(type: String): SQLiteDatabase {
        return activityBase.getDatabase(type)
    }

    protected fun createContentView(id: Int): View? {
        return activityBase.createContentView(id)
    }

    protected fun createContentView(view: View): View? {
        return activityBase.createContentView(view)
    }

    protected fun setContentShown(shown: Boolean) {
        activityBase.setContentShown(shown)
    }

    protected fun setContentMsg(msg: String) {
        activityBase.setContentMsg(msg)
    }

    protected fun setFragmentContentShown(shown: Boolean) {
        activityBase.setContentShown(view, shown)
    }

    protected fun setFragmentContentShownNoAnimation(shown: Boolean) {
        activityBase.setContentShownNoAnimation(view, shown)
    }

    protected fun <T : FragmentBase> setBackgroundTask(task: CursorAsyncTask<T>): CursorAsyncTask<T> {
        mTask = task
        return task
    }

    protected fun setActionBarTitle(c: Cursor, subtitle: String) {
        activityBase.setActionBarTitle(c, subtitle)
    }

    protected fun setActionBarTitle(title: String) {
        activityBase.setActionBarTitle(title)
    }

    protected fun setActionBarTitle(title: String, subTitle: String) {
        activityBase.setActionBarTitle(title, subTitle)
    }

    protected fun setActionBarSubtitle(subtitle: String) {
        activityBase.setActionBarSubtitle(subtitle)
    }

    fun getAirportDetails(siteNumber: String): Cursor? {
        return activityBase.getAirportDetails(siteNumber)
    }

    fun showAirportTitle(c: Cursor) {
        activityBase.showAirportTitle(c)
        activityBase.showFaddsEffectiveDate(c)
    }

    protected fun showNavaidTitle(c: Cursor) {
        activityBase.showNavaidTitle(c)
    }

    @SuppressLint("SetTextI18n")
    fun showWxTitle(cursors: Array<Cursor>) {
        val wxs = cursors[0]
        val awos = cursors[1]

        val root = view ?: return

        var tv = root.findViewById<TextView>(R.id.wx_station_name)
        val icaoCode = wxs.getString(wxs.getColumnIndex(Wxs.STATION_ID))
        val stationName = wxs.getString(wxs.getColumnIndex(Wxs.STATION_NAME))
        tv.text = "$icaoCode - $stationName"
        if (awos.moveToFirst()) {
            tv = root.findViewById(R.id.wx_station_info)
            var type = awos.getString(awos.getColumnIndex(Awos1.WX_SENSOR_TYPE))
            if (type.isNullOrBlank()) {
                type = "ASOS/AWOS"
            }
            val city = awos.getString(awos.getColumnIndex(Airports.ASSOC_CITY))
            val state = awos.getString(awos.getColumnIndex(Airports.ASSOC_STATE))
            tv.text = "$type, $city, $state"

            val phone = awos.getString(awos.getColumnIndex(Awos1.STATION_PHONE_NUMBER))
            if (!phone.isNullOrBlank()) {
                tv = root.findViewById(R.id.wx_station_phone)
                tv.text = phone
                makeClickToCall(tv)
                tv.visibility = View.VISIBLE
            }

            var freq = awos.getString(awos.getColumnIndex(Awos1.STATION_FREQUENCY))
            if (!freq.isNullOrBlank()) {
                tv = root.findViewById(R.id.wx_station_freq)
                UiUtils.setTextViewDrawable(tv, R.drawable.ic_antenna)
                tv.text = freq
                tv.visibility = View.VISIBLE
            }

            freq = awos.getString(awos.getColumnIndex(Awos1.SECOND_STATION_FREQUENCY))
            if (!freq.isNullOrBlank()) {
                tv = root.findViewById(R.id.wx_station_freq2)
                UiUtils.setTextViewDrawable(tv, R.drawable.ic_antenna)
                tv.text = freq
                tv.visibility = View.VISIBLE
            }
        } else {
            tv = root.findViewById(R.id.wx_station_info)
            tv.text = "ASOS/AWOS"
        }

        val s = wxs.getInt(wxs.getColumnIndex(Wxs.STATION_ELEVATOIN_METER))
        tv = root.findViewById(R.id.wx_station_info2)
        val elev = FormatUtils.formatFeet(DataUtils.metersToFeet(s.toLong()).toFloat())
        tv.text = "Located at $elev MSL elevation"

        val cb = root.findViewById<CheckBox>(R.id.airport_star)
        cb.isChecked = dbManager.isFavoriteWx(icaoCode)
        cb.tag = icaoCode
        cb.setOnClickListener { v ->
            val cb1 = v as CheckBox
            val icaoCode1 = cb1.tag as String
            if (cb1.isChecked) {
                dbManager.addToFavoriteWx(icaoCode1)
                Toast.makeText(activityBase, "Added to favorites list",
                        Toast.LENGTH_SHORT).show()
            } else {
                dbManager.removeFromFavoriteWx(icaoCode1)
                Toast.makeText(activityBase, "Removed from favorites list",
                        Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun makeClickToCall(row: View, resid: Int) {
        val tv = row.findViewById<TextView>(resid)
        makeClickToCall(tv)
        if (tv.isClickable) {
            val backgroundResource = UiUtils.getSelectableItemBackgroundResource(activity!!)
            row.setBackgroundResource(backgroundResource)
        }
    }

    protected fun makeClickToCall(tv: TextView) {
        val pm = activityBase.packageManager
        val hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        if (hasTelephony && tv.text.isNotEmpty()) {
            UiUtils.setTextViewDrawable(tv, R.drawable.ic_phone)
            tv.tag = Intent.ACTION_DIAL
            tv.setOnClickListener(mOnPhoneClickListener)
        } else {
            UiUtils.removeTextViewDrawable(tv)
            tv.setOnClickListener(null)
        }
    }

    private fun makeRowClickable(row: View, clss: Class<*>, args: Bundle?) {
        val r = { activityBase.replaceFragment(clss, args, true) }
        makeRowClickable(row, r)
    }

    private fun makeRowClickable(row: View, tag: Any) {
        row.tag = tag
        row.setOnClickListener(mOnRowClickListener)
        val backgroundResource = UiUtils.getSelectableItemBackgroundResource(activity!!)
        row.setBackgroundResource(backgroundResource)
    }

    protected fun addClickableRow(layout: LinearLayout, label: String, value: String?,
                                  clss: Class<*>, args: Bundle): View {
        val row = addRow(layout, label, value)
        makeRowClickable(row, clss, args)
        return row
    }

    protected fun addClickableRow(layout: LinearLayout, label: String, clss: Class<*>,
                                  args: Bundle?): View {
        return addClickableRow(layout, label, clss, args)
    }

    protected fun addClickableRow(layout: LinearLayout, row: View, clss: Class<*>,
                                  args: Bundle?): View {
        addRow(layout, row)
        makeRowClickable(row, clss, args)
        return row
    }

    protected fun addClickableRow(layout: LinearLayout, label: String, tag: Any): View {
        return addClickableRow(layout, label, null, tag)
    }

    protected fun addClickableRow(layout: LinearLayout, label: String, value: String?,
                                  tag: Any): View {
        val row = addRow(layout, label, value)
        makeRowClickable(row, tag)
        return row
    }

    protected fun addClickableRow(layout: LinearLayout, label1: String, value1: String,
                                  label2: String, value2: String, tag: Any): View {
        val row = addRow(layout, label1, value1, label2, value2)
        makeRowClickable(row, tag)
        return row
    }

    protected fun addClickableRow(layout: LinearLayout, label1: String, value1: String,
                                  label2: String, value2: String, clss: Class<*>,
                                  args: Bundle): View {
        val row = addRow(layout, label1, value1, label2, value2)
        makeRowClickable(row, clss, args)
        return row
    }

    protected fun addPhoneRow(layout: LinearLayout, label: String, phone: String): View {
        val row = addRow(layout, label, phone)
        makeClickToCall(row, R.id.item_value)
        return row
    }

    protected fun addPhoneRow(layout: LinearLayout, phone: String): View {
        val row = addRow(layout, phone)
        makeClickToCall(row, R.id.item_label)
        return row
    }

    protected fun addProgressRow(layout: LinearLayout, label: String): View {
        if (layout.childCount > 0) {
            addSeparator(layout)
        }
        val row = inflate<LinearLayout>(R.layout.list_item_text1)
        val tv = row.findViewById<TextView>(R.id.text)
        tv.text = label
        layout.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        return row
    }

    protected fun addSimpleRow(layout: LinearLayout, value: String): View {
        val tv = inflate<TextView>(R.layout.detail_row_simple)
        tv.text = value
        layout.addView(tv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        return tv
    }

    @JvmOverloads
    protected fun addRow(layout: LinearLayout, label: String, value: String? = null): View {
        if (layout.childCount > 0) {
            addSeparator(layout)
        }

        val row = inflate<LinearLayout>(R.layout.detail_row_item2)
        var tv = row.findViewById<TextView>(R.id.item_label)
        tv.text = label
        tv = row.findViewById(R.id.item_value)
        if (!value.isNullOrBlank()) {
            tv.text = value
        } else {
            tv.visibility = View.GONE
        }
        layout.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        return row
    }

    protected fun addRow(layout: LinearLayout, label: String, value1: String?, value2: String?)
            : View {
        if (layout.childCount > 0) {
            addSeparator(layout)
        }

        val row = inflate<LinearLayout>(R.layout.detail_row_item3)
        var tv = row.findViewById<TextView>(R.id.item_label)
        tv.text = label
        tv = row.findViewById(R.id.item_value)
        if (!value1.isNullOrBlank()) {
            tv.text = value1
        } else {
            tv.visibility = View.GONE
        }
        tv = row.findViewById(R.id.item_extra_value)
        if (!value2.isNullOrBlank()) {
            tv.text = value2
        } else {
            tv.visibility = View.GONE
        }
        layout.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        return row
    }

    protected fun addRow(layout: LinearLayout, label1: String, value1: String?,
                         label2: String?, value2: String?): View {
        if (layout.childCount > 0) {
            addSeparator(layout)
        }

        val row = inflate<LinearLayout>(R.layout.detail_row_item4)
        var tv = row.findViewById<TextView>(R.id.item_label)
        tv.text = label1
        tv = row.findViewById(R.id.item_value)
        if (!value1.isNullOrBlank()) {
            tv.text = value1
        } else {
            tv.visibility = View.GONE
        }
        tv = row.findViewById(R.id.item_extra_label)
        if (!label2.isNullOrBlank()) {
            tv.text = label2
        } else {
            tv.visibility = View.GONE
        }
        tv = row.findViewById(R.id.item_extra_value)
        if (!value2.isNullOrBlank()) {
            tv.text = value2
        } else {
            tv.visibility = View.GONE
        }
        layout.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        return row
    }

    protected fun addBulletedRow(layout: LinearLayout, text: String) {
        val row = inflate<LinearLayout>(R.layout.detail_row_bullet)
        val tv = row.findViewById<TextView>(R.id.item_value)
        tv.text = text
        layout.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    protected fun addRow(layout: LinearLayout, row: View): View {
        if (layout.childCount > 0) {
            addSeparator(layout)
        }

        layout.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        return row
    }

    protected fun addSeparator(layout: LinearLayout) {
        val separator = View(activityBase)
        separator.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.color_background))
        layout.addView(separator, LayoutParams(LayoutParams.MATCH_PARENT, 1))
    }

    fun <T : View> findViewById(id: Int): T? {
        return view?.findViewById(id)
    }

    protected fun <T : View> inflate(id: Int): T {
        return activityBase.inflate(id)
    }

    protected fun <T : View> inflate(id: Int, root: ViewGroup): T {
        return activityBase.inflate(id, root)
    }

}
