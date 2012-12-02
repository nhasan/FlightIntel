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

package com.nadmm.airports.tfr;

import java.io.File;
import java.net.URI;
import java.util.Date;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;

import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;

public class TfrService extends IntentService {

    private static final String SERVICE_NAME = "tfr";

    public static final String TFR_HOST = "www.jepptech.com";
    public static final String TFR_PATH = "/tfr/Query.asp";
    public static final String TFR_QUERY = "UserID=Public&DeletedMinutes=180&ExpiredMinutes=0";
    public static final String ACTION_GET_TFR_LIST = "flightintel.tfr.action.GET_TFR_LIST";
    public static final String FORCE_REFRESH = "FORCE_REFRESH";
    public static final String TFR_LIST = "TFR_LIST";

    private static final String TFR_CACHE_NAME = "tfr.xml";
    private static final long TFR_CACHE_MAX_AGE = 15*DateUtils.MINUTE_IN_MILLIS;

    private final File mDataDir;
    private final HttpClient mHttpClient;
    private final TfrParser mParser;

    public TfrService() {
        super( SERVICE_NAME );

        mHttpClient = NetworkUtils.getHttpClient();
        mDataDir = SystemUtils.getExternalDir( SERVICE_NAME );
        mParser = new TfrParser();

        if ( !mDataDir.exists() ) {
            mDataDir.mkdirs();
        }

        // Remove any old files from cache first
        cleanupCache( mDataDir, TFR_CACHE_MAX_AGE );
    }

    private void cleanupCache( File dir, long maxAge ) {
        // Delete all files that are older
        Date now = new Date();
        File[] files = dir.listFiles();
        for ( File file : files ) {
            long age = now.getTime()-file.lastModified();
            if ( age > maxAge ) {
                file.delete();
            }
        }
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_TFR_LIST ) ) {
            getTfrList( intent );
        }
    }

    private void getTfrList( Intent intent ) {
        boolean force = intent.getBooleanExtra( FORCE_REFRESH, false );

        File tfrFile = new File( mDataDir, TFR_CACHE_NAME );
        if ( !tfrFile.exists() || force ) {
            fetch( tfrFile );
        }

        TfrList tfrList = new TfrList();
        mParser.parse( tfrFile, tfrList );

        Intent result = makeResultIntent( intent.getAction() );
        result.putExtra( TFR_LIST, tfrList );
        sendResultIntent( result );
    }

    private void fetch( File tfrFile ) {
        try {
            URI uri = URIUtils.createURI( "http", TFR_HOST, 80, TFR_PATH, TFR_QUERY, null );

            NetworkUtils.doHttpGet( this, mHttpClient, uri, tfrFile, null, null, null );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "TFR: "+e.getMessage() );
        }
    }

    private Intent makeResultIntent( String action ) {
        Intent intent = new Intent();
        intent.setAction( action );
        return intent;
    }

    private void sendResultIntent( Intent intent ) {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.sendBroadcast( intent );
    }

}
