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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nadmm.airports.library

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import android.text.format.Formatter
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.nadmm.airports.Application
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Library
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class LibraryPageFragment : FragmentBase() {
    private var mIsOk = false
    private lateinit var mReceiver: BroadcastReceiver
    private var mOnClickListener: View.OnClickListener? = null
    private var mCategory: String? = null
    private var mContextMenuRow: View? = null
    private val mBookRowMap = HashMap<String, View>()
    private val mColumns = arrayOf(
        Library.BOOK_NAME,
        Library.BOOK_DESC,
        Library.EDITION,
        Library.AUTHOR,
        Library.DOWNLOAD_SIZE,
        Library.FLAG
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val args = arguments
        mCategory = args?.getString(Library.CATEGORY_CODE)
        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action ?: ""
                if (action == LibraryService.ACTION_DOWNLOAD_PROGRESS) {
                    handleProgress(intent)
                } else {
                    handleBook(intent)
                }
            }
        }
        mOnClickListener = View.OnClickListener { v: View ->
            val activity = requireActivity() as LibraryActivity
            if (!activity.isPending) {
                val path = v.getTag(R.id.LIBRARY_PDF_PATH) as String?
                if (path == null) {
                    if (mIsOk) {
                        activity.isPending = true
                        val progressBar = v.findViewById<ProgressBar>(R.id.progress)
                        progressBar.visibility = View.VISIBLE
                        val name = v.getTag(R.id.LIBRARY_PDF_NAME) as String
                        getBook(name)
                    } else {
                        UiUtils.showToast(activity, "Cannot start download")
                    }
                } else {
                    SystemUtils.startPDFViewer(activity, path)
                }
            } else {
                UiUtils.showToast(
                    activity, "Please wait, another download is in progress",
                    Toast.LENGTH_SHORT
                )
            }
        }
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        val activity = requireActivity() as LibraryActivity
        mCategory?.let { activity.registerReceiver(it, mReceiver) }
        super.onResume()
    }

    override fun onPause() {
        val activity = requireActivity() as LibraryActivity
        mCategory?.let { activity.unregisterReceiver(it) }
        super.onPause()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflate<View>(R.layout.library_detail_view, container!!)
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mCategory?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    doQuery(it)
                }
                showBooks(result)
            }
        }
    }

    private fun doQuery(category: String): Array<Cursor?> {
        val db = getDatabase(DatabaseManager.DB_LIBRARY)
        var builder = SQLiteQueryBuilder()
        builder.tables = Library.TABLE_NAME
        var c = builder.query(
            db, arrayOf("DISTINCT " + Library.BOOK_DESC),
            Library.CATEGORY_CODE + "=?", arrayOf(category),
            null, null, null
        )
        val result = arrayOfNulls<Cursor>(c.count)
        if (c.count > 0) {
            builder = SQLiteQueryBuilder()
            builder.tables = Library.TABLE_NAME
            c = builder.query(
                db, mColumns,
                Library.CATEGORY_CODE + "=?", arrayOf(category),
                null, null, Library._ID
            )
            c.moveToFirst()
            var i = 0
            var prevDesc = ""
            var matrix: MatrixCursor? = null
            do {
                val name = c.getString(c.getColumnIndex(Library.BOOK_NAME))
                val desc = c.getString(c.getColumnIndex(Library.BOOK_DESC))
                val edition = c.getString(c.getColumnIndex(Library.EDITION))
                val author = c.getString(c.getColumnIndex(Library.AUTHOR))
                val size = c.getLong(c.getColumnIndex(Library.DOWNLOAD_SIZE))
                val flag = c.getString(c.getColumnIndex(Library.FLAG))
                if (desc != prevDesc) {
                    matrix = MatrixCursor(mColumns)
                    result[i++] = matrix
                    prevDesc = desc
                }
                if (matrix != null) {
                    val values = arrayOf<Any?>(name, desc, edition, author, size, flag)
                    matrix.addRow(values)
                }
            } while (c.moveToNext())
        }
        return result
    }

    private fun showBooks(result: Array<Cursor?>) {
        if (activity == null) {
            return
        }
        val msg: String
        if (!Application.sDonationDone) {
            msg = "This function is only available after a donation"
            mIsOk = false
        } else if (!NetworkUtils.isNetworkAvailable(activity)) {
            msg = "Not connected to the internet"
            mIsOk = false
        } else if (NetworkUtils.canDownloadData(activityBase)) {
            msg = "Connected to an unmetered network"
            mIsOk = true
        } else {
            msg = "Connected to a metered network"
            mIsOk = false
        }
        val tv = findViewById<TextView>(R.id.msg_txt)
        tv!!.text = msg
        UiUtils.setTextViewDrawable(
            tv,
            if (mIsOk) R.drawable.ic_check else R.drawable.ic_highlight_remove
        )
        val topLayout = findViewById<LinearLayout>(R.id.main_content)
        for (c in result) {
            if (c!!.moveToFirst()) {
                val layout = inflate<LinearLayout>(
                    R.layout.library_detail_section,
                    topLayout!!
                )
                topLayout.addView(layout)
                do {
                    val name = c.getString(c.getColumnIndex(Library.BOOK_NAME))
                    val desc = c.getString(c.getColumnIndex(Library.BOOK_DESC))
                    val edition = c.getString(c.getColumnIndex(Library.EDITION))
                    val author = c.getString(c.getColumnIndex(Library.AUTHOR))
                    val size = c.getLong(c.getColumnIndex(Library.DOWNLOAD_SIZE))
                    val flag = c.getString(c.getColumnIndex(Library.FLAG))
                    addLibraryRow(layout, name, desc, edition, author, flag, size)
                } while (c.moveToNext())
            }
        }
        setFragmentContentShown(true)
        checkBooks()
    }

    @SuppressLint("InlinedApi")
    private fun addLibraryRow(
        layout: LinearLayout, name: String, desc: String, edition: String,
        author: String, flag: String?, size: Long
    ) {
        if (layout.childCount > 0) {
            addSeparator(layout)
        }
        val row = inflate<RelativeLayout>(R.layout.library_row_item)
        var tv = row.findViewById<TextView>(R.id.book_desc)
        tv.text = desc
        tv = row.findViewById(R.id.book_edition)
        tv.text = edition
        if (flag != null && flag == "N") {
            UiUtils.setTextViewDrawable(tv, R.drawable.star)
        }
        tv = row.findViewById(R.id.book_author)
        tv.text = author
        tv = row.findViewById(R.id.book_size)
        tv.text = Formatter.formatShortFileSize(activity, size)
        row.setTag(R.id.LIBRARY_PDF_NAME, name)
        row.setOnClickListener(mOnClickListener)
        val background = UiUtils.getSelectableItemBackgroundResource(activity)
        row.setBackgroundResource(background)
        showStatus(row, false)
        mBookRowMap[name] = row
        layout.addView(
            row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun handleBook(intent: Intent) {
        val pdfName = intent.getStringExtra(LibraryService.BOOK_NAME)
        mBookRowMap[pdfName]?.let {
            val path = intent.getStringExtra(LibraryService.PDF_PATH)
            showStatus(it, path != null)
            it.setTag(R.id.LIBRARY_PDF_PATH, path)
            if (path != null) {
                registerForContextMenu(it)
            } else {
                unregisterForContextMenu(it)
            }
            // Hide the progressbar
            val progressBar = it.findViewById<ProgressBar>(R.id.progress)
            progressBar.visibility = View.GONE
        }
    }

    private fun handleProgress(intent: Intent) {
        val name = intent.getStringExtra(NetworkUtils.CONTENT_NAME)
        mBookRowMap[name]?.let {
            val progressBar = it.findViewById<ProgressBar>(R.id.progress)
            val length = intent.getLongExtra(NetworkUtils.CONTENT_LENGTH, 0)
            if (!progressBar.isShown) {
                progressBar.visibility = View.VISIBLE
            }
            progressBar.isIndeterminate = (length == 0L)
            if (progressBar.max.toLong() != length) {
                progressBar.max = length.toInt()
            }
            val progress = intent.getLongExtra(NetworkUtils.CONTENT_PROGRESS, 0)
            progressBar.progress = progress.toInt()
        }
    }

    private fun showStatus(row: View, isAvailable: Boolean) {
        val tv = row.findViewById<TextView>(R.id.book_desc)
        if (isAvailable) {
            UiUtils.setTextViewDrawable(tv, R.drawable.ic_check_box)
        } else {
            UiUtils.setTextViewDrawable(tv, R.drawable.ic_check_box_outline_blank)
        }
    }

    private fun getBook(name: String) {
        val service = makeServiceIntent(LibraryService.ACTION_GET_BOOK)
        service.putExtra(LibraryService.BOOK_NAME, name)
        requireActivity().startService(service)
    }

    private fun deleteBook(name: String) {
        val service = makeServiceIntent(LibraryService.ACTION_DELETE_BOOK)
        service.putExtra(LibraryService.BOOK_NAME, name)
        requireActivity().startService(service)
    }

    private fun checkBooks() {
        val service = makeServiceIntent(LibraryService.ACTION_CHECK_BOOKS)
        val books = ArrayList(mBookRowMap.keys)
        service.putExtra(LibraryService.BOOK_NAMES, books)
        requireActivity().startService(service)
    }

    private fun makeServiceIntent(action: String): Intent {
        val service = Intent(activity, LibraryService::class.java)
        service.action = action
        service.putExtra(LibraryService.CATEGORY, mCategory)
        return service
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        if (activity != null) {
            val inflater = requireActivity().menuInflater
            inflater.inflate(R.menu.library_context_menu, menu)
            mContextMenuRow = v
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_delete -> if (mContextMenuRow != null) {
                val name = mContextMenuRow!!.getTag(R.id.LIBRARY_PDF_NAME) as String
                deleteBook(name)
            }
            else -> {
            }
        }
        return super.onContextItemSelected(item)
    }
}