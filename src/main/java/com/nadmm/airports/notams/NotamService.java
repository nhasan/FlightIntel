/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2021 Nadeem Hasan <nhasan@nadmm.com>
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

package com.nadmm.airports.notams;

import android.app.IntentService;
import android.content.Intent;
import android.text.format.DateUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;

import java.io.File;
import java.net.URL;
import java.util.Date;

public class NotamService extends IntentService {

    public static final String ACTION_GET_NOTAM = "flightintel.intent.action.GET_NOTAM";
    public static final String LOCATION = "LOCATION";
    public static final String FORCE_REFRESH = "FORCE_REFRESH";
    public static final String NOTAM_PATH = "NOTAM_PATH";

    private static final long NOTAM_CACHE_MAX_AGE = 15*DateUtils.MINUTE_IN_MILLIS;
    private static final String SERVICE_NAME = "notam";

    private File mDataDir;

    public NotamService() {
        super( SERVICE_NAME );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mDataDir = SystemUtils.getExternalDir( this, SERVICE_NAME );
        cleanupCache();
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String location = intent.getStringExtra( LOCATION );
        boolean force = intent.getBooleanExtra( FORCE_REFRESH, false );

        File notamFile = new File( mDataDir, "NOTAM_"+location+".json" );
        if ( force || !notamFile.exists() ) {
            try {
                fetchNotams( location, notamFile );
            } catch ( Exception e ) {
                UiUtils.showToast( this, e.getMessage() );
            }
        }
        sendResult( location, notamFile );
    }

    private void fetchNotams( String location, File notamFile ) throws Exception {
        String NOTAM_URL = "https://api.flightintel.com/notams/%s";
        URL url = new URL( String.format( NOTAM_URL, location ) );
        boolean ok = NetworkUtils.doHttpGet(this, url, notamFile, null, null, null );
        if ( ok && notamFile.length() > 0 ) {
            sendResult( location, notamFile );
        }
    }

    protected void sendResult( String location, File notamFile ) {
        Intent result = new Intent();
        result.setAction( ACTION_GET_NOTAM );
        if ( notamFile.exists() ) {
            result.putExtra( NOTAM_PATH, notamFile.getAbsolutePath() );
        }
        result.putExtra( LOCATION, location );
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.sendBroadcast( result );
    }

    private void cleanupCache() {
        Date now = new Date();
        File[] files = mDataDir.listFiles();
        if ( files != null ) {
            for ( File file : files ) {
                long age = now.getTime() - file.lastModified();
                if ( age > NOTAM_CACHE_MAX_AGE ) {
                    file.delete();
                }
            }
        }
    }

}
