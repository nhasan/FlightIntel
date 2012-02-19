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
import java.net.URI;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;

import com.nadmm.airports.utils.NetworkUtils;

public abstract class NoaaService extends IntentService {

    protected final String NOAA_HOST = "weather.aero";
    protected final String DATASERVER_PATH = "/dataserver1_4/httpparam";
    public static final String STATION_ID = "STATION_ID";
    public static final String CACHE_ONLY = "CACHE_ONLY";
    public static final String FORCE_REFRESH = "FORCE_REFRESH";
    public static final String RESULT = "RESULT";

    public static final String ACTION_GET_METAR = "flightintel.intent.action.GET_METAR";
    public static final String ACTION_GET_TAF = "flightintel.intent.action.GET_TAF";

    public NoaaService( String name ) {
        super( name );
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

    protected boolean fetchFromNOAA( URI uri, File xml ) 
            throws Exception {
        if ( !NetworkUtils.isNetworkAvailable( this ) ) {
            return false;
        }

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpHost target = new HttpHost( NOAA_HOST, 80 );
        HttpGet get = new HttpGet( uri );

        HttpResponse response = httpClient.execute( target, get );
        int status = response.getStatusLine().getStatusCode();
        if ( status != HttpStatus.SC_OK ) {
            throw new Exception( response.getStatusLine().getReasonPhrase() );
        }

        byte[] buffer = new byte[ 4096 ];
        int count;
        FileOutputStream out = new FileOutputStream( xml );
        InputStream in = new GZIPInputStream( response.getEntity().getContent() );
        while ( ( count = in.read( buffer, 0, buffer.length ) ) != -1 ) {
            out.write( buffer, 0, count );
        }
        in.close();
        out.close();
        return true;
    }

}
