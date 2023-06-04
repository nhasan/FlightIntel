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
package com.nadmm.airports

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.ResourceCursorAdapter
import com.nadmm.airports.utils.UiUtils

abstract class ListMenuFragment : ListFragmentBase() {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        arguments?.apply {
            supportActionBar?.subtitle = this.getString(SUBTITLE_TEXT)
            val id = this.getInt(MENU_ID)
            val c = getMenuCursor(id)
            setCursor(c)
        }
    }

    override fun newListAdapter(context: Context?, c: Cursor?): CursorAdapter? {
        return ListMenuAdapter(context, c)
    }

    override fun onListItemClick(l: ListView, v: View, position: Int) {
        val c = listAdapter!!.getItem(position) as Cursor
        val id = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID))
        val title = c.getString(c.getColumnIndexOrThrow(ListMenuCursor.ITEM_TITLE))
        val clss = getItemFragmentClass(id)
        if (clss != null) {
            val args = Bundle()
            args.putString(ActivityBase.FRAGMENT_TAG_EXTRA, id.toString())
            args.putString(SUBTITLE_TEXT, title)
            args.putInt(MENU_ID, id)
            activityBase.replaceFragment(clss, args)
        }
    }

    protected abstract fun getItemFragmentClass(id: Int): Class<*>?
    protected abstract fun getMenuCursor(id: Int): Cursor

    private class ListMenuAdapter(context: Context?, c: Cursor?) :
        ResourceCursorAdapter(context, R.layout.list_menu_item, c, 0) {
        override fun bindView(view: View, context: Context, c: Cursor) {
            val icon = c.getInt(c.getColumnIndexOrThrow(ListMenuCursor.ITEM_ICON))
            val title = c.getString(c.getColumnIndexOrThrow(ListMenuCursor.ITEM_TITLE))
            val summary = c.getString(c.getColumnIndexOrThrow(ListMenuCursor.ITEM_SUMMARY))
            val iv = view.findViewById<ImageView>(R.id.item_icon)
            if (icon != 0) {
                iv.setImageDrawable(UiUtils.getDefaultTintedDrawable(context, icon))
            } else {
                iv.visibility = View.GONE
            }
            var tv = view.findViewById<TextView>(R.id.item_title)
            tv.text = title
            tv = view.findViewById(R.id.item_summary)
            tv.text = summary
        }
    }

    abstract class ListMenuCursor(id: Int) : MatrixCursor(sColumnNames) {
        protected abstract fun populateMenuItems(id: Int)

        companion object {
            const val ITEM_ICON = "ITEM_ICON"
            const val ITEM_TITLE = "ITEM_TITLE"
            const val ITEM_SUMMARY = "ITEM_SUMMARY"
            private val sColumnNames = arrayOf(BaseColumns._ID, ITEM_ICON, ITEM_TITLE, ITEM_SUMMARY)
        }

        init {
            this.populateMenuItems(id)
        }
    }

    companion object {
        const val MENU_ID = "MENU_ID"
        const val SUBTITLE_TEXT = "SUBTITLE_TEXT"
    }
}