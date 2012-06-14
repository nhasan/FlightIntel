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

package com.nadmm.airports.wx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;

import android.app.IntentService;
import android.content.Intent;

import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.SystemUtils;

public abstract class NoaaService extends IntentService {

    protected final String NOAA_HOST = "weather.aero";
    protected final String RADAR_HOST = "radar.weather.gov";

    protected final String NOAA_DATASERVER_PATH = "/dataserver1_4/httpparam";

    public static final String STATION_ID = "STATION_ID";
    public static final String CACHE_ONLY = "CACHE_ONLY";
    public static final String FORCE_REFRESH = "FORCE_REFRESH";
    public static final String RADIUS_NM = "RADIUS_NM";
    public static final String LOCATION = "LOCATION";
    public static final String HOURS_BEFORE = "HOURS_BEFORE";
    public static final String COORDS_BOX = "COORDS_BOX";
    public static final String TYPE = "TYPE";
    public static final String IMAGE_CODE = "IMAGE_CODE";
    public static final String RESULT = "RESULT";

    public static final String TYPE_TEXT = "TYPE_TEXT";
    public static final String TYPE_IMAGE = "TYPE_IMAGE";

    public static final String ACTION_GET_METAR = "flightintel.intent.action.GET_METAR";
    public static final String ACTION_GET_TAF = "flightintel.intent.action.GET_TAF";
    public static final String ACTION_GET_PIREP = "flightintel.intent.action.GET_PIREP";
    public static final String ACTION_GET_AIRSIGMET = "flightintel.intent.action.GET_AIRSIGMET";
    public static final String ACTION_GET_RADAR = "flightintel.intent.action.GET_RADAR";
    public static final String ACTION_GET_PROGCHART = "flightintel.intent.action.GET_PROGCHART";
    public static final String ACTION_GET_WIND = "flightintel.intent.action.GET_WIND";

    protected File mDataDir;
    protected int mAge;
    private HttpClient mHttpClient;

    public NoaaService( String name, long age ) {
        super( name );
        mHttpClient = NetworkUtils.getHttpClient();
        mDataDir = SystemUtils.getExternalDir( "wx/"+name );

        if ( !mDataDir.exists() ) {
            mDataDir.mkdirs();
        }

        // Remove any old files from cache first
        cleanupCache( mDataDir, age );
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

    protected boolean fetchFromNoaa( String query, File file, boolean compressed ) 
            throws Exception {
        return fetchFromNoaa( NOAA_DATASERVER_PATH, query, file, compressed );
    }

    protected boolean fetchFromNoaa( String path, String query, File file, boolean compressed ) 
            throws Exception {
        return fetch( NOAA_HOST, path, query, file, compressed );
    }

    protected boolean fetch( String host, String path, String query, File file, boolean compressed ) 
            throws Exception {
        URI uri = URIUtils.createURI( "http", host, 80, path, query, null );
        return fetch( uri, file, compressed );
    }

    protected boolean fetch( URI uri, File file, boolean compressed ) 
            throws Exception {
        if ( !NetworkUtils.isNetworkAvailable( this ) ) {
            return false;
        }

        HttpGet get = new HttpGet( uri );
        HttpResponse response = mHttpClient.execute( get );

        int status = response.getStatusLine().getStatusCode();
        if ( status != HttpStatus.SC_OK ) {
            throw new Exception( response.getStatusLine().getReasonPhrase() );
        }

        byte[] buffer = new byte[ 16*1024 ];
        int count;
        FileOutputStream out = new FileOutputStream( file );
        InputStream in = response.getEntity().getContent();
        if ( compressed ) {
            in = new GZIPInputStream( in );
        }
        while ( ( count = in.read( buffer, 0, buffer.length ) ) != -1 ) {
            out.write( buffer, 0, count );
        }
        in.close();
        out.close();
        return true;
    }

    protected File getDataFile( String name ) {
        return new File( mDataDir, name );
    }

    protected void sendResultIntent( String action, String stationId, Serializable result ) {
        Intent intent = makeResultIntent( action, TYPE_TEXT );
        intent.putExtra( STATION_ID, stationId );
        intent.putExtra( RESULT, result );
        sendBroadcast( intent );
    }

    protected void sendResultIntent( String action, String code, File result ) {
        Intent intent = makeResultIntent( action, TYPE_IMAGE );
        intent.putExtra( IMAGE_CODE, code );
        if ( result.exists() ) {
            intent.putExtra( RESULT, result.getAbsolutePath() );
        }
        sendBroadcast( intent );
    }

    private Intent makeResultIntent( String action, String type ) {
        Intent intent = new Intent();
        intent.setAction( action );
        intent.putExtra( TYPE, type );
        return intent;
    }

}
