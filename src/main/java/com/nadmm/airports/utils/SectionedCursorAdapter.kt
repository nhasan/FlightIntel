/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
 * Copyright 2012 Google Inc
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
package com.nadmm.airports.utils

import android.content.Context
import android.database.Cursor
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.cursoradapter.widget.ResourceCursorAdapter
import androidx.core.util.size

abstract class SectionedCursorAdapter(context: Context, layout: Int, c: Cursor?, private val mSectionResourceId: Int) :
    ResourceCursorAdapter(context, layout, c, 0) {
    private val mLayoutInflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val mSections = SparseArray<Section?>()

    class Section(var firstPosition: Int, var title: CharSequence?) {
        var sectionedPosition: Int = 0
    }

    init {
        setSections()
    }

    private fun setSections() {
        val c = cursor
        if (c.moveToFirst()) {
            var last = ""
            var offset = 0 // offset positions for the headers we're adding
            do {
                val cur = this.sectionName
                if (!cur.contentEquals(last)) {
                    val section = Section(c.getPosition(), cur)
                    section.sectionedPosition = section.firstPosition + offset
                    mSections.append(section.sectionedPosition, section)
                    ++offset
                    last = cur
                }
            } while (c.moveToNext())
        }
    }

    abstract val sectionName: String

    fun sectionedPositionToPosition(sectionedPosition: Int): Int {
        if (isSectionHeaderPosition(sectionedPosition)) {
            return ListView.INVALID_POSITION
        }

        var offset = 0
        for (i in 0..<mSections.size) {
            if (mSections.valueAt(i)!!.sectionedPosition > sectionedPosition) {
                break
            }
            --offset
        }
        return sectionedPosition + offset
    }

    private fun isSectionHeaderPosition(position: Int): Boolean {
        return mSections.get(position) != null
    }

    override fun getCount(): Int {
        return (if (cursor.count > 0) cursor.count + mSections.size else 0)
    }

    override fun getItem(position: Int): Any? {
        return if (isSectionHeaderPosition(position))
            mSections.get(position)
        else
            super.getItem(sectionedPositionToPosition(position))
    }

    override fun getItemId(position: Int): Long {
        return (if (isSectionHeaderPosition(position))
            Int.Companion.MAX_VALUE - mSections.indexOfKey(position)
        else
            sectionedPositionToPosition(position)).toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return if (isSectionHeaderPosition(position)) 0 else 1
    }

    override fun isEnabled(position: Int): Boolean {
        return !isSectionHeaderPosition(position)
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        var convertView = convertView
        if (isSectionHeaderPosition(position)) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mSectionResourceId, parent, false)
            }
            val tv = convertView as TextView
            tv.text = mSections.get(position)!!.title
        } else {
            convertView = super.getView(sectionedPositionToPosition(position), convertView, parent)
        }

        return convertView
    }

    override fun changeCursor(c: Cursor?) {
        super.changeCursor(c)
        setSections()
    }

    override fun onContentChanged() {
        super.onContentChanged()
        setSections()
    }
}
