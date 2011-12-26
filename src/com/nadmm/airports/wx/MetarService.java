/*
 * FlightIntel for Pilots
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

package com.nadmm.airports.wx;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Intent;

import com.nadmm.airports.AirportsMain;
import com.nadmm.airports.utils.GuiUtils;
import com.nadmm.airports.utils.NetworkUtils;

public class MetarService extends IntentService {

    private final String NOAA_HOST = "weather.aero";
    private final String METAR_PATH = "/dataserver_current/httpparam";
    private final String METAR_QUERY = "datasource=metars&requesttype=retrieve" +
    		"&format=xml&compression=gzip&hoursBeforeNow=3" +
    		"&mostRecentForEachStation=constraint&stationString=";

    private final File METAR_DIR = new File( 
            AirportsMain.EXTERNAL_STORAGE_DATA_DIRECTORY, "/metar" );

    private static final int MSECS_PER_MINUTE = 60*1000;
    private static final int METAR_CACHE_MAX_AGE = 30*MSECS_PER_MINUTE;

    public static final String STATION_ID = "STATION_ID";
    public static final String CACHE_ONLY = "CACHE_ONLY";
    public static final String FORCE_REFRESH = "FORCE_REFRESH";
    public static final String RESULT ="RESULT";
    public static final String ACTION_GET_METAR = "flightintel.intent.action.GET_METAR";

    protected MetarParser mParser;

    public MetarService() {
        super( "MetarService" );
        mParser = new MetarParser();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if ( !METAR_DIR.exists() ) {
            METAR_DIR.mkdirs();
        }

        // Remove any old METAR files from cache first
        cleanupMetarCache();
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        // Get request parameters
        String stationId = intent.getStringExtra( STATION_ID );
        boolean cacheOnly = intent.getBooleanExtra( CACHE_ONLY, false );
        boolean forceRefresh = intent.getBooleanExtra( FORCE_REFRESH, false );

        Metar metar = new Metar();

        File xml = new File( METAR_DIR, "METAR_"+stationId+".xml" );
        if ( forceRefresh || ( !cacheOnly && !xml.exists() ) ) {
            fetchMetarFromNOAA( stationId, xml );
        }

        if ( xml.exists() ) {
            mParser.parse( xml, metar );
            if ( !metar.isValid ) {
                xml.delete();
            }
        }

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( ACTION_GET_METAR );
        result.putExtra( STATION_ID, stationId );
        if ( metar.isValid ) {
            result.putExtra( RESULT, metar );
        }
        sendBroadcast( result );
    }

    protected void cleanupMetarCache() {
        // Delete all METAR files that are older
        Date now = new Date();
        File[] files = METAR_DIR.listFiles();
        for ( File file : files ) {
            long age = now.getTime()-file.lastModified();
            if ( age > METAR_CACHE_MAX_AGE ) {
                file.delete();
            }
        }
    }

    protected boolean fetchMetarFromNOAA( String stationId, File xml ) {
        if ( !NetworkUtils.isNetworkAvailable( this ) ) {
            return false;
        }

        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpHost target = new HttpHost( NOAA_HOST, 80 );
            URI uri = URIUtils.createURI( "http", NOAA_HOST, 80, METAR_PATH,
                    METAR_QUERY+stationId, null );
            HttpGet get = new HttpGet( uri );

            HttpResponse response = httpClient.execute( target, get );
            int status = response.getStatusLine().getStatusCode();
            if ( status != HttpStatus.SC_OK ) {
                GuiUtils.showToast( this, "Unable to fetch METAR: "
                            + response.getStatusLine().getReasonPhrase() );
                return false;
            }

            byte[] buffer = new byte[ 4096 ];
            int count;
            FileOutputStream out = new FileOutputStream( xml );
            GZIPInputStream in = new GZIPInputStream( response.getEntity().getContent() );
            while ( ( count = in.read( buffer, 0, buffer.length ) ) != -1 ) {
                out.write( buffer, 0, count );
            }
            in.close();
            out.close();
            return true;
        } catch ( Exception e ) {
            GuiUtils.showToast( this, "Unable to fetch METAR: "+e.getMessage() );
            return false;
        }
    }

}
