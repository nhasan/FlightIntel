/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2020 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.TextView

import androidx.core.content.ContextCompat
import androidx.cursoradapter.widget.CursorAdapter

abstract class ListFragmentBase : FragmentBase() {

    lateinit var listView: ListView
    private var mListViewState: Parcelable? = null

    val listAdapter: ListAdapter?
        get() = listView.adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState?.containsKey(LISTVIEW_STATE) == true) {
            mListViewState = savedInstanceState.getParcelable(LISTVIEW_STATE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.list_view_layout, container, false)
        listView = layout.findViewById(android.R.id.list) ?: return null
        listView.setOnItemClickListener {
            _, view, position, _ -> onListItemClick(listView, view, position)
        }
        listView.cacheColorHint = ContextCompat.getColor(activity!!, R.color.color_background)

        return createContentView(layout)
    }

    override fun onDestroy() {
        val adapter = listView.adapter
        if (adapter is CursorAdapter) {
            adapter.cursor.close()
        }

        super.onDestroy()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setFragmentContentShownNoAnimation(false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mListViewState = listView.onSaveInstanceState()
        outState.putParcelable(LISTVIEW_STATE, mListViewState)
    }

    protected open fun setCursor(c: Cursor?) {
        val adapter = newListAdapter(activity, c)
        setAdapter(adapter)
    }

    fun setAdapter(adapter: ListAdapter?) {
        listView.adapter = adapter

        if (adapter != null) {
            setListShown(adapter.count > 0)

            if (mListViewState != null) {
                listView.onRestoreInstanceState(mListViewState)
                mListViewState = null
            }
        } else {
            setListShown(false)
        }

        setFragmentContentShown(true)
    }

    fun setEmptyText(text: String) {
        val tv = findViewById<TextView>(android.R.id.empty)
        tv!!.text = text
    }

    protected fun setListShown(show: Boolean) {
        val tv = findViewById<TextView>(android.R.id.empty)
        if (show) {
            tv!!.visibility = View.GONE
            listView.visibility = View.VISIBLE
        } else {
            tv!!.visibility = View.VISIBLE
            listView.visibility = View.GONE
        }
    }

    protected open fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter? {
        return null
    }

    protected abstract fun onListItemClick(l: ListView, v: View, position: Int)

    companion object {
        private const val LISTVIEW_STATE = "LISTVIEW_STATE"
    }

}
