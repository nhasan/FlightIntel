/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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
import androidx.core.os.BundleCompat
import androidx.cursoradapter.widget.CursorAdapter
import com.nadmm.airports.databinding.ListViewLayoutBinding
import kotlinx.parcelize.Parcelize

abstract class ListFragmentBase : FragmentBase() {

    private var listViewState: ListViewState? = null
    private var _binding: ListViewLayoutBinding? = null
    private val binding get() = _binding!!
    val listView: ListView
        get() = binding.list

    val listAdapter: ListAdapter?
        get() = listView.adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listViewState = savedInstanceState?.run {
            BundleCompat.getParcelable(this, LISTVIEW_STATE, ListViewState::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = ListViewLayoutBinding.inflate(inflater, container, false)
        return binding.list.run {
            setOnItemClickListener {
                    _, view, position, _ -> onListItemClick(this, view, position)
            }

            createContentView(binding.root)
        }
    }

    override fun onDestroy() {
        super.onDestroy() // Don't forget to call super!
        listView.adapter?.let { adapter ->
            if (adapter is CursorAdapter) {
                // The CursorAdapter's swapCursor(null) method will close the old cursor.
                // This is the recommended way to signal the adapter to release its cursor.
                adapter.swapCursor(null)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setFragmentContentShownNoAnimation(false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        listView.run {
            listViewState = ListViewState(onSaveInstanceState())
        }
    }

    protected open fun setCursor(c: Cursor) {
        val adapter = newListAdapter(activity, c)
        setAdapter(adapter)
    }

    fun setAdapter(adapter: ListAdapter?) {
        listView.adapter = adapter
        adapter?.run {
            listViewState?.let { state ->
                listView.onRestoreInstanceState(state.saveState)
            }
            setListShown(count > 0)
        }
        setFragmentContentShown(true)
    }

    fun setEmptyText(text: String) {
        binding.empty.text = text
    }

    protected fun setListShown(show: Boolean) {
        if (show) {
            binding.empty.visibility = View.GONE
            listView.visibility = View.VISIBLE
        } else {
            binding.empty.visibility = View.VISIBLE
            listView.visibility = View.GONE
        }
    }

    protected open fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter? {
        return null
    }

    protected abstract fun onListItemClick(l: ListView, v: View, position: Int)

    @Parcelize
    data class ListViewState(val saveState: Parcelable?) : Parcelable

    companion object {
        private const val LISTVIEW_STATE = "LISTVIEW_STATE"
    }
}
