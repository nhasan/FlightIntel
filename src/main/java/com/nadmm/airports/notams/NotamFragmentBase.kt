/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2022 Nadeem Hasan <nhasan@nadmm.com>
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
package com.nadmm.airports.notams

import com.nadmm.airports.FragmentBase
import android.content.IntentFilter
import android.content.BroadcastReceiver
import java.util.ArrayList
import android.os.Bundle
import android.content.Context
import android.content.Intent
import java.io.File
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.annotation.SuppressLint
import com.nadmm.airports.R
import android.widget.TextView
import java.util.Locale
import android.widget.LinearLayout
import java.time.format.DateTimeFormatter
import java.lang.StringBuilder
import java.io.FileInputStream
import com.google.gson.GsonBuilder
import java.time.OffsetDateTime
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.io.IOException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.nadmm.airports.utils.TimeUtils
import kotlin.Throws
import java.lang.NumberFormatException
import java.time.format.DateTimeParseException

open class NotamFragmentBase : FragmentBase() {
    private lateinit var mFilter: IntentFilter
    private lateinit var mReceiver: BroadcastReceiver
    private val mCategories = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFilter = IntentFilter(NotamService.ACTION_GET_NOTAM)
        mReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (NotamService.ACTION_GET_NOTAM == action) {
                    val path = intent.getStringExtra(NotamService.NOTAM_PATH)
                    if (path != null) {
                        val location = intent.getStringExtra(NotamService.LOCATION)
                        val notamFile = File(path)
                        showNotams(location, notamFile)
                        isRefreshing = false
                    }
                }
            }
        }
    }

    override fun onResume() {
        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.registerReceiver(mReceiver, mFilter)
        super.onResume()
    }

    override fun onPause() {
        val bm = LocalBroadcastManager.getInstance(requireActivity())
        bm.unregisterReceiver(mReceiver)
        super.onPause()
    }

    protected fun getNotams(location: String?, force: Boolean) {
        val service = Intent(activity, NotamService::class.java)
        service.action = NotamService.ACTION_GET_NOTAM
        service.putExtra(NotamService.LOCATION, location)
        service.putExtra(NotamService.FORCE_REFRESH, force)
        requireActivity().startService(service)
    }

    protected fun addCategory(type: String) {
        mCategories.add(type)
    }

    @SuppressLint("SetTextI18n")
    private fun showNotams(location: String?, notamFile: File) {
        val notams = parseNotams(notamFile)
        setActionBarSubtitle(
            resources.getQuantityString(
                R.plurals.notams_found,
                notams.size, notams.size
            )
        )
        val updated = findViewById<TextView>(R.id.notam_last_updated)
        updated!!.text = String.format(
            Locale.US, "Updated %s",
            TimeUtils.formatElapsedTime(notamFile.lastModified())
        )
        val content = findViewById<LinearLayout>(R.id.notam_content_layout)
        content!!.removeAllViews()
        for (notam in notams) {
            addRow(content, formatNotam(location, notam))
        }
        setFragmentContentShown(true)
    }

    private fun formatNotam(location: String?, notam: Notam): String {
        val dtf = DateTimeFormatter.ofPattern("dd MMM HH:mm yyyy", Locale.US)
        val sb = StringBuilder()
        if (notam.classification == "FDC") {
            sb.append("FDC ")
        }
        sb.append(notam.notamID)
        if (notam.xovernotamID?.trim()?.isEmpty() == false) {
            sb.append(" (")
            sb.append(notam.xovernotamID)
            sb.append(")")
        }
        sb.append(" - ")
        if (notam.location != location) {
            sb.append(notam.location)
            sb.append(" ")
        }
        sb.append(notam.text)
        if (notam.text?.endsWith(".") == false) {
            sb.append(".")
        }
        sb.append(" ")
        sb.append(dtf.format(notam.effectiveStart).uppercase(Locale.getDefault()))
        sb.append(" UNTIL ")
        if (notam.effectiveEnd != null) {
            sb.append(dtf.format(notam.effectiveEnd).uppercase(Locale.getDefault()))
            if (notam.estimatedEnd == "Y") {
                sb.append(" ESTIMATED")
            }
        } else {
            sb.append("PERM")
        }
        sb.append(". CREATED: ")
        sb.append(dtf.format(notam.issued).uppercase(Locale.getDefault()))
        if (notam.lastUpdated != null) {
            sb.append(" UPDATED: ")
            sb.append(dtf.format(notam.lastUpdated).uppercase(Locale.getDefault()))
        }
        return sb.toString()
    }

    private fun parseNotams(notamFile: File): ArrayList<Notam> {
        var input: FileInputStream? = null
        val notams = ArrayList<Notam>()
        try {
            val gson = GsonBuilder()
                .registerTypeAdapter(Int::class.javaPrimitiveType, IntTypeAdapter())
                .registerTypeAdapter(OffsetDateTime::class.java, DateTypeAdapter())
                .create()
            input = FileInputStream(notamFile)
            val reader = JsonReader(InputStreamReader(input, StandardCharsets.UTF_8))
            reader.beginArray()
            while (reader.hasNext()) {
                val notam = gson.fromJson<Notam>(reader, Notam::class.java)
                val text = notam.text ?: ""
                notam.category = text.substring(0, text.indexOf(" ")).trim { it <= ' ' }
                if (mCategories.isEmpty() || mCategories.contains(notam.category)) {
                    notams.add(notam)
                }
            }
            reader.endArray()
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                input?.close()
            } catch (ignored: IOException) {
            }
        }
        return notams
    }

    class IntTypeAdapter : TypeAdapter<Int?>() {
        @Throws(IOException::class)
        override fun read(reader: JsonReader): Int? {
            // Allows to parse empty strings
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            val stringValue = reader.nextString()
            return try {
                stringValue.toInt()
            } catch (e: NumberFormatException) {
                0
            }
        }

        override fun write(out: JsonWriter, value: Int?) {}
    }

    class DateTypeAdapter : TypeAdapter<OffsetDateTime?>() {
        @Throws(IOException::class)
        override fun read(reader: JsonReader): OffsetDateTime? {
            // Allows to parse empty strings
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            val stringValue = reader.nextString()
            return try {
                OffsetDateTime.parse(stringValue)
            } catch (e: DateTimeParseException) {
                null
            }
        }

        override fun write(out: JsonWriter, value: OffsetDateTime?) {}
    }
}