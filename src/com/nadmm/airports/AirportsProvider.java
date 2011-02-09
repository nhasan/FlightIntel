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

import java.util.HashMap;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.nadmm.airports.DatabaseManager.Airports;

/**
 * Provides access to FAA airports database
 *
 */
public class AirportsProvider extends ContentProvider {
    public static final String TAG = "ContentProvider";

    public static final String AUTHORITY = "com.nadmm.airports.AirportsProvider";
    public static final Uri CONTENT_URI = Uri.parse( "content://" + AUTHORITY + "/airport" );

    // MIME types used for searching airports or looking up a single airport
    public static final String DIR_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
                                                  + "/vnd.nadmm.airports";
    public static final String ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                                                + "/vnd.nadmm.airports";

    private static final int SEARCH_AIRPORTS = 0;
    private static final int GET_AIRPORT = 1;
    private static final int SEARCH_SUGGEST = 2;

    private static final UriMatcher mUriMatcher = buildUriMatcher();
    private static final HashMap<String, String> mColumnMap = buildProjectionMap();

    private DatabaseManager mDbManager;

    private static final String[] mSuggestionColumns = new String[] {
        BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

    @Override
    public boolean onCreate() {
        mDbManager = new DatabaseManager( getContext() );
        return true;
    }

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

    private static HashMap<String, String> buildProjectionMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put( BaseColumns._ID, BaseColumns._ID );
        map.put( SearchManager.SUGGEST_COLUMN_TEXT_1,
                "IFNULL("+Airports.ICAO_CODE+", "+Airports.FAA_CODE+")"
                +" AS "+SearchManager.SUGGEST_COLUMN_TEXT_1 );
        map.put( SearchManager.SUGGEST_COLUMN_TEXT_2,
                Airports.FACILITY_NAME+"||', '||"+Airports.ASSOC_CITY
                +"||' '||"+Airports.ASSOC_STATE
                +" AS "+SearchManager.SUGGEST_COLUMN_TEXT_2 );
        map.put( SearchManager.SUGGEST_COLUMN_INTENT_DATA, 
                Airports.SITE_NUMBER+" AS "+SearchManager.SUGGEST_COLUMN_INTENT_DATA);

        return map;
    }

    @Override
    public String getType( Uri uri ) {
        switch ( mUriMatcher.match( uri ) ) {
            case SEARCH_AIRPORTS:
                return DIR_MIME_TYPE;
            case GET_AIRPORT:
                return ITEM_MIME_TYPE;
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            default:
                throw new IllegalArgumentException( "Unknown Uri " + uri );
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, 
            String[] selectionArgs, String sortOrder) {
        Log.v( TAG, "Search="+uri.toString() );

        switch ( mUriMatcher.match( uri ) ) {
            case SEARCH_AIRPORTS:
            case SEARCH_SUGGEST:
                return searchAirports( uri );
            default:
                throw new IllegalArgumentException( "Unknown Uri " + uri );
        }
    }

    private Cursor searchAirports( Uri uri ) {
        String query = uri.getLastPathSegment();
        if ( query == null || SearchManager.SUGGEST_URI_PATH_QUERY.equals( query ) ) {
            throw new IllegalArgumentException(
                    "query must be provided for the Uri: "+uri );
        }
        return mDbManager.searchAirports( query.toUpperCase(), mColumnMap, mSuggestionColumns );
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
