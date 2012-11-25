/*
 * FlightIntel for Pilots
 *
 * Copyright 2012 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.library;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.content.LocalBroadcastManager;

import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;

public class LibraryService extends IntentService {

    private static final String SERVICE_NAME = "library";

    public static final String LIBRARY_HOST = "commondatastorage.googleapis.com";
    public static final String LIBRARY_PATH = "/flightintel/library";
    public static final String ACTION_GET_BOOK = "flightintel.library.action.GET_BOOK";
    public static final String ACTION_DELETE_BOOK = "flightintel.library.action.DELETE_BOOK";
    public static final String ACTION_CHECK_BOOKS = "flightintel.library.action.CHECK_BOOKS";
    public static final String ACTION_DOWNLOAD_PROGRESS = "flightintel.library.action.PROGRESS";
    public static final String CATEGORY = "CATEGORY";
    public static final String BOOK_NAME = "BOOK_NAME";
    public static final String BOOK_NAMES = "BOOK_NAMES";
    public static final String PDF_PATH = "PDF_PATH";

    private final File mDataDir;
    private final HttpClient mHttpClient;

    public LibraryService() {
        super( SERVICE_NAME );

        mHttpClient = NetworkUtils.getHttpClient();
        mDataDir = SystemUtils.getExternalDir( SERVICE_NAME );

        if ( !mDataDir.exists() ) {
            mDataDir.mkdirs();
        }
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_CHECK_BOOKS ) ) {
            checkBooks( intent );
        } else if ( action.equals( ACTION_GET_BOOK ) ) {
            getBook( intent );
        } else if ( action.equals( ACTION_DELETE_BOOK ) ) {
            deleteBook( intent );
        }
    }

    private void checkBooks( Intent intent ) {
        String category = intent.getStringExtra( CATEGORY );
        ArrayList<String> books = intent.getStringArrayListExtra( BOOK_NAMES );

        cleanupBooks( category, books );

        File categoryDir = getCategoryDir( category );
        for ( String book : books ) {
            File pdfFile = new File( categoryDir, book );
            sendResult( intent.getAction(), category, pdfFile );
        }
    }

    private void getBook( Intent intent ) {
        String category = intent.getStringExtra( CATEGORY );
        String book = intent.getStringExtra( BOOK_NAME );
        File categoryDir = getCategoryDir( category );

        File pdfFile = new File( categoryDir, book );
        if ( !pdfFile.exists() ) {
            fetch( category, pdfFile );
        }

        sendResult( intent.getAction(), category, pdfFile );
    }

    private void deleteBook( Intent intent ) {
        String category = intent.getStringExtra( CATEGORY );
        String book = intent.getStringExtra( BOOK_NAME );
        File categoryDir = getCategoryDir( category );

        File pdfFile = new File( categoryDir, book );
        if ( pdfFile.exists() ) {
            pdfFile.delete();
        }

        sendResult( ACTION_CHECK_BOOKS, category, pdfFile );
    }

    private boolean fetch( String category, File pdfFile ) {
        try {
            String path = LIBRARY_PATH+"/"+category+"/"+pdfFile.getName()+".gz";
            URI uri = URIUtils.createURI( "http", LIBRARY_HOST, 80, path, null, null );

            ProgressReceiver receiver = new ProgressReceiver();
            Bundle result = new Bundle();
            result.putString( NetworkUtils.CONTENT_NAME, pdfFile.getName() );
            result.putString( CATEGORY, category );

            return NetworkUtils.doHttpGet( this, mHttpClient, uri, pdfFile, receiver, result,
                    GZIPInputStream.class );
        } catch ( Exception e ) {
            UiUtils.showToast( this, e.getMessage() );
        }
        return false;
    }

    protected void sendResult( String action, String category, File pdfFile ) {
        Intent result = new Intent( action );
        result.putExtra( CATEGORY, category );
        result.putExtra( BOOK_NAME, pdfFile.getName() );
        if ( pdfFile.exists() ) {
            result.putExtra( PDF_PATH, pdfFile.getAbsolutePath() );
        }
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.sendBroadcast( result );
    }

    private void cleanupBooks( String category, ArrayList<String> books ) {
        // Delete all books that are no longer in the library list for a category
        File categoryDir = getCategoryDir( category );
        File[] list = categoryDir.listFiles();
        if ( list != null ) {
            for ( File pdfFile : list ) {
                if ( !books.contains( pdfFile.getName() ) ) {
                    pdfFile.delete();
                }
            }
        }
    }

    private File getCategoryDir( String category ) {
        File categoryDir = new File( mDataDir, category );
        if ( !categoryDir.exists() ) {
            categoryDir.mkdirs();
        }
        return categoryDir;
    }

    protected void handleProgress( int resultCode, Bundle resultData ) {
        Intent intent = new Intent( ACTION_DOWNLOAD_PROGRESS );
        intent.putExtras( resultData );
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.sendBroadcast( intent );
    }

    private class ProgressReceiver extends ResultReceiver {

        public ProgressReceiver() {
            super( null );
        }

        @Override
        public void send( int resultCode, Bundle resultData ) {
            // We want to handle the result in the same thread synchronously
            handleProgress( resultCode, resultData );
        }

    }

}
