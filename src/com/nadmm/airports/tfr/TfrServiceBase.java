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

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;

import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;

public abstract class TfrServiceBase extends IntentService {

    private  static final String SERVICE_NAME = "tfr";

    private final File mDataDir;
    private final HttpClient mHttpClient;
    private final long TFR_CACHE_MAX_AGE = 15*DateUtils.MINUTE_IN_MILLIS;

    public TfrServiceBase() {
        super( SERVICE_NAME );

        mHttpClient = NetworkUtils.getHttpClient();
        mDataDir = SystemUtils.getExternalDir( SERVICE_NAME );

        if ( !mDataDir.exists() ) {
            mDataDir.mkdirs();
        }

        // Remove any old files from cache first
        cleanupCache( mDataDir, TFR_CACHE_MAX_AGE );
    }

    protected void cleanupCache( File dir, long maxAge ) {
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

    protected void fetch( URI uri, File tfrFile ) {
        try {
            NetworkUtils.doHttpGet( this, mHttpClient, uri, tfrFile, null, null, null );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "TFR: "+e.getMessage() );
        }
    }

    protected File getFile( String name ) {
        return new File( mDataDir, name );
    }

    protected Intent makeResultIntent( String action ) {
        Intent intent = new Intent();
        intent.setAction( action );
        return intent;
    }

    protected void sendResultIntent( Intent intent ) {
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.sendBroadcast( intent );
    }

}
