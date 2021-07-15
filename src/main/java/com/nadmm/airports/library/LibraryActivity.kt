/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2021 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.R
import com.nadmm.airports.TabPagerActivityBase
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.forEach
import kotlinx.coroutines.*
import java.util.*

class LibraryActivity : TabPagerActivityBase() {
    private var mPending = false
    private val mLock = Any()
    private lateinit var mReceivers: HashMap<String, BroadcastReceiver>
    private lateinit var mReceiver: BroadcastReceiver
    private lateinit var mFilter: IntentFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActionBarTitle("Library", null)
        mReceivers = HashMap()
        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val category = intent.getStringExtra(LibraryService.CATEGORY)
                val receiver = mReceivers[category]
                receiver?.onReceive(context, intent)

                // Show the PDF here as the Fragment requesting it may be paused
                val action = intent.action
                if (action != null && action == LibraryService.ACTION_GET_BOOK) {
                    val path = intent.getStringExtra(LibraryService.PDF_PATH)
                    isPending = false
                    if (path != null) {
                        SystemUtils.startPDFViewer(this@LibraryActivity, path)
                    }
                }
            }
        }
        mFilter = IntentFilter()
        mFilter.priority = 10
        mFilter.addAction(LibraryService.ACTION_CHECK_BOOKS)
        mFilter.addAction(LibraryService.ACTION_GET_BOOK)
        mFilter.addAction(LibraryService.ACTION_DOWNLOAD_PROGRESS)

        CoroutineScope(Dispatchers.IO).launch {
            val result = doQuery()
            populateTabs(result)
        }
    }

    override fun onResume() {
        val bm = LocalBroadcastManager.getInstance(this)
        bm.registerReceiver(mReceiver, mFilter)
        super.onResume()
    }

    override fun onPause() {
        val bm = LocalBroadcastManager.getInstance(this)
        bm.unregisterReceiver(mReceiver)
        super.onPause()
    }

    override val selfNavDrawerItem: Int
        get() = R.id.navdrawer_library

    private fun populateTabs(c: Cursor) {
        c.forEach {
            val code = c.getString(c.getColumnIndex(DatabaseManager.BookCategories.CATEGORY_CODE))
            val name = c.getString(c.getColumnIndex(DatabaseManager.BookCategories.CATEGORY_NAME))
            val args = Bundle()
            args.putString(DatabaseManager.BookCategories.CATEGORY_CODE, code)
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

    fun registerReceiver(category: String, receiver: BroadcastReceiver) {
        mReceivers[category] = receiver
    }

    fun unregisterReceiver(category: String) {
        mReceivers.remove(category)
    }

    private fun doQuery(): Cursor {
        val db = getDatabase(DatabaseManager.DB_LIBRARY)
        val builder = SQLiteQueryBuilder()
        builder.tables = DatabaseManager.BookCategories.TABLE_NAME
        return builder.query(
            db, arrayOf("*"), null, null, null, null,
            DatabaseManager.BookCategories._ID
        )
    }
}