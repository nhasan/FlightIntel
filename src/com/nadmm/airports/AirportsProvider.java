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
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.net.Uri.Builder;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.nadmm.airports.DatabaseManager.Airports;

/**
 * Provides access to FAA airports database
 *
 */
public class AirportsProvider extends ContentProvider {
    public static final String TAG = AirportsProvider.class.getSimpleName();

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

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final HashMap<String, String> mSuggestionColumnMap = buildSuggestionMap();

    private static final String[] mSuggestionColumns = new String[] {
        BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher( UriMatcher.NO_MATCH );

        // URIs to get the airport information
        matcher.addURI( AUTHORITY, "airport", SEARCH_AIRPORTS );
        matcher.addURI( AUTHORITY, "airport/*", GET_AIRPORT );

        // URIs to get the search suggestions
        matcher.addURI( AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST );
        matcher.addURI( AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST );

        return matcher;
    }

    private static HashMap<String, String> buildSuggestionMap() {
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
        switch ( sUriMatcher.match( uri ) ) {
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
        if ( selectionArgs == null ) {
            throw new IllegalArgumentException(
                "selectionArgs must be provided for the Uri: " + uri);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( getContext() );
        Builder builder= uri.buildUpon();

        // Get the row limit for the number of search result returned
        if ( prefs.getBoolean( PreferencesActivity.KEY_SEARCH_LIMITED_RESULT, true ) ) {
            // Search result limit is set
            String limit = prefs.getString( PreferencesActivity.KEY_SEARCH_RESULT_LIMIT, "20" );
            builder.appendQueryParameter( "limit", limit );
        }

        uri = builder.build();

        String query = selectionArgs[ 0 ].toUpperCase();
        Log.v( TAG, "Search="+uri.toString()+", query="+query );

        switch ( sUriMatcher.match( uri ) ) {
            case SEARCH_SUGGEST:
                return suggestAirports( uri, query );
            case SEARCH_AIRPORTS:
                return searchAirports( uri, query );
            default:
                throw new IllegalArgumentException( "Unknown Uri " + uri );
        }
    }

    private Cursor suggestAirports( Uri uri, String query ) {
        DatabaseManager dbManager = DatabaseManager.instance();
        SQLiteDatabase db = dbManager.getDatabase( DatabaseManager.DB_FADDS );
        if ( db == null ) {
            return null;
        }

        String selection = Airports.FAA_CODE+"=? OR "
                +Airports.ICAO_CODE+"=? OR "
                +Airports.FACILITY_NAME+" LIKE ? OR "
                +Airports.ASSOC_CITY+" LIKE ?";
        String[] selectionArgs = new String[] { query, query, "%"+query+"%", "%"+query+"%" };
        String limit = uri.getQueryParameter( "limit" );

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables( Airports.TABLE_NAME );
        builder.setProjectionMap( mSuggestionColumnMap );
        Cursor c = builder.query( db, mSuggestionColumns, selection, selectionArgs, 
                null, null, null, limit );
        return c;
    }

    private Cursor searchAirports( Uri uri, String query ) {
        String selection = "("+Airports.FAA_CODE+"=? OR "
                +Airports.ICAO_CODE+"=? OR "
                +Airports.FACILITY_NAME+" LIKE ? ) OR "
                +Airports.ASSOC_CITY+" LIKE ?";
        String[] selectionArgs = new String[] { query, query, "%"+query+"%", "%"+query+"%" };
        String limit = uri.getQueryParameter( "limit" );

        Cursor c = AirportsCursorHelper.query( selection, selectionArgs, 
                null, null, Airports.FACILITY_NAME+" ASC", limit );
        return c;
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
