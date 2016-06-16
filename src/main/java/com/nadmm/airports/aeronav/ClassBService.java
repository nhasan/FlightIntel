/*
 * FlightIntel for Pilots
 *
 * Copyright 2016 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;

import com.nadmm.airports.data.DatabaseManager.Airports;
import com.nadmm.airports.utils.ClassBUtils;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.UiUtils;

import java.io.File;
import java.util.Date;

public class ClassBService extends AeroNavService {

    public static final String ACTION_GET_CLASSB_GRAPHIC
            = "flightintel.intent.action.ACTION_GET_CLASSB_GRAPHIC";
    public static final String PDF_PATH = "PDF_PATH";

    private static final String FAA_HOST = "www.faa.gov";
    private static final String CLASS_B_PATH =
            "/air_traffic/flight_info/aeronav/digital_products/vfr_class_b/media";

    public ClassBService() {
        super( "classb" );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        cleanupCache( getServiceDataDir(), DateUtils.DAY_IN_MILLIS );
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

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_CLASSB_GRAPHIC ) ) {
            getClassBGraphic( intent );
        }
    }

    private void getClassBGraphic( Intent intent ) {
        String facility = intent.getStringExtra( Airports.FAA_CODE );
        String classBFilename = ClassBUtils.getClassBFilename( facility );

        File pdfFile = new File( getServiceDataDir(), classBFilename );
        if ( !pdfFile.exists() ) {
            try {
                NetworkUtils.doHttpGet( this, FAA_HOST, CLASS_B_PATH+"/"+classBFilename, pdfFile );
            } catch ( Exception e ) {
                UiUtils.showToast( this, "Error: " + e.getMessage() );
            }
        }

        Bundle extras = new Bundle();
        extras.putString( Airports.FAA_CODE, facility );
        extras.putString( PDF_PATH, pdfFile.getAbsolutePath() );
        sendResult( intent.getAction(), extras );
    }

}
