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
package com.nadmm.airports.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.ResultReceiver
import androidx.core.net.ConnectivityManagerCompat
import com.nadmm.airports.ActivityBase
import com.nadmm.airports.Application
import com.nadmm.airports.utils.UiUtils.showToast
import java.io.*
import java.lang.reflect.Constructor
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession
import kotlin.math.max

object NetworkUtils {
    private val sBuffer = ByteArray(32 * 1024)
    const val CONTENT_PROGRESS = "CONTENT_PROGRESS"
    const val CONTENT_LENGTH = "CONTENT_LENGTH"
    const val CONTENT_NAME = "CONTENT_NAME"

    @JvmStatic
    fun isNetworkAvailable(context: Context): Boolean {
        return isNetworkAvailable(context, true)
    }

    private fun isNetworkAvailable(context: Context, showMsg: Boolean): Boolean {
        val connMan = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        val network = connMan.activeNetworkInfo
        if (showMsg && (network == null || !network.isConnected)) {
            showToast(context, "Please check your internet connection")
            return false
        }
        return true
    }

    private fun isConnectedToMeteredNetwork(context: Context): Boolean {
        val cm = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        return ConnectivityManagerCompat.isActiveNetworkMetered(cm)
    }

    @JvmStatic
    fun checkNetworkAndDownload(context: Context, runnable: Runnable) {
        if (!isNetworkAvailable(context)) {
            showToast(context, "Please check your internet connection")
        }
        if (isConnectedToMeteredNetwork(context)) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(
                """
    You are connected to a metered network such as mobile data or tethered to mobile data.
    Continue download?
    """.trimIndent()
            )
                .setPositiveButton("Yes") { _: DialogInterface?, _: Int -> runnable.run() }
                .setNegativeButton("No") { _: DialogInterface?, _: Int -> }
            val alert = builder.create()
            alert.show()
        } else {
            runnable.run()
        }
    }

    fun canDownloadData(activity: ActivityBase): Boolean {
        val alwaysAutoFetch = activity.prefAutoDownoadOnMeteredNetwork
        return (alwaysAutoFetch
                || !isConnectedToMeteredNetwork(activity))
    }

    @JvmStatic
    @Throws(Exception::class)
    fun doHttpsGet(context: Context, host: String?, path: String?, file: File?): Boolean {
        return doHttpGet(context, "https", host, 443, path, null, file, null, null, null)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun doHttpsGet(
        context: Context, host: String?, path: String?,
        query: String?, file: File?, receiver: ResultReceiver?,
        result: Bundle?, filter: Class<out FilterInputStream?>?
    ): Boolean {
        val uri = URI("https", null, host, 443, path, query, null)
        return doHttpGet(context, uri.toURL(), file, receiver, result, filter)
    }

    @Throws(Exception::class)
    fun doHttpGet(
        context: Context, scheme: String?, host: String?, port: Int,
        path: String?, query: String?, file: File?, receiver: ResultReceiver?,
        result: Bundle?, filter: Class<out FilterInputStream?>?
    ): Boolean {
        val uri = URI(scheme, null, host, port, path, query, null)
        return doHttpGet(context, uri.toURL(), file, receiver, result, filter)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun doHttpGet(
        context: Context, url: URL,
        file: File?, receiver: ResultReceiver?, result: Bundle?,
        filter: Class<out FilterInputStream?>?
    ): Boolean {
        if (!isNetworkAvailable(context)) {
            return false
        }
        if (receiver != null && result == null) {
            throw Exception("Result cannot be null when receiver is passed")
        }
        var f: InputStream? = null
        val `in`: CountingInputStream
        var out: OutputStream? = null
        try {
            val hostnameVerifier =
                HostnameVerifier { _: String?, _: SSLSession? -> true }
            val conn = url.openConnection() as HttpURLConnection
            if (conn is HttpsURLConnection) {
                conn.hostnameVerifier = hostnameVerifier
            }
            if (!url.host.contains("faa.gov")) {
                // Do not override for FAA websites
                conn.setRequestProperty(
                    "User-Agent", String.format(
                        "FlightIntel/%s (Android; nhasan@nadmm.com)",
                        Application.version
                    )
                )
            }
            val status = conn.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                if (receiver != null) {
                    // Signal the receiver that download is aborted
                    result!!.putLong(CONTENT_LENGTH, 0)
                    result.putLong(CONTENT_PROGRESS, 0)
                    receiver.send(2, result)
                }
                throw Exception(conn.responseMessage)
            }
            val length = conn.contentLength.toLong()
            if (receiver != null) {
                result!!.putLong(CONTENT_LENGTH, length)
            }
            out = FileOutputStream(file)
            `in` = CountingInputStream(conn.inputStream)
            f = if (filter != null) {
                val ctor = filter.getConstructor(
                    InputStream::class.java
                ) as Constructor<FilterInputStream?>
                ctor.newInstance(`in`)
            } else {
                `in`
            }
            val chunk = max(length / 50, sBuffer.size.toLong())
            var last: Long = 0
            var count: Int
            while (f!!.read(sBuffer).also { count = it } != -1) {
                out.write(sBuffer, 0, count)
                if (receiver != null) {
                    val current = `in`.byteCount.toLong()
                    val delta = current - last
                    if (delta >= chunk) {
                        result!!.putLong(CONTENT_PROGRESS, current)
                        receiver.send(0, result)
                        last = current
                    }
                }
            }
            if (receiver != null) {
                // If compressed, the filter stream may not read the entire source stream
                result!!.putLong(CONTENT_PROGRESS, length)
                receiver.send(1, result)
            }
        } finally {
            try {
                f?.close()
                out?.close()
            } catch (ignored: IOException) {
            }
        }
        return true
    }

    /*
     *  An input stream that works as a pass-through filter but counts the bytes read from
     *  the underlying stream. It is needed for compressed streams that are wrapped by a
     *  GzipInputStream filter that only reports the decompressed byte count. Wrapping raw
     *  stream with this filter stream allows us to keep track of the download progress
     *  when all we know is the compressed size not the decompressed size.
     */
    private class CountingInputStream(`in`: InputStream?) : BufferedInputStream(`in`) {
        // Count of bytes actually read from the raw stream
        var byteCount: Int = 0
            private set

        @Throws(IOException::class)
        override fun read(buffer: ByteArray, offset: Int, bytes: Int): Int {
            val count = super.read(buffer, offset, bytes)
            if (count != -1) {
                byteCount += count
            }
            return count
        }
    }
}