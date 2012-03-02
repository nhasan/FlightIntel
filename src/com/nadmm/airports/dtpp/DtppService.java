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

package com.nadmm.airports.dtpp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Intent;

import com.nadmm.airports.AirportsMain;
import com.nadmm.airports.utils.UiUtils;

public class DtppService extends IntentService {

    public static final String ACTION_GET_CHART = "flightintel.intent.action.GET_CHART";
    public static final String ACTION_CHECK_CHART = "flightintel.intent.action.CHECK_CHART";

    public static final String TPP_CYCLE = "TPP_CYCLE";
    public static final String PDF_NAME = "PDF_NAME";
    public static final String GET_IF_MISSING = "GET_IF_MISSING";
    public static final String RESULT = "RESULT";

    private final File DTPP_DIR = new File(
            AirportsMain.EXTERNAL_STORAGE_DATA_DIRECTORY, "/dtpp" );

    private final HttpHost mTarget = new HttpHost( "aeronav.faa.gov", 80 );

    public DtppService() {
        super( "DtppService" );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if ( !DTPP_DIR.exists() ) {
            DTPP_DIR.mkdirs();
        }
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        String pdfName = intent.getStringExtra( PDF_NAME );

        File cycleDir = new File( DTPP_DIR, tppCycle );
        if ( !cycleDir.exists() ) {
            cycleDir.mkdir();
        }
        File pdfFile = new File( cycleDir, pdfName );

        boolean download = false;
        if ( action.equals( ACTION_GET_CHART ) ) {
            download = true;
        } else if ( action.equals( ACTION_CHECK_CHART ) ) {
            download = intent.getBooleanExtra( GET_IF_MISSING, false );
        }

        if ( download && !pdfFile.exists() ) {
            downloadChart( tppCycle, pdfFile );
        }

        // Broadcast the result
        Intent result = new Intent();
        result.setAction( action );
        result.putExtra( TPP_CYCLE, tppCycle );
        result.putExtra( PDF_NAME, pdfName );
        if ( pdfFile.exists() ) {
            result.putExtra( RESULT, pdfFile.getAbsolutePath() );
        }
        sendBroadcast( result );
    }

    protected void downloadChart( String tppCycle, File pdfFile ) {
        InputStream in = null;
        FileOutputStream out = null;

        try {
            DefaultHttpClient httpClient = new DefaultHttpClient();
            String path;
            if ( pdfFile.getName().equals( "legendAD.pdf" ) ) {
                path = "/content/aeronav/online/pdf_files/legendAD.pdf";
            } else {
                path = String.format( "/d-tpp/%s/%s", tppCycle, pdfFile.getName() );
            }
            URI uri = new URI( path );
            HttpGet get = new HttpGet( uri );

            HttpResponse response = httpClient.execute( mTarget, get );
            if ( response.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) {
                return;
            }

            out = new FileOutputStream( pdfFile );

            HttpEntity entity = response.getEntity();
            in = entity.getContent();

            byte[] buffer = new byte[ 16*1024 ];
            int count;
            int total = 0;

            while ( ( count = in.read( buffer, 0, buffer.length ) ) != -1 ) {
                out.write( buffer, 0, count );
                total += count;
            }
        } catch ( Exception e ) {
            UiUtils.showToast( getApplicationContext(), e.getMessage() );
        } finally {
            if ( in != null ) {
                try {
                    in.close();
                } catch ( IOException e ) {
                }
            }
            if ( out != null ) {
                try {
                    out.close();
                } catch ( IOException e ) {
                }
            }
        }
    }

}
