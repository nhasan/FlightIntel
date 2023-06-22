/*
 * FlightIntel for Pilots
 *
 * Copyright 2023 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.datis

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

object DatisParser {
    fun parse(datisPath: String): ArrayList<Datis> {
        var input: FileInputStream? = null
        val datisList = ArrayList<Datis>()
        try {
            val gson = GsonBuilder()
                .registerTypeAdapter(OffsetDateTime::class.java, DateTypeAdapter())
                .create()
            input = FileInputStream(File(datisPath))
            val reader = JsonReader(InputStreamReader(input, StandardCharsets.UTF_8))
            reader.beginArray()
            while (reader.hasNext()) {
                val datis = gson.fromJson<Datis>(reader, Datis::class.java)
                datisList.add(datis)
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
        return datisList
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

data class Datis(
    @SerializedName("icaolocation")
    var icaoLocation: String,
    @SerializedName("issuedtimestamp")
    var issuedTimestamp: OffsetDateTime,
    @SerializedName("atistype")
    var atisType: String,
    @SerializedName("atiscode")
    var atisCode: String,
    @SerializedName("atisheader")
    var atisHeader: String,
    @SerializedName("atisbody")
    var atisBody: String
    ) {
}