/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2025 Nadeem Hasan <nhasan@nadmm.com>
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

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.Reader

object FileUtils {
    fun removeDir(dir: File) {
        val files = dir.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory()) {
                    removeDir(file)
                } else {
                    file.delete()
                }
            }
        }
        dir.delete()
    }

    fun readFile(path: String?): String {
        val sb = StringBuilder()
        var `is`: FileInputStream? = null
        try {
            `is` = FileInputStream(path)
            val reader: Reader = BufferedReader(InputStreamReader(`is`))
            val buffer = CharArray(8192)
            var read: Int
            while ((reader.read(buffer).also { read = it }) > 0) {
                sb.append(buffer, 0, read)
            }
        } finally {
            `is`?.close()
        }
        return sb.toString()
    }
}
