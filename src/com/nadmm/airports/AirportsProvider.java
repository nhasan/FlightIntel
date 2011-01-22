/*
 * Airports for Android
 *
 * Copyright 2011 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

/**
 * Provides access to FAA airports database
 *
 */
public class AirportsProvider extends ContentProvider {
    public static final String TAG = "ContentProvider";

    public static final String AUTHORITY = "com.nadmm.airports.ContentProvider";
    public static final Uri CONTENT_URI = Uri.parse( "content://" + AUTHORITY + "/airport" );

    // MIME types used for searching airports or looking up a single airport
    public static final String DIR_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                                                  + "/vnd.nadmm.airports";
    public static final String ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                                                + "/vnd.nadmm.airports";

    public static final int SEARCH_AIRPORTS = 0;
    public static final int GET_AIRPORT = 1;
    public static final int SEARCH_SUGGEST = 2;

    public static final UriMatcher sUriMatcher = buildUriMatcher();

    public static final String[] projection = new String[] {
        "_ID", SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2
    };

    public static final Object[][] data = {
        { 1, "KTTN", "Trenton Mercer Airport" },
        { 2, "39N", "Princeton Airport" },
        { 3, "KEWR", "Newark Airport" }
    };

    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher( UriMatcher.NO_MATCH );

        // URIs to get the airport information
        matcher.addURI( AUTHORITY, "airports", SEARCH_AIRPORTS );
        matcher.addURI( AUTHORITY, "airports/#", GET_AIRPORT );

        // URIs to get the search suggestions
        matcher.addURI( AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST );
        matcher.addURI( AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST );

        return matcher;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#onCreate()
     */
    @Override
    public boolean onCreate() {
        return true;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#getType(android.net.Uri)
     */
    @Override
    public String getType(Uri uri) {
        switch ( sUriMatcher.match( uri ) ) {
        case SEARCH_AIRPORTS:
            return DIR_MIME_TYPE;
        case GET_AIRPORT:
            return ITEM_MIME_TYPE;
        case SEARCH_SUGGEST:
            return SearchManager.SUGGEST_MIME_TYPE;
        default:
            throw new IllegalArgumentException( "Unknown URL " + uri );
        }
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#query(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, 
            String sortOrder) {
        Log.v( TAG, uri.toString() );
        MatrixCursor cursor = new MatrixCursor( AirportsProvider.projection, data.length );
        for ( int i=0; i<data.length; ++i )
        {
            cursor.addRow( data[ i ] );
        }
        return cursor;
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#insert(android.net.Uri, android.content.ContentValues)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#delete(android.net.Uri, java.lang.String, java.lang.String[])
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see android.content.ContentProvider#update(android.net.Uri, android.content.ContentValues, java.lang.String, java.lang.String[])
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
