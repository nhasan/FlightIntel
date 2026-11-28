/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2026 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.provider.BaseColumns
import android.sax.RootElement
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import android.util.Xml
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nadmm.airports.FragmentBase
import com.nadmm.airports.R
import com.nadmm.airports.utils.ExternalStorageActivity
import com.nadmm.airports.utils.NetworkUtils
import com.nadmm.airports.utils.SectionedCursorAdapter
import com.nadmm.airports.utils.SystemUtils
import com.nadmm.airports.utils.TimeUtils
import com.nadmm.airports.utils.UiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream

/**
 * Lists the installed data and the data available to download, and
 * downloads, installs and deletes it. The work runs in coroutines off the
 * main thread, with download progress shown on each file's row.
 */
class DownloadFragment : FragmentBase() {
    private lateinit var listView: ListView
    // Progress rows by data type, made as the list draws the available data
    private val trackers = HashMap<String, ProgressTracker>()
    private val installedData = ArrayList<DataInfo>()
    private val availableData = ArrayList<DataInfo>()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** One data file, installed or available, by type and version. */
    private class DataInfo : Comparable<DataInfo> {
        var type = ""
        var desc = ""
        var version = 0
        var fileName = ""
        var size = 0
        var start = Date(0)
        var end = Date(0)

        fun copy() = DataInfo().also {
            it.type = type; it.desc = desc; it.version = version; it.fileName = fileName
            it.size = size; it.start = start; it.end = end
        }

        override fun equals(other: Any?) =
            other is DataInfo && type == other.type && version == other.version

        override fun hashCode() = 31 * type.hashCode() + version

