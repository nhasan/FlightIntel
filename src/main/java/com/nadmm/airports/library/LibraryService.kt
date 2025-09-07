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

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import androidx.core.os.bundleOf
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.GZIPInputStream

class LibraryService : Service() {
    private val serviceJob = SupervisorJob() // Use SupervisorJob for better error handling in children
    // Coroutine scope tied to Dispatchers.IO for background work
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var mDataDir: File

    override fun onCreate() {
        super.onCreate()
        mDataDir = SystemUtils.getExternalDir(this, "library")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            intent?.let {
                when (intent.action) {
                    ACTION_CHECK_BOOKS -> {
                        checkBooks(intent)
                    }

                    ACTION_GET_BOOK -> {
                        getBook(intent)
                    }

                    ACTION_DELETE_BOOK -> {
                        deleteBook(intent)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder?  = null

    private suspend fun checkBooks(intent: Intent) {
        intent.getStringExtra(CATEGORY)?.let { category ->
            intent.getStringArrayListExtra(BOOK_NAMES)?.let { books ->
                cleanupBooks(category, books)
                val categoryDir = getCategoryDir(category)
                for (book in books) {
                    val pdfFile = File(categoryDir, book)
                    sendResult(intent.action!!, category, pdfFile)
                }
            }
        }
    }

    private suspend fun getBook(intent: Intent) {
        intent.getStringExtra(CATEGORY)?.let { category ->
            intent.getStringExtra(BOOK_NAME)?.let { book ->
                val categoryDir = getCategoryDir(category)
                val pdfFile = File(categoryDir, book)
                if (!pdfFile.exists()) {
                    fetch(category, pdfFile)
                }
                sendResult(intent.action!!, category, pdfFile)
            }
        }
    }

    private suspend fun deleteBook(intent: Intent) {
        intent.getStringExtra(CATEGORY)?.let { category ->
            intent.getStringExtra(BOOK_NAME)?.let { book ->
                val categoryDir = getCategoryDir(category)
                val pdfFile = File(categoryDir, book)
                if (pdfFile.exists()) {
                    pdfFile.delete()
                }
                sendResult(ACTION_CHECK_BOOKS, category, pdfFile)
            }
        }
    }

    private fun fetch(category: String, pdfFile: File): Boolean {
        try {
            val result = bundleOf(
                ACTION to ACTION_DOWNLOAD_PROGRESS,
                NetworkUtils.CONTENT_NAME to pdfFile.name,
            )
            val path = LIBRARY_PATH + "/" + category + "/" + pdfFile.name + ".gz"
            return NetworkUtils.doHttpsGet1(
                this, LIBRARY_HOST, path, null,
                pdfFile, GZIPInputStream::class.java, ::progress, result
            )
        } catch (e: Exception) {
            UiUtils.showToast(this, e.message)
        }
        return false
    }

    private fun progress(result: Bundle) {
        serviceScope.launch {
            Events.post(result)
        }
    }

    private suspend fun sendResult(action: String, category: String, pdfFile: File) {
        val result = bundleOf(
            ACTION to action,
            CATEGORY to category,
            BOOK_NAME to pdfFile.name
        )
        if (pdfFile.exists()) {
            result.putString(PDF_PATH, pdfFile.absolutePath)
        }
        Events.post(result)
    }

    private fun cleanupBooks(category: String, books: ArrayList<String>) {
        // Delete all books that are no longer in the library list for a category
        val categoryDir = getCategoryDir(category)
        val list = categoryDir.listFiles()
        if (list != null) {
            for (pdfFile in list) {
                if (!books.contains(pdfFile.name) || pdfFile.length() == 0L) {
                    pdfFile.delete()
                }
            }
        }
    }

    private fun getCategoryDir(category: String): File {
        val categoryDir = File(mDataDir, category)
        if (!categoryDir.exists()) {
            categoryDir.mkdirs()
        }
        return categoryDir
    }

    object Events {
        private val _events = MutableSharedFlow<Bundle>()
        val events = _events.asSharedFlow()

        suspend fun post(bundle: Bundle) {
            _events.emit(bundle)
        }
    }

    companion object {
        const val LIBRARY_HOST = "commondatastorage.googleapis.com"
        const val LIBRARY_PATH = "/flightintel/library"
        const val ACTION_GET_BOOK = "flightintel.library.action.GET_BOOK"
        const val ACTION_DELETE_BOOK = "flightintel.library.action.DELETE_BOOK"
        const val ACTION_CHECK_BOOKS = "flightintel.library.action.CHECK_BOOKS"
        const val ACTION_DOWNLOAD_PROGRESS = "flightintel.library.action.PROGRESS"
        const val ACTION = "ACTION"
        const val CATEGORY = "CATEGORY"
        const val BOOK_NAME = "BOOK_NAME"
        const val BOOK_NAMES = "BOOK_NAMES"
        const val PDF_PATH = "PDF_PATH"
    }
}