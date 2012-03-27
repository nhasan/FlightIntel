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

package com.nadmm.airports;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.nadmm.airports.utils.DataUtils;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.TimeUtils;
import com.nadmm.airports.utils.UiUtils;

public class NotamActivityBase extends ActivityBase {

    private static final File NOTAM_DIR = new File(
            AirportsMain.EXTERNAL_STORAGE_DATA_DIRECTORY, "/notam" );
    private static final int MSEC_PER_MINUTE = 60*1000;
    private static final int NOTAM_CACHE_MAX_AGE = 5*MSEC_PER_MINUTE;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        if ( !NOTAM_DIR.exists() ) {
            NOTAM_DIR.mkdirs();
        }
    }

    protected void showNotams( String icaoCode ) {
        LinearLayout content = (LinearLayout) findViewById( R.id.notam_content_layout );
        File notamFile = new File( NOTAM_DIR, "NOTAM_"+icaoCode+".txt" );

        HashMap<String, ArrayList<String>> notams = getNotamFromCache( notamFile );
        if ( notams == null ) {
            return;
        }

        int count = 0;
        // Get subjects in specific order
        String[] subjects = DataUtils.getNotamSubjects();
        for ( String subject : subjects ) {
            if ( !notams.containsKey( subject ) ) {
                continue;
            }
            TextView label = new TextView( this );
            label.setPadding(
                    UiUtils.convertDpToPx( this, 6 ),
                    UiUtils.convertDpToPx( this, 12 ),
                    UiUtils.convertDpToPx( this, 6 ),
                    0 );
            label.setTextAppearance( this, R.style.TextSmall_Bold );
            label.setText( subject );
            content.addView( label, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT ) );
            addSeparator( content );
            ArrayList<String> list = notams.get( subject );
            for ( String notam : list ) {
                addBulletedRow( content, notam );
                addSeparator( content );
                ++count;
            }
        }

        TextView title1 = (TextView) findViewById( R.id.notam_title1 );
        title1.setText( getResources().getQuantityString( R.plurals.notams_found, count, count ) );
        TextView title2 = (TextView) findViewById( R.id.notam_title2 );
        Date lastModified = new Date( notamFile.lastModified() );
        title2.setText( "Last updated at "+ TimeUtils.formatDateTime(
                this, lastModified.getTime() ) );
    }

    protected void getNotams( String icaoCode ) {
        cleanupNotamCache();
        File notamFile = new File( NOTAM_DIR, "NOTAM_"+icaoCode+".txt" );
        if ( notamFile.exists() ) {
            // NOTAM cache file is still valid, do nothing
            return;
        }

        // Notam not fetched yet or had expired and cleaned up, fetch from FAA
        try {
            getNotamsFromFAA( icaoCode, notamFile );
        } catch ( IOException e ) {
            UiUtils.showToast( NotamActivityBase.this, e.getMessage() );
        }
    }

    private void getNotamsFromFAA( String icaoCode, File notamFile ) throws IOException {
        if ( !NetworkUtils.isNetworkAvailable( this ) ) {
            return;
        }
        InputStream in = null;
        URL url = new URL( "https://pilotweb.nas.faa.gov"
        		+"/PilotWeb/notamRetrievalByICAOAction.do?method=displayByICAOs" );
        String params = "formatType=ICAO&retrieveLocId="+icaoCode+"&reportType=RAW"
                +"&actionType=notamRetrievalByICAOs&openItems=&submit=View%20NOTAMs";

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
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

    private HashMap<String, ArrayList<String>> getNotamFromCache( File notamFile ) {
        HashMap<String, ArrayList<String>> notams = new HashMap<String, ArrayList<String>>();

        if ( !notamFile.exists() ) {
            // NOTAM cache file is missing. Must be an issue with fetching from FAA
            TextView title1 = (TextView) findViewById( R.id.notam_title1 );
            title1.setText( "There was an error while fetching NOTAMs from FAA." );
            return null;
        }

        // Remeber the ids we have seen to remove duplicates
        Set<String> notamIDs = new HashSet<String>();

        // Build the NOTAM list group by the subject
        try {
            BufferedReader in = new BufferedReader( new FileReader( notamFile ) );
            String notam;
            while ( ( notam = in.readLine() ) != null ) {
                String parts[] = notam.split( " ", 5 );
                String notamID = parts[ 1 ];
                if ( !notamIDs.contains( notamID ) ) {
                    String keyword = parts[0].equals( "!FDC" )? "FDC" : parts[ 3 ];
                    String subject = DataUtils.getNotamSubjectFromKeyword( keyword );
                    ArrayList<String> list = notams.get( subject );
                    if ( list == null ) {
                        list = new ArrayList<String>();
                    }
                    list.add( notam );
                    notams.put( subject, list );
                    notamIDs.add( notamID );
                }
            }
            in.close();
        } catch ( IOException e ) {
            TextView title1 = (TextView) findViewById( R.id.notam_title1 );
            title1.setText( "There was an error during parsing NOTAMs from cache: "
                    +e.getMessage() );
        }

        return notams;
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
                if ( line.toUpperCase().contains( "<PRE>" ) ) {
                    builder = new StringBuilder();
                    inside = true;
                }
            }
            if ( inside ) {
                builder.append( line+" " );
                if ( line.toUpperCase().contains( "</PRE>" ) ) {
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

    private void cleanupNotamCache() {
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
