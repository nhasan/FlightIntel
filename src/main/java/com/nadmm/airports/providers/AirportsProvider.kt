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
package com.nadmm.airports.providers

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.provider.BaseColumns
import com.nadmm.airports.afd.AirportsCursorHelper.query
import com.nadmm.airports.data.DatabaseManager
import com.nadmm.airports.data.DatabaseManager.Airports

class AirportsProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    override fun getType(uri: Uri): String {
        return when (sUriMatcher.match(uri)) {
            SEARCH_AIRPORTS -> DIR_MIME_TYPE
            GET_AIRPORT -> ITEM_MIME_TYPE
            SEARCH_SUGGEST -> SearchManager.SUGGEST_MIME_TYPE
            else -> throw IllegalArgumentException("Unknown Uri $uri")
        }
    }

    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {
        requireNotNull(selectionArgs) { "selectionArgs must be provided for the Uri: $uri" }
        val query = selectionArgs[0].uppercase().trim { it <= ' ' }
        return when (sUriMatcher.match(uri)) {
            SEARCH_SUGGEST -> suggestAirports(uri, query)
            SEARCH_AIRPORTS -> searchAirports(uri, query)
            else -> throw IllegalArgumentException("Unknown Uri $uri")
        }
    }

    private fun suggestAirports(uri: Uri, query: String): Cursor? {
        val dbManager = DatabaseManager.instance(context)
        val db = dbManager.getDatabase(DatabaseManager.DB_FADDS) ?: return null
        val selection = (Airports.FAA_CODE + "=? OR "
                + Airports.ICAO_CODE + "=? OR "
                + Airports.FACILITY_NAME + " LIKE ? OR "
                + Airports.ASSOC_CITY + " LIKE ?")
        val selectionArgs = arrayOf(query, query, "%$query%", "$query%")
        val limit = uri.getQueryParameter("limit")
        val builder = SQLiteQueryBuilder()
        builder.tables = Airports.TABLE_NAME
        builder.projectionMap = mSuggestionColumnMap
        return builder.query(
            db, mSuggestionColumns, selection, selectionArgs,
            null, null, SearchManager.SUGGEST_COLUMN_TEXT_1 + " ASC", limit
        )
    }

    private fun searchAirports(uri: Uri, query: String): Cursor {
        val selection = ("(" + Airports.FAA_CODE + "=? OR "
                + Airports.ICAO_CODE + "=? OR "
                + Airports.FACILITY_NAME + " LIKE ? ) OR "
                + Airports.ASSOC_CITY + " LIKE ?")
        val selectionArgs = arrayOf(query, query, "%$query%", "%$query%")
        val limit = uri.getQueryParameter("limit")
        val dbManager = DatabaseManager.instance(context)
        val db = dbManager.getDatabase(DatabaseManager.DB_FADDS)
        return query(
            db, selection, selectionArgs,
            null, null, Airports.FACILITY_NAME + " ASC", limit
        )
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        throw UnsupportedOperationException()
    }

    companion object {
        private const val AUTHORITY = "com.nadmm.airports.providers.AirportsProvider"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/airport")

        // MIME types used for searching airports or looking up a single airport
        const val DIR_MIME_TYPE = (ContentResolver.CURSOR_DIR_BASE_TYPE
                + "/vnd.nadmm.airports")
        const val ITEM_MIME_TYPE = (ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd.nadmm.airport")
        private const val SEARCH_AIRPORTS = 0
        private const val GET_AIRPORT = 1
        private const val SEARCH_SUGGEST = 2
        private val sUriMatcher = buildUriMatcher()
        private val mSuggestionColumnMap = buildSuggestionMap()
        private val mSuggestionColumns = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA
        )

        private fun buildUriMatcher(): UriMatcher {
            val matcher = UriMatcher(UriMatcher.NO_MATCH)

            // URIs to get the airport information
            matcher.addURI(AUTHORITY, "airport", SEARCH_AIRPORTS)
            matcher.addURI(AUTHORITY, "airport/*", GET_AIRPORT)

            // URIs to get the search suggestions
            matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST)
            matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST)
            return matcher
        }

        private fun buildSuggestionMap(): HashMap<String, String> {
            val map = HashMap<String, String>()
            map[BaseColumns._ID] = BaseColumns._ID
            map[SearchManager.SUGGEST_COLUMN_TEXT_1] =
                ("IFNULL(" + Airports.ICAO_CODE + ", " + Airports.FAA_CODE
                        + ") ||' - '||" + Airports.FACILITY_NAME
                        + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1)
            map[SearchManager.SUGGEST_COLUMN_TEXT_2] =
                (Airports.ASSOC_CITY + "||', '||" + Airports.ASSOC_STATE
                        + " AS " + SearchManager.SUGGEST_COLUMN_TEXT_2)
            map[SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA] =
                Airports.SITE_NUMBER + " AS " + SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA
            return map
        }
    }
}