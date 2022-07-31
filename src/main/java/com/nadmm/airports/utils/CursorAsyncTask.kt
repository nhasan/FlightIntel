/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

import android.database.Cursor
import android.os.AsyncTask

import com.nadmm.airports.FragmentBase

import java.lang.ref.WeakReference

abstract class CursorAsyncTask<T : FragmentBase>(fragment: T)
    : AsyncTask<String, Void, Array<Cursor?>>() {
    private val mFragment: WeakReference<T> = WeakReference(fragment)

    val fragment: T?
        get() = mFragment.get()

    override fun onPreExecute() {}

    override fun doInBackground(vararg params: String): Array<Cursor?>? {
        val fragment = mFragment.get()
        return if (fragment != null && fragment.activity != null) {
            onExecute(fragment, *params)
        } else null

    }

    override fun onPostExecute(result: Array<Cursor?>) {
        val fragment = mFragment.get()
        var close = true
        if (fragment != null && fragment.activity != null) {
            close = onResult(fragment, result)
        }

        if (close) {
            for (c in result) {
                c?.close()
            }
        }
    }

    protected abstract fun onExecute(fragment: T, vararg params: String): Array<Cursor?>
    protected abstract fun onResult(fragment: T, result: Array<Cursor?>): Boolean

}
