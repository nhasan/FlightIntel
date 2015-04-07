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

import android.content.Intent;

import java.io.File;

public class DafdService extends AeroNavService {

    public DafdService() {
        super( "dafd" );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_AFD ) ) {
            getAfd( intent );
        } else if ( action.equals( ACTION_CHECK_AFD ) ) {
            checkAfd( intent );
        }
    }

    protected void getAfd( Intent intent ) {
        String afdCycle = intent.getStringExtra( CYCLE_NAME );
        String pdfName = intent.getStringExtra( PDF_NAME );

        File cycleDir = getCycleDir( afdCycle );
        File pdfFile = new File( cycleDir, pdfName );
        if ( !pdfFile.exists() ) {
            String path = String.format( "/afd/%s/%s", afdCycle, pdfName );
            fetch( path, pdfFile );
        }

        sendResult( ACTION_GET_AFD, afdCycle, pdfFile );
    }

    protected void checkAfd( Intent intent ) {
        String afdCycle = intent.getStringExtra( CYCLE_NAME );
        String pdfName = intent.getStringExtra( PDF_NAME );

        File cycleDir = getCycleDir( afdCycle );
        File pdfFile = new File( cycleDir, pdfName );

        sendResult( ACTION_CHECK_AFD, afdCycle, pdfFile );
    }

}
