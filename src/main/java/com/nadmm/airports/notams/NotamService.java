/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2015 Nadeem Hasan <nhasan@nadmm.com>
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;

import com.nadmm.airports.utils.SystemUtils;
import com.nadmm.airports.utils.UiUtils;

public class NotamService extends IntentService {

    private static final String SERVICE_NAME = "notam";

    public static final String ACTION_GET_NOTAM = "flightintel.intent.action.GET_NOTAM";

    public static final String ICAO_CODE = "ICAO_CODE";
    public static final String NOTAM_PATH = "NOTAM_PATH";
    private static final long NOTAM_CACHE_MAX_AGE = 15*DateUtils.MINUTE_IN_MILLIS;

    private final File NOTAM_DIR;
    private final URL NOTAM_URL;
    private final String NOTAM_PARAM = "formatType=ICAO&retrieveLocId=%s&reportType=RAW"
            +"&actionType=notamRetrievalByICAOs&openItems=&submit=View%%20NOTAMs";

    public NotamService() throws MalformedURLException {
        super( SERVICE_NAME );
        NOTAM_DIR = SystemUtils.getExternalDir( SERVICE_NAME );
        NOTAM_URL = new URL( "https://pilotweb.nas.faa.gov"
                +"/PilotWeb/notamRetrievalByICAOAction.do?method=displayByICAOs" );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        cleanupCache();
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String icaoCode = intent.getStringExtra( ICAO_CODE );
        File notamFile = new File( NOTAM_DIR, "NOTAM_"+icaoCode+".txt" );
        if ( !notamFile.exists() ) {
            try {
                fetchNotams( icaoCode, notamFile );
            } catch ( IOException e ) {
                UiUtils.showToast( this, e.getMessage() );
            }
        }
        sendResult( notamFile );
    }

    private void fetchNotams( String icaoCode, File notamFile ) throws IOException {
        InputStream in = null;
        String params = String.format( NOTAM_PARAM, icaoCode );

        HttpsURLConnection conn = (HttpsURLConnection) NOTAM_URL.openConnection();
        conn.setRequestProperty( "Connection", "close" );
        conn.setDoInput( true );
        conn.setDoOutput( true );
        conn.setUseCaches( false );
        conn.setConnectTimeout( 30*1000 );
        conn.setReadTimeout( 30*1000 );
        conn.setRequestMethod( "POST" );
        conn.setRequestProperty( "User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US)" );
        conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded" );
        conn.setRequestProperty( "Content-Length", Integer.toString(params.length() ) );

        // Write out the form parameters as the request body
        OutputStream faa = conn.getOutputStream();
        faa.write( params.getBytes( "UTF-8" ) );
        faa.close();

        int response = conn.getResponseCode();
        if ( response == HttpURLConnection.HTTP_OK ) {
            // Request was successful, parse the html to extract notams
            in = conn.getInputStream();
            ArrayList<String> notams = parseNotamsFromHtml( in );
            in.close();

            // Write the NOTAMS to the cache file
            BufferedOutputStream cache = new BufferedOutputStream(
                    new FileOutputStream( notamFile ) );
            for ( String notam : notams ) {
                cache.write( notam.getBytes() );
                cache.write( '\n' );
            }
            cache.close();
        }
    }

    private ArrayList<String> parseNotamsFromHtml( InputStream in ) throws IOException {
        ArrayList<String> notams = new ArrayList<String>();

        BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );

        String line = null;
        boolean inside = false;
        StringBuilder builder = null;
        while ( ( line = reader.readLine() ) != null ) {
            if ( !inside ) {
                // Inspect the contents of all <pre> tags to find NOTAMs
                if ( line.toUpperCase( Locale.US ).contains( "<PRE>" ) ) {
                    builder = new StringBuilder();
                    inside = true;
                }
            }
            if ( inside ) {
                builder.append( line );
                builder.append( " " );
                if ( line.toUpperCase( Locale.US ).contains( "</PRE>" ) ) {
                    inside = false;
                    int start = builder.indexOf( "!" );
                    if ( start >= 0 ) {
                        // Now get the actual inner contents
                        int end = builder.indexOf( "SOURCE:" );
                        String notam = builder.substring( start, end ).trim();
                        // Normalize the whitespaces
                        //notam = whitespaces.matcher( notam ).replaceAll( " " );
                        notams.add( notam );
                    }
                }
            }
        }

        return notams;
    }

    protected void sendResult( File notamFile ) {
        Intent result = new Intent();
        result.setAction( ACTION_GET_NOTAM );
        if ( notamFile.exists() ) {
            result.putExtra( NOTAM_PATH, notamFile.getAbsolutePath() );
        }
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance( this );
        bm.sendBroadcast( result );
    }

    private void cleanupCache() {
        Date now = new Date();
        File[] files = NOTAM_DIR.listFiles();
        for ( File file : files ) {
            long age = now.getTime()-file.lastModified();
            if ( age > NOTAM_CACHE_MAX_AGE ) {
                file.delete();
            }
        }
    }

}
