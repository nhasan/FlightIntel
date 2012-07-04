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
import com.nadmm.airports.utils.SystemUtils;

public class DtppService extends AeroNavService {

    private static final String DTPP = "dtpp";
    private static final File DTPP_DIR = SystemUtils.getExternalDir( DTPP );

    public DtppService() {
        super( DTPP );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_CHARTS ) ) {
            getCharts( intent );
        } else if ( action.equals( ACTION_CHECK_CHARTS ) ) {
            getCharts( intent );
        } else if ( action.equals( ACTION_DELETE_CHARTS ) ) {
            deleteCharts( intent );
        }
    }

    protected void getCharts( Intent intent ) {
        String action = intent.getAction();

        String tppCycle = intent.getStringExtra( CYCLE_NAME );
        String tppVolume = intent.getStringExtra( TPP_VOLUME );
        ArrayList<String> pdfNames = intent.getStringArrayListExtra( PDF_NAMES );

        File dir = getVolumeDir( tppCycle, tppVolume );

        for ( String pdfName : pdfNames ) {
            File pdfFile = new File( dir, pdfName );
            if ( !pdfFile.exists() ) {
                if ( action.equals( ACTION_CHECK_CHARTS ) ) {
                    boolean download = intent.getBooleanExtra( DOWNLOAD_IF_MISSING, false );
                    if ( !download ) {
                        continue;
                    }
                }
                downloadChart( tppCycle, pdfFile );
            }
            sendResult( action, tppCycle, pdfFile );
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

    protected File getVolumeDir( String cycle, String tppVolume ) {
        File dir = new File( getCycleDir( cycle ), tppVolume );
        if ( !dir.exists() ) {
            dir.mkdir();
        }
        return dir;
    }

    public static int getChartsCount( String cycle, String tppVolume ) {
        File dir = new File( DTPP_DIR, cycle+"/"+tppVolume );
        String[] files = dir.list();
        return files != null? files.length : 0;
    }

}