        override fun compareTo(other: DataInfo): Int =
            if (type != other.type) type.compareTo(other.type) else start.compareTo(other.start)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.download_list_view, container, false)
        listView = view.findViewById(android.R.id.list)
        listView.addFooterView(inflate(R.layout.download_footer))
        listView.setFooterDividersEnabled(true)
        view.findViewById<Button>(R.id.btnDownload).setOnClickListener { checkNetworkAndDownload() }
        view.findViewById<Button>(R.id.btnDelete).setOnClickListener { checkDelete() }
        return createContentView(view)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkData()
    }

    private fun checkNetworkAndDownload() {
        NetworkUtils.checkNetworkAndDownload(requireActivity()) { download() }
    }

    private fun download() {
        if (!SystemUtils.isExternalStorageAvailable) {
            startActivity(Intent(activity, ExternalStorageActivity::class.java))
            requireActivity().finish()
            return
        }
        viewLifecycleOwner.lifecycleScope.launch { downloadAll() }
    }

    /**
     * Downloads and installs each available file in turn, refreshing the list
     * after each, then removes expired data and reopens the databases.
     */
    private suspend fun downloadAll() {
        while (true) {
            val data = availableData.firstOrNull() ?: break
            // The row's tracker is looked up as progress comes, as the list
            // makes it only once it draws the row
            mainHandler.post { trackers[data.type]?.initProgress(data.size) }
            val ok = withContext(Dispatchers.IO) { downloadData(data) && updateCatalog(data) }
            trackers[data.type]?.hideProgress()
            if (!ok) {
                updateDownloadList()
                return
            }
            installedData.add(data)
            availableData.remove(data)
            updateDownloadList()
        }
        withContext(Dispatchers.IO) {
            cleanupExpiredData()
            dbManager.closeDatabases()
            dbManager.openDatabases()
        }
        UiUtils.showToast(requireContext(), "Data installation completed successfully")
        updateDownloadList()
    }

    private fun downloadData(data: DataInfo): Boolean {
        val context = context ?: return false
        return try {
            val dbFile = dbManager.getDatabaseFile(data.fileName)
            val receiver = object : ResultReceiver(mainHandler) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                    val progress = resultData.getLong(NetworkUtils.CONTENT_PROGRESS)
                    trackers[data.type]?.setProgress(progress.toInt())
                }
            }
            NetworkUtils.doHttpsGet(context, HOST, "$PATH/${data.fileName}.gz", null,
                dbFile, receiver, Bundle(), GZIPInputStream::class.java)
            true
        } catch (e: Exception) {
            UiUtils.showToast(context, e.message)
            false
        }
    }

    private fun updateCatalog(data: DataInfo): Boolean {
        val values = ContentValues().apply {
            put(DatabaseManager.Catalog.TYPE, data.type)
            put(DatabaseManager.Catalog.DESCRIPTION, data.desc)
            put(DatabaseManager.Catalog.VERSION, data.version)
            put(DatabaseManager.Catalog.START_DATE, TimeUtils.format3339(data.start))
            put(DatabaseManager.Catalog.END_DATE, TimeUtils.format3339(data.end))
            put(DatabaseManager.Catalog.DB_NAME, data.fileName)
            put(DatabaseManager.Catalog.INSTALL_DATE, TimeUtils.format3339(Date()))
        }
        Log.i(TAG, "Inserting catalog: type=${data.type}, version=${data.version}, db=${data.fileName}")
        if (dbManager.insertCatalogEntry(values) < 0) {
            context?.let { UiUtils.showToast(it, "Failed to update catalog database") }
            return false
        }
        return true
    }

    private fun checkDelete() {
        MaterialAlertDialogBuilder(requireActivity())
            .setTitle("Delete Data")
            .setMessage("Are you sure you want to delete all installed data?")
            .setPositiveButton("Yes") { _, _ -> deleteData() }
            .setNegativeButton("No", null)
            .show()
    }

    /** Deletes every installed file and its catalog entry, with the screen's spinner showing. */
    private fun deleteData() {
        viewLifecycleOwner.lifecycleScope.launch {
            setContentShown(false)
            val ok = withContext(Dispatchers.IO) { deleteAllData() }
            if (!ok) {
                Toast.makeText(requireContext(), "There was an error while deleting installed data",
                    Toast.LENGTH_LONG).show()
            }
            checkData()
        }
    }

    private fun deleteAllData(): Boolean {
        var ok = true
        // Make sure all the databases to delete are closed
        dbManager.closeDatabases()
        val catalogDb = dbManager.catalogDb
        catalogDb.query(DatabaseManager.Catalog.TABLE_NAME, null, null, null, null, null, null).use { c ->
            while (c.moveToNext()) {
                val id = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID))
                val dbName = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Catalog.DB_NAME))
                val rows = catalogDb.delete(DatabaseManager.Catalog.TABLE_NAME, "_id=?", arrayOf(id.toString()))
                if (rows != 1) ok = false
                dbManager.getDatabaseFile(dbName).takeIf { it.exists() }?.delete()
            }
        }
        return ok
    }

    /** Lists the installed data, and what the manifest has that is newer. */
    private fun checkData() {
        viewLifecycleOwner.lifecycleScope.launch {
            view?.findViewById<Button>(R.id.btnDownload)?.isEnabled = false
            installedData.clear()
            availableData.clear()
            val ok = withContext(Dispatchers.IO) {
                readInstalled() && downloadManifest() && parseManifest()
            }
            if (!ok) {
                view?.findViewById<TextView>(android.R.id.empty)?.setText(R.string.download_error)
                setContentShown(true)
                return@launch
            }
            dropExpiredAndInstalled()
            updateDownloadList()
        }
    }

    private fun readInstalled(): Boolean {
        val now = Date()
        dbManager.allFromCatalog.use { c ->
            while (c.moveToNext()) {
                val info = DataInfo().apply {
                    type = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Catalog.TYPE))
                    desc = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Catalog.DESCRIPTION))
                    version = c.getInt(c.getColumnIndexOrThrow(DatabaseManager.Catalog.VERSION))
                }
                val start = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Catalog.START_DATE))
                val end = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Catalog.END_DATE))
                Log.i(TAG, "${info.type},$start,$end")
                info.start = TimeUtils.parse3339(start) ?: continue
                info.end = TimeUtils.parse3339(end) ?: continue
                if (now.before(info.end)) installedData.add(info)
            }
        }
        return true
    }

    /** Fetches the manifest of available data, unless fetched in the last 10 minutes. */
    private fun downloadManifest(): Boolean {
        val context = context ?: return false
        if (!NetworkUtils.isNetworkAvailable(context)) {
            UiUtils.showToast(context, "Please check your network connection")
            return false
        }
        return try {
            val manifest = File(context.cacheDir, MANIFEST)
            val age = System.currentTimeMillis() - manifest.lastModified()
            if (!manifest.exists() || age >= MANIFEST_MAX_AGE) {
                NetworkUtils.doHttpsGet(context, HOST, "$PATH/$MANIFEST", manifest)
            }
            true
        } catch (e: Exception) {
            UiUtils.showToast(context, e.message)
            false
        }
    }

    private fun parseManifest(): Boolean {
        val context = context ?: return false
        return try {
            val info = DataInfo()
            val root = RootElement("manifest")
            val datafile = root.getChild("datafile")
            datafile.setEndElementListener { availableData.add(info.copy()) }
            datafile.getChild("type").setEndTextElementListener { info.type = it }
            datafile.getChild("desc").setEndTextElementListener { info.desc = it }
            datafile.getChild("version").setEndTextElementListener { info.version = it.toInt() }
            datafile.getChild("filename").setEndTextElementListener { info.fileName = it }
            datafile.getChild("size").setEndTextElementListener { info.size = it.toInt() }
            datafile.getChild("start").setEndTextElementListener { info.start = TimeUtils.parse3339(it) ?: Date(0) }
            datafile.getChild("end").setEndTextElementListener { info.end = TimeUtils.parse3339(it) ?: Date(0) }
            File(context.cacheDir, MANIFEST).inputStream().use {
                Xml.parse(it, Xml.Encoding.UTF_8, root.contentHandler)
            }
            availableData.sort()
            true
        } catch (e: Exception) {
            UiUtils.showToast(context, e.message)
            false
        }
    }

    /** Drops available data that has expired or is already installed. */
    private fun dropExpiredAndInstalled() {
        val now = Date()
        availableData.removeAll { available ->
            when {
                now.after(available.end) -> {
                    Log.i(TAG, "Removing expired ${available.type}:${available.version}")
                    true
                }
                available in installedData -> {
                    Log.i(TAG, "Removing installed ${available.type}:${available.version}")
                    true
                }
                else -> false
            }
        }
    }

    private fun cleanupExpiredData() {
        val catalogDb = dbManager.catalogDb
        val now = Date()
        dbManager.allFromCatalog.use { c ->
            while (c.moveToNext()) {
                // Delete each expired database and its catalog entry
                val end = TimeUtils.parse3339(c.getString(c.getColumnIndexOrThrow(DatabaseManager.Catalog.END_DATE)))
                if (end != null && end.after(now)) continue
                val id = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID))
                val dbName = c.getString(c.getColumnIndexOrThrow(DatabaseManager.Catalog.DB_NAME))
                if (catalogDb.isOpen && dbManager.getDatabaseFile(dbName).delete()) {
                    catalogDb.delete(DatabaseManager.Catalog.TABLE_NAME, "${BaseColumns._ID}=?",
                        arrayOf(id.toString()))
                }
            }
        }
    }

    private fun updateDownloadList() {
        val view = view ?: return
        val cursor = DownloadCursor(requireContext()).apply {
            availableData.forEach { addRow(R.string.download_available, it) }
            installedData.forEach { addRow(R.string.download_installed, it) }
        }
        listView.adapter = DownloadListAdapter(requireContext(), cursor)
        setContentShown(true)

        if (cursor.count == 0) {
            view.findViewById<TextView>(android.R.id.empty).setText(R.string.download_error)
            return
        }
        view.findViewById<Button>(R.id.btnDelete).apply {
            visibility = if (installedData.isEmpty()) View.GONE else View.VISIBLE
            isEnabled = true
        }
        view.findViewById<Button>(R.id.btnDownload).apply {
            visibility = if (availableData.isEmpty()) View.GONE else View.VISIBLE
            isEnabled = true
        }
    }

    private class DownloadCursor(private val context: Context) : MatrixCursor(
        arrayOf(BaseColumns._ID, SECTION, TYPE, DESC, DATES, MSG, EXPIRED)
    ) {
        private var id = 0
        private val now = Date()

        fun addRow(section: Int, info: DataInfo) {
            val dates = DateUtils.formatDateRange(context, info.start.time, info.end.time + 1000,
                DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_ALL)
            val size = String.format(Locale.US, "%s (%s @ %dkbps)",
                Formatter.formatShortFileSize(context, info.size.toLong()),
                DateUtils.formatElapsedTime(info.size / (SPEED_KBPS * 1024 / 8)), SPEED_KBPS)
            newRow().add(id++).add(section).add(info.type).add(info.desc)
                .add("Effective $dates").add(size).add(if (!now.before(info.end)) "Y" else "N")
        }

        companion object {
            const val SECTION = "SECTION"
            const val TYPE = "TYPE"
            const val DESC = "DESC"
            const val DATES = "DATES"
            const val MSG = "MSG"
            const val EXPIRED = "EXPIRED"
            // For the download time estimate
            private const val SPEED_KBPS = 500L
        }
    }

    private inner class DownloadListAdapter(context: Context, c: Cursor) :
        SectionedCursorAdapter(context, R.layout.download_list_item, c, R.layout.list_item_header) {

        override val sectionName: String
            get() = resources.getString(cursor.getInt(cursor.getColumnIndexOrThrow(DownloadCursor.SECTION)))

        override fun newView(context: Context, c: Cursor, parent: ViewGroup): View {
            val view = super.newView(context, c, parent)
            if (c.getInt(c.getColumnIndexOrThrow(DownloadCursor.SECTION)) == R.string.download_available) {
                trackers[c.getString(c.getColumnIndexOrThrow(DownloadCursor.TYPE))] = ProgressTracker(view)
            } else {
                view.findViewById<View>(R.id.download_msg).visibility = View.GONE
                view.findViewById<View>(R.id.download_status).visibility = View.GONE
            }
            return view
        }

        override fun areAllItemsEnabled() = true

        override fun isEnabled(position: Int) = false

        override fun bindView(view: View, context: Context, cursor: Cursor) {
            view.findViewById<TextView>(R.id.download_desc).text =
                cursor.getString(cursor.getColumnIndexOrThrow(DownloadCursor.DESC))
            view.findViewById<TextView>(R.id.download_dates).apply {
                text = cursor.getString(cursor.getColumnIndexOrThrow(DownloadCursor.DATES))
                if (cursor.getString(cursor.getColumnIndexOrThrow(DownloadCursor.EXPIRED)) == "Y") {
                    text = "$text (Expired)"
                    setTextColor(Color.RED)
                }
            }
            view.findViewById<TextView>(R.id.download_msg).text =
                cursor.getString(cursor.getColumnIndexOrThrow(DownloadCursor.MSG))
        }
    }

    /** The progress bar and text on an available file's row. */
    private inner class ProgressTracker(v: View) {
        private val msgText: TextView = v.findViewById(R.id.download_msg)
        private val statusText: TextView = v.findViewById(R.id.download_status)
        private val progressBar: ProgressBar = v.findViewById(R.id.download_progress)

        fun initProgress(max: Int) {
            progressBar.max = max
            msgText.setText(R.string.installing)
            setProgress(0)
            listOf(progressBar, statusText, msgText).forEach { it.visibility = View.VISIBLE }
        }

        fun hideProgress() {
            listOf(progressBar, statusText, msgText).forEach { it.visibility = View.GONE }
        }

        fun setProgress(progress: Int) {
            progressBar.progress = progress
            val context = context ?: return
            if (progress < progressBar.max) {
                statusText.text = String.format("%s of %s",
                    Formatter.formatShortFileSize(context, progress.toLong()),
                    Formatter.formatShortFileSize(context, progressBar.max.toLong()))
            } else {
                msgText.setText(R.string.install_done)
                statusText.visibility = View.GONE
            }
        }
    }

    companion object {
        private val TAG = DownloadActivity::class.java.name
        private const val HOST = "commondatastorage.googleapis.com"
        private const val PATH = "/flightintel/database"
        private const val MANIFEST = "manifest.xml"
        private const val MANIFEST_MAX_AGE = 10 * DateUtils.MINUTE_IN_MILLIS
    }
}
