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

import android.content.Intent;

import com.nadmm.airports.utils.NetworkUtils;

public class DtppService extends AeroNavService {

    public DtppService() {
        super( "dtpp" );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
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
        String tppCycle = intent.getStringExtra( CYCLE_NAME );
        ArrayList<String> pdfNames = intent.getStringArrayListExtra( PDF_NAMES );
        boolean download = intent.getBooleanExtra( DOWNLOAD_IF_MISSING, false );

        File cycleDir = getCycleDir( tppCycle );

        for ( String pdfName : pdfNames ) {
            File pdfFile = new File( cycleDir, pdfName );
            if ( download && !pdfFile.exists() ) {
                downloadChart( tppCycle, pdfFile );
            }
            sendResult( ACTION_CHECK_CHARTS, tppCycle, pdfFile );
        }
    }

    protected void getChart( Intent intent ) {
        String tppCycle = intent.getStringExtra( CYCLE_NAME );
        ArrayList<String> pdfNames = intent.getStringArrayListExtra( PDF_NAMES );

        File cycleDir = getCycleDir( tppCycle );

        for ( String pdfName : pdfNames ) {
            File pdfFile = new File( cycleDir, pdfName );
            if ( !pdfFile.exists() ) {
                downloadChart( tppCycle, pdfFile );
            }
            sendResult( ACTION_GET_CHART, tppCycle, pdfFile );
        }
    }

    protected void deleteCharts( Intent intent ) {
        String tppCycle = intent.getStringExtra( CYCLE_NAME );
        ArrayList<String> pdfNames = intent.getStringArrayListExtra( PDF_NAMES );

        File cycleDir = getCycleDir( tppCycle );

        for ( String pdfName : pdfNames ) {
            File pdfFile = new File( cycleDir, pdfName );
            if ( pdfFile.exists() ) {
                pdfFile.delete();
            }
            sendResult( ACTION_CHECK_CHARTS, tppCycle, pdfFile );
        }
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

}
