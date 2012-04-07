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
import java.util.ArrayList;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;

import android.app.IntentService;
import android.content.Intent;

import com.nadmm.airports.AirportsMain;
import com.nadmm.airports.utils.NetworkUtils;

public class DtppService extends IntentService {

    public static final String ACTION_GET_CHART = "flightintel.intent.action.GET_CHART";
    public static final String ACTION_CHECK_CHARTS = "flightintel.intent.action.CHECK_CHARTS";
    public static final String ACTION_DELETE_CHARTS = "flightintel.intent.action.DELETE_CHARTS";

    public static final String TPP_VOLUME = "TPP_VOLUME";
    public static final String TPP_CYCLE = "TPP_CYCLE";
    public static final String PDF_NAME = "PDF_NAME";
    public static final String PDF_NAMES = "PDF_NAMES";
    public static final String DOWNLOAD_IF_MISSING = "DOWNLOAD_IF_MISSING";
    public static final String PDF_PATH = "PDF_PATH";

    private final String DTPP_HOST = "aeronav.faa.gov";
    private final File DTPP_DIR = new File(
            AirportsMain.EXTERNAL_STORAGE_DATA_DIRECTORY, "/dtpp" );

    private final HttpClient mHttpClient;
    private final HttpHost mTarget;

    public DtppService() {
        super( "DtppService" );
        mHttpClient = NetworkUtils.getHttpClient();
        mTarget = new HttpHost( DTPP_HOST, 80 );
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
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        File cycleDir = new File( DTPP_DIR, tppCycle );
        if ( !cycleDir.exists() ) {
            cleanupOldCycles();
            cycleDir.mkdir();
        }

        String action = intent.getAction();
        if ( action.equals( ACTION_GET_CHART ) ) {
            getChart( intent );
        } else if ( action.equals( ACTION_CHECK_CHARTS ) ) {
            checkCharts( intent );
        } else if ( action.equals( ACTION_DELETE_CHARTS ) ) {
            deleteCharts( intent );
        }
    }

    protected void checkCharts( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        ArrayList<String> pdfNames = intent.getStringArrayListExtra( PDF_NAMES );
        boolean download = intent.getBooleanExtra( DOWNLOAD_IF_MISSING, false );

        File cycleDir = new File( DTPP_DIR, tppCycle );

        for ( String pdfName : pdfNames ) {
            File pdfFile = new File( cycleDir, pdfName );
            if ( download && !pdfFile.exists() ) {
                downloadChart( tppCycle, pdfFile );
            }
            sendResult( ACTION_CHECK_CHARTS, tppCycle, pdfFile );
        }
    }

    protected void getChart( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        ArrayList<String> pdfNames = intent.getStringArrayListExtra( PDF_NAMES );

        File cycleDir = new File( DTPP_DIR, tppCycle );

        for ( String pdfName : pdfNames ) {
            File pdfFile = new File( cycleDir, pdfName );
            if ( !pdfFile.exists() ) {
                downloadChart( tppCycle, pdfFile );
            }
            sendResult( ACTION_GET_CHART, tppCycle, pdfFile );
        }
    }

    protected void deleteCharts( Intent intent ) {
        String tppCycle = intent.getStringExtra( TPP_CYCLE );
        ArrayList<String> pdfNames = intent.getStringArrayListExtra( PDF_NAMES );

        File cycleDir = new File( DTPP_DIR, tppCycle );

        for ( String pdfName : pdfNames ) {
            File pdfFile = new File( cycleDir, pdfName );
            if ( pdfFile.exists() ) {
                pdfFile.delete();
            }
            sendResult( ACTION_CHECK_CHARTS, tppCycle, pdfFile );
        }
    }

    protected void sendResult( String action, String tppCycle, File pdfFile ) {
        Intent result = new Intent();
        result.setAction( action );
        result.putExtra( TPP_CYCLE, tppCycle );
        result.putExtra( PDF_NAME, pdfFile.getName() );
        if ( pdfFile.exists() ) {
            result.putExtra( PDF_PATH, pdfFile.getAbsolutePath() );
        }
        sendBroadcast( result );
    }

    protected void downloadChart( String tppCycle, File pdfFile ) {
        String path;
        if ( pdfFile.getName().equals( "legendAD.pdf" ) ) {
            path = "/content/aeronav/online/pdf_files/legendAD.pdf";
        } else {
            path = String.format( "/d-tpp/%s/%s", tppCycle, pdfFile.getName() );
        }

        NetworkUtils.doHttpGet( this, mHttpClient, mTarget, path, pdfFile );
    }

    protected void cleanupOldCycles() {
        File[] cycles = DTPP_DIR.listFiles();
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
