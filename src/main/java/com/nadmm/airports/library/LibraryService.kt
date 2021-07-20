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

import android.app.IntentService
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.UiUtils
import java.io.File
import java.util.*
import java.util.zip.GZIPInputStream

class LibraryService : IntentService(SERVICE_NAME) {
    private lateinit var mDataDir: File

    override fun onCreate() {
        super.onCreate()
        mDataDir = SystemUtils.getExternalDir(this, SERVICE_NAME)
    }

    override fun onHandleIntent(intent: Intent?) {
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

    private fun checkBooks(intent: Intent) {
        intent.action?.let { action ->
            intent.getStringExtra(CATEGORY)?.let { category ->
                intent.getStringArrayListExtra(BOOK_NAMES)?.let { books ->
                    cleanupBooks(category, books)
                    val categoryDir = getCategoryDir(category)
                    for (book in books) {
                        val pdfFile = File(categoryDir, book)
                        sendResult(action, category, pdfFile)
                    }
                }
            }
        }
    }

    private fun getBook(intent: Intent) {
        intent.action?.let { action ->
            intent.getStringExtra(CATEGORY)?.let { category ->
                intent.getStringExtra(BOOK_NAME)?.let { book ->
                    val categoryDir = getCategoryDir(category)
                    val pdfFile = File(categoryDir, book)
                    if (!pdfFile.exists()) {
                        fetch(category, pdfFile)
                    }
                    sendResult(action, category, pdfFile)
                }
            }
        }
    }

    private fun deleteBook(intent: Intent) {
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
            val receiver = ProgressReceiver()
            val result = Bundle()
            result.putString(NetworkUtils.CONTENT_NAME, pdfFile.name)
            result.putString(CATEGORY, category)
            val path = LIBRARY_PATH + "/" + category + "/" + pdfFile.name + ".gz"
            return NetworkUtils.doHttpsGet(
                this, LIBRARY_HOST, path, null,
                pdfFile, receiver, result, GZIPInputStream::class.java
            )
        } catch (e: Exception) {
            UiUtils.showToast(this, e.message)
        }
        return false
    }

    private fun sendResult(action: String, category: String, pdfFile: File) {
        val result = Intent(action)
        result.putExtra(CATEGORY, category)
        result.putExtra(BOOK_NAME, pdfFile.name)
        if (pdfFile.exists()) {
            result.putExtra(PDF_PATH, pdfFile.absolutePath)
        }
        val bm = LocalBroadcastManager.getInstance(this)
        bm.sendBroadcast(result)
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

    private fun handleProgress(resultData: Bundle) {
        val intent = Intent(ACTION_DOWNLOAD_PROGRESS)
        intent.putExtras(resultData)
        val bm = LocalBroadcastManager.getInstance(this)
        bm.sendBroadcast(intent)
    }

    private inner class ProgressReceiver : ResultReceiver(null) {
        override fun send(resultCode: Int, resultData: Bundle) {
            // We want to handle the result in the same thread synchronously
            handleProgress(resultData)
        }
    }

    companion object {
        private const val SERVICE_NAME = "library"
        const val LIBRARY_HOST = "commondatastorage.googleapis.com"
        const val LIBRARY_PATH = "/flightintel/library"
        const val ACTION_GET_BOOK = "flightintel.library.action.GET_BOOK"
        const val ACTION_DELETE_BOOK = "flightintel.library.action.DELETE_BOOK"
        const val ACTION_CHECK_BOOKS = "flightintel.library.action.CHECK_BOOKS"
        const val ACTION_DOWNLOAD_PROGRESS = "flightintel.library.action.PROGRESS"
        const val CATEGORY = "CATEGORY"
        const val BOOK_NAME = "BOOK_NAME"
        const val BOOK_NAMES = "BOOK_NAMES"
        const val PDF_PATH = "PDF_PATH"
    }
}