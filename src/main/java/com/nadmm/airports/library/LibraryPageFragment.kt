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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.nadmm.airports.library

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteQueryBuilder
import android.os.Bundle
import android.text.format.Formatter
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isNotEmpty
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Library
import com.nadmm.airports.databinding.LibraryDetailSectionBinding
import com.nadmm.airports.databinding.LibraryDetailViewBinding
import com.nadmm.airports.databinding.LibraryRowItemBinding
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.UiUtils
import com.nadmm.airports.utils.forEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryPageFragment : FragmentBase() {
    private var mIsOk = false
    private lateinit var mCategory: String
    private var mOnClickListener: View.OnClickListener? = null
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
    private var _binding: LibraryDetailViewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        mCategory = arguments?.getString(Library.CATEGORY_CODE)
            ?: throw IllegalArgumentException("Category code is required")
        mOnClickListener = View.OnClickListener { v: View ->
            val activity = requireActivity() as LibraryActivity
            if (!activity.isPending) {
                val path = v.getTag(R.id.LIBRARY_PDF_PATH) as String?
                if (path == null) {
                    if (mIsOk) {
                        activity.isPending = true
                        val row = LibraryRowItemBinding.bind(v)
                        row.progress.visibility = View.VISIBLE
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = LibraryDetailViewBinding.inflate(inflater, container, false)
        return createContentView(binding.root)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                doQuery(mCategory)
            }
            showBooks(result)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                LibraryService.Events.events.collect { result ->
                    val action = result.getString(LibraryService.ACTION) ?: ""
                    if (action == LibraryService.ACTION_DOWNLOAD_PROGRESS) {
                        handleProgress(result)
                    } else {
                        handleBook(result)
                    }
                }
            }
        }
    }

    private fun doQuery(category: String): Array<Cursor?> {
        val db = getDatabase(DatabaseManager.DB_LIBRARY)
        val result = ArrayList<MatrixCursor>()

        val builder = SQLiteQueryBuilder()
        builder.tables = Library.TABLE_NAME
        val c = builder.query(db, mColumns,
            Library.CATEGORY_CODE + "=?", arrayOf(category),
            null, null, Library._ID)
        var prevDesc = ""
        c.forEach {
            val name = c.getString(mColumns.indexOf(Library.BOOK_NAME))
            val desc = c.getString(mColumns.indexOf(Library.BOOK_DESC))
            val edition = c.getString(mColumns.indexOf(Library.EDITION))
            val author = c.getString(mColumns.indexOf(Library.AUTHOR))
            val size = c.getLong(mColumns.indexOf(Library.DOWNLOAD_SIZE))
            val flag = c.getString(mColumns.indexOf(Library.FLAG))
            if (desc != prevDesc) {
                result.add(MatrixCursor(mColumns))
                prevDesc = desc
            }
            if (result.isNotEmpty()) {
                val values = arrayOf<Any?>(name, desc, edition, author, size, flag)
                result.last().addRow(values)
            }
        }
        return result.toTypedArray()
    }

    private fun showBooks(result: Array<Cursor?>) {
        if (activity == null) {
            return
        }
        val msg: String
        mIsOk = when {
            !NetworkUtils.isNetworkAvailable(activityBase) -> {
                msg = "Not connected to the internet"
                false
            }
            NetworkUtils.canDownloadData(activityBase) -> {
                msg = "Connected to an unmetered network"
                true
            }
            else -> {
                msg = "Connected to a metered network"
                false
            }
        }

        binding.msgTxt.text = msg
        UiUtils.setTextViewDrawable(
            binding.msgTxt,
            if (mIsOk) R.drawable.ic_outline_check_circle_24 else R.drawable.ic_outline_cancel_24
        )
        val topLayout = binding.mainContent
        for (c in result) {
            c?.let {
                if (c.moveToFirst()) {
                    val section = LibraryDetailSectionBinding.inflate(layoutInflater, topLayout, false)
                    topLayout.addView(section.root)
                    do {
                        val name = c.getString(mColumns.indexOf(Library.BOOK_NAME))
                        val desc = c.getString(mColumns.indexOf(Library.BOOK_DESC))
                        val edition = c.getString(mColumns.indexOf(Library.EDITION))
                        val author = c.getString(mColumns.indexOf(Library.AUTHOR))
                        val size = c.getLong(mColumns.indexOf(Library.DOWNLOAD_SIZE))
                        val flag = c.getString(mColumns.indexOf(Library.FLAG))
                        addLibraryRow(section.root, name, desc, edition, author, flag, size)
                    } while (c.moveToNext())
                }
                c.close()
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
        if (layout.isNotEmpty()) {
            addSeparator(layout)
        }
        val row = LibraryRowItemBinding.inflate(layoutInflater)
        with (row) {
            bookDesc.text = desc
            bookEdition.text = edition
            if (flag != null && flag == "N") {
                UiUtils.setTextViewDrawable(bookEdition, R.drawable.ic_outline_new_releases_24)
            }
            bookAuthor.text = author
            bookSize.text = Formatter.formatShortFileSize(activity, size)
            root.setTag(R.id.LIBRARY_PDF_NAME, name)
            root.setOnClickListener(mOnClickListener)
            activity?.let {
                root.setBackgroundResource(UiUtils.getSelectableItemBackgroundResource(it))
            }
            showStatus(root, false)
            mBookRowMap[name] = root
            layout.addView(
                root, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    private fun handleBook(result: Bundle) {
        val pdfName = result.getString(LibraryService.BOOK_NAME)
        mBookRowMap[pdfName]?.let {
            val path = result.getString(LibraryService.PDF_PATH)
            showStatus(it, path != null)
            it.setTag(R.id.LIBRARY_PDF_PATH, path)
            if (path != null) {
                registerForContextMenu(it)
            } else {
                unregisterForContextMenu(it)
            }
            val action = result.getString(LibraryService.ACTION)
            if (action == LibraryService.ACTION_GET_BOOK) {
                SystemUtils.startPDFViewer(activity, path)
            }
        }
    }

    private fun handleProgress(result: Bundle) {
        val name = result.getString(NetworkUtils.CONTENT_NAME)
        mBookRowMap[name]?.let {
            with(LibraryRowItemBinding.bind(it)) {
                progress.visibility = View.VISIBLE
                val length = result.getInt(NetworkUtils.CONTENT_LENGTH, 0)
                progress.isIndeterminate = (length == 0)
                if (progress.max != length) {
                    progress.max = length
                }
                val current = result.getInt(NetworkUtils.CONTENT_PROGRESS, 0)
                progress.progress = current
                if (current == length) {
                    progress.visibility = View.GONE
                    (activity as LibraryActivity).isPending = false
                }
            }
        }
    }

    private fun showStatus(row: View, isAvailable: Boolean) {
        with (LibraryRowItemBinding.bind(row)) {
            if (isAvailable) {
                UiUtils.setTextViewDrawable(bookDesc, R.drawable.ic_outline_check_box_24)
            } else {
                UiUtils.setTextViewDrawable(bookDesc, R.drawable.ic_outline_check_box_outline_blank_24)
            }
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