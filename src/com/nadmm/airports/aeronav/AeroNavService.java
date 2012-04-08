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

package com.nadmm.airports.aeronav;

import java.io.File;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;

import com.nadmm.airports.AirportsMain;
import com.nadmm.airports.utils.NetworkUtils;

import android.app.IntentService;
import android.content.Intent;

public abstract class AeroNavService extends IntentService {

    public static final String CYCLE_NAME = "CYCLE_NAME";
    public static final String PDF_NAME = "PDF_NAME";
    public static final String PDF_PATH = "PDF_PATH";
    public static final String PDF_NAMES = "PDF_NAMES";
    public static final String DOWNLOAD_IF_MISSING = "DOWNLOAD_IF_MISSING";
    public static final String VOLUME_NAME = "VOLUME_NAME";

    protected final HttpClient mHttpClient;
    protected final HttpHost mTarget;

    private final String AERONAV_HOST = "aeronav.faa.gov";
    private final File DATA_DIR;

    public AeroNavService( String name ) {
        super( name );

        mHttpClient = NetworkUtils.getHttpClient();
        mTarget = new HttpHost( AERONAV_HOST, 80 );
        DATA_DIR = new File( AirportsMain.EXTERNAL_STORAGE_DATA_DIRECTORY, "/"+name );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if ( !DATA_DIR.exists() ) {
            DATA_DIR.mkdirs();
        }
    }

    protected File getCycleDir( String cycle ) {
        File dir = new File( DATA_DIR, cycle );
        if ( !dir.exists() ) {
            cleanupOldCycles();
            dir.mkdir();
        }
        return dir;
    }

    protected void sendResult( String action, String cycle, File pdfFile ) {
        Intent result = new Intent();
        result.setAction( action );
        result.putExtra( CYCLE_NAME, cycle );
        result.putExtra( PDF_NAME, pdfFile.getName() );
        if ( pdfFile.exists() ) {
            result.putExtra( PDF_PATH, pdfFile.getAbsolutePath() );
        }
        sendBroadcast( result );
    }

    protected void cleanupOldCycles() {
        File[] cycles = DATA_DIR.listFiles();
        for ( File cycle : cycles ) {
            if ( cycle.isDirectory() ) {
                // First delete all the charts within this cycle directory
                File[] files = cycle.listFiles();
                for ( File file : files ) {
                    file.delete();
                }
            }
            // Now delete the cycle directory itself
            cycle.delete();
        }
    }

}
