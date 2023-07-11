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

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import android.os.Parcelable
import android.provider.BaseColumns
import android.view.KeyEvent
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import com.nadmm.airports.ListFragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports
import com.nadmm.airports.data.DatabaseManager.States
import com.nadmm.airports.utils.CursorAsyncTask
import com.nadmm.airports.utils.SectionedCursorAdapter
import com.nadmm.airports.utils.makeAirportBundle
import java.util.*
import kotlin.collections.HashMap

class BrowseAirportsFragment : ListFragmentBase() {

    private var mAdapter: SectionedCursorAdapter? = null
    private var mMode: Int = 0
    private var mListState: Parcelable? = null
    private var mStateCode: String? = null
    private var mStateName: String? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState != null && savedInstanceState.containsKey(BROWSE_MODE)) {
            mMode = savedInstanceState.getInt(BROWSE_MODE)
            if (mMode == BROWSE_AIRPORTS_MODE) {
                mStateCode = savedInstanceState.getString(Airports.ASSOC_STATE)
                mStateName = savedInstanceState.getString(States.STATE_NAME)
            }
        } else {
            mMode = BROWSE_STATE_MODE
        }

        if (mMode == BROWSE_STATE_MODE) {
            setBackgroundTask(BrowseStateTask(this)).execute()
        } else {
            setBackgroundTask(BrowseAirportsTask(this)).execute(mStateCode, mStateName)
        }

        view?.isFocusableInTouchMode = true
        view?.requestFocus()
        view?.setOnKeyListener { _, keyCode, event ->
            if (mMode == BROWSE_AIRPORTS_MODE
                    && keyCode == KeyEvent.KEYCODE_BACK
                    && event.action == KeyEvent.ACTION_UP) {
                // Intercept back key to go back to state mode
                mMode = BROWSE_STATE_MODE
                setAdapter(null)
                setBackgroundTask(BrowseStateTask(this)).execute()
                true
            } else {
                false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(BROWSE_MODE, mMode)
        if (mMode == BROWSE_AIRPORTS_MODE) {
            outState.putString(Airports.ASSOC_STATE, mStateCode)
            outState.putString(States.STATE_NAME, mStateName)
        }
    }

    override fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter? {
        mAdapter = if (mMode == BROWSE_STATE_MODE) {
            StateCursorAdapter(context, R.layout.browse_all_item, c)
        } else {
            CityCursorAdapter(context, c)
        }
        return mAdapter
    }

    override fun onListItemClick(l: ListView, v: View, position: Int) {
        val curPos = mAdapter!!.sectionedPositionToPosition(position)
        val adapter = listAdapter as CursorAdapter
        val c = adapter.cursor
        c.moveToPosition(curPos)
        if (mMode == BROWSE_STATE_MODE) {
            mMode = BROWSE_AIRPORTS_MODE
            mListState = listView?.onSaveInstanceState()
            setAdapter(null)
            mStateCode = c.getString(c.getColumnIndexOrThrow(Airports.ASSOC_STATE))
            mStateName = c.getString(c.getColumnIndexOrThrow(States.STATE_NAME))
            setBackgroundTask(BrowseAirportsTask(this)).execute(mStateCode, mStateName)
        } else {
            Intent(activity, AirportActivity::class.java).apply {
                putExtras(c.makeAirportBundle())
                startActivity(this)
            }
        }
    }

    private fun setStateCursor(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        setCursor(apt)
        if (mListState != null) {
            listView?.onRestoreInstanceState(mListState)
        }
    }

    private fun setAirportsCursor(result: Array<Cursor?>) {
        val apt = result[0] ?: return
        setCursor(apt)
    }

    private inner class StateCursorAdapter(context: Context?, layout: Int, c: Cursor?)
        : SectionedCursorAdapter(context, layout, c, R.layout.list_item_header) {

        override fun bindView(view: View, context: Context, c: Cursor) {
            // Browsing all states
            val stateName = c.getString(c.getColumnIndexOrThrow(States.STATE_NAME))
            val count = c.getInt(c.getColumnIndexOrThrow(BaseColumns._COUNT))
            val tv = view.findViewById<TextView>(R.id.browse_state_name)
            tv.text = String.format(Locale.US, "%s (%d)", stateName, count)
        }

        override fun getSectionName(): String {
            val c = cursor
            return c.getString(c.getColumnIndexOrThrow(States.STATE_NAME)).substring(0, 1)
        }
    }

    private inner class CityCursorAdapter(context: Context?, c: Cursor?)
        : SectionedCursorAdapter(context, R.layout.airport_list_item, c, R.layout.list_item_header) {

        var mAdapter: AirportsCursorAdapter = AirportsCursorAdapter(context, c)

        override fun bindView(view: View, context: Context, c: Cursor) {
            mAdapter.bindView(view, context, c)
        }

        override fun getSectionName(): String {
            val c = cursor
            return c.getString(c.getColumnIndexOrThrow(Airports.ASSOC_CITY))
        }
    }

    private class BrowseStateTask(fragment: BrowseAirportsFragment)
        : CursorAsyncTask<BrowseAirportsFragment>(fragment) {

        override fun onExecute(fragment: BrowseAirportsFragment, vararg params: String)
                : Array<Cursor?> {
            val db = fragment.getDatabase(DatabaseManager.DB_FADDS)

            // Show all the states grouped by first letter
            val builder = SQLiteQueryBuilder()
            builder.tables = ("${Airports.TABLE_NAME} a LEFT OUTER JOIN ${States.TABLE_NAME} s" +
                    " ON a.${Airports.ASSOC_STATE}=s.${States.STATE_CODE}")
            builder.projectionMap = sStateMap
            val c = builder.query(db,
                    arrayOf(
                            BaseColumns._ID,
                            Airports.ASSOC_STATE, States.
                            STATE_NAME, BaseColumns._COUNT
                    ),
                    null, null, States.STATE_NAME, null, States.STATE_NAME)

            return arrayOf(c)
        }

        override fun onResult(fragment: BrowseAirportsFragment, result: Array<Cursor?>)
                : Boolean {
            fragment.setStateCursor(result)
            return false
        }

    }

    private class BrowseAirportsTask(fragment: BrowseAirportsFragment)
        : CursorAsyncTask<BrowseAirportsFragment>(fragment) {

        override fun onExecute(fragment: BrowseAirportsFragment, vararg params: String)
                : Array<Cursor?> {
            val db = fragment.getDatabase(DatabaseManager.DB_FADDS)
            val stateCode = params[0]
            val stateName = params[1]

            val selection = ("(${Airports.ASSOC_STATE} <> '' AND ${Airports.ASSOC_STATE} = ?)"
                    + " OR (${Airports.ASSOC_STATE} = '' AND ${Airports.ASSOC_COUNTY} = ?)")
            val selectionArgs = arrayOf(stateCode, stateName)

            val c = AirportsCursorHelper.query(db, selection, selectionArgs, null, null,
                    "${Airports.ASSOC_CITY}, ${Airports.FACILITY_NAME}", null)

            return arrayOf(c)
        }

        override fun onResult(fragment: BrowseAirportsFragment, result: Array<Cursor?>)
                : Boolean {
            fragment.setAirportsCursor(result)
            return false
        }


    }

    companion object {

        // Projection map for queries
        private val sStateMap: HashMap<String, String> = HashMap()

        init {
            sStateMap[BaseColumns._ID] = "max(${BaseColumns._ID}) AS ${BaseColumns._ID}"
            sStateMap[Airports.ASSOC_STATE] = Airports.ASSOC_STATE
            sStateMap[States.STATE_NAME] = ("IFNULL(${States.STATE_NAME}, ${Airports.ASSOC_COUNTY})"
                    + " AS ${States.STATE_NAME}")
            sStateMap[BaseColumns._COUNT] = "count(*) AS ${BaseColumns._COUNT}"
        }

        private const val BROWSE_STATE_MODE = 0
        private const val BROWSE_AIRPORTS_MODE = 1

        private const val BROWSE_MODE = "BROWSE_MODE"
    }
}
