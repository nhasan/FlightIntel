/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2025 Nadeem Hasan <nhasan@nadmm.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nadmm.airports.library

import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.R
import com.nadmm.airports.TabPagerActivityBase
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.BookCategories
import com.nadmm.airports.utils.forEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryActivity : TabPagerActivityBase() {
    private var mPending = false
    private val mLock = Any()
    private val mColumns = arrayOf(
        BookCategories.CATEGORY_CODE,
        BookCategories.CATEGORY_NAME
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarTitle("Library", null)

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { doQuery() }
            populateTabs(result)
        }
    }

    override val selfNavDrawerItem: Int
        get() = R.id.navdrawer_library

    private fun populateTabs(c: Cursor) {
        c.forEach {
            val code = c.getString(mColumns.indexOf(BookCategories.CATEGORY_CODE))
            val name = c.getString(mColumns.indexOf(BookCategories.CATEGORY_NAME))
            val args = Bundle().apply {
                putString(BookCategories.CATEGORY_CODE, code)
            }
            addTab(name, LibraryPageFragment::class.java, args)
        }
    }

    var isPending: Boolean
        get() {
            synchronized(mLock) { return mPending }
        }
        set(pending) {
            synchronized(mLock) { mPending = pending }
        }

    private fun doQuery(): Cursor {
        val db = getDatabase(DatabaseManager.DB_LIBRARY)
        val builder = SQLiteQueryBuilder()
        builder.tables = BookCategories.TABLE_NAME
        return builder.query(
            db, mColumns, null, null, null, null,
            BookCategories._ID
        )
    }
}