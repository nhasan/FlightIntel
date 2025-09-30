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
package com.nadmm.airports.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.nadmm.airports.utils.UiUtils.showToast
import java.io.File

object SystemUtils {
    private const val MIME_TYPE_PDF = "application/pdf"

    fun canDisplayMimeType(context: Context, mimeType: String?): Boolean {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setType(mimeType)
        val list = pm.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return !list.isEmpty()
    }

    @JvmStatic
    val isExternalStorageAvailable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }

    fun startPDFViewer(context: Context, path: String) {
        if (canDisplayMimeType(context, MIME_TYPE_PDF)) {
            // Fire an intent to view the PDF chart
            val pdfFile = File(path)
            val pdfUri = FileProvider.getUriForFile(
                context,
                "com.nadmm.airports.fileprovider", pdfFile
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(pdfUri, MIME_TYPE_PDF)
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } else {
            // No PDF viewer is installed, send user to Play Store
            showToast(context, "Please install a PDF viewer app first")
            val market = Intent(Intent.ACTION_VIEW)
            val uri = "market://details?id=com.google.android.apps.pdfviewer".toUri()
            market.setData(uri)
            context.startActivity(market)
        }
    }

    fun getExternalDir(context: Context, dirName: String?): File {
        return context.getExternalFilesDirs(dirName)[0]
    }

    @JvmStatic
    fun getExternalFile(context: Context, dirName: String?, fileName: String): File {
        val dir = getExternalDir(context, dirName)
        return File(dir, fileName)
    }
}
