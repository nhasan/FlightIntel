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
import java.net.URI;

import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;

import android.app.IntentService;
import android.content.Intent;

import com.nadmm.airports.utils.FileUtils;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;

public abstract class AeroNavService extends IntentService {

    public static final String ACTION_GET_AFD = "flightintel.intent.action.GET_AFD";
    public static final String ACTION_CHECK_AFD = "flightintel.intent.action.CHECK_AFD";
    public static final String ACTION_GET_CHARTS = "flightintel.intent.action.GET_CHARTS";
    public static final String ACTION_CHECK_CHARTS = "flightintel.intent.action.CHECK_CHARTS";
    public static final String ACTION_DELETE_CHARTS = "flightintel.intent.action.DELETE_CHARTS";
    public static final String ACTION_COUNT_CHARTS = "flightintel.intent.action.COUNT_CHARTS";

    public static final String CYCLE_NAME = "CYCLE_NAME";
    public static final String TPP_VOLUME = "TPP_VOLUME";
    public static final String PDF_NAME = "PDF_NAME";
    public static final String PDF_PATH = "PDF_PATH";
    public static final String PDF_NAMES = "PDF_NAMES";
    public static final String PDF_COUNT = "PDF_COUNT";
    public static final String DOWNLOAD_IF_MISSING = "DOWNLOAD_IF_MISSING";

    protected final HttpClient mHttpClient;

    private final String AERONAV_HOST = "aeronav.faa.gov";
    private final File DATA_DIR;

    public AeroNavService( String name ) {
        super( name );

        mHttpClient = NetworkUtils.getHttpClient();
        DATA_DIR = SystemUtils.getExternalDir( name );
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

    protected boolean fetch( String path, File file ) {
        try {
            URI uri = URIUtils.createURI( "http", AERONAV_HOST, 80, path, null, null );
            return NetworkUtils.doHttpGet( this, mHttpClient, uri, file, null );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "Error: "+e.getMessage() );
        }
        return false;
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
        if ( cycles != null ) {
            for ( File cycle : cycles ) {
                FileUtils.removeDir( cycle );
            }
        }
    }

}
