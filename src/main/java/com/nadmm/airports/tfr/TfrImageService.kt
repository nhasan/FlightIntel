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

package com.nadmm.airports.tfr;

import android.content.Intent;

import com.nadmm.airports.tfr.TfrList.Tfr;
import com.nadmm.airports.utils.NetworkUtils;
import com.nadmm.airports.utils.UiUtils;

import java.io.File;

public class TfrImageService extends TfrServiceBase {

    //http://tfr.faa.gov/save_maps/sect_2_8597.gif
    public static final String TFR_HOST = "tfr.faa.gov";
    public static final String TFR_PATH = "/save_maps";

    public static final String ACTION_GET_TFR_IMAGE = "flightintel.tfr.action.GET_TFR_IMAGE";
    public static final String TFR_ENTRY = "TFR_ENTRY";
    public static final String TFR_IMAGE_PATH = "TFR_IMAGE_PATH";

    public TfrImageService() {
        super();
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_TFR_IMAGE ) ) {
            getTfrImage( intent );
        }
    }

    private void getTfrImage( Intent intent ) {
        Tfr tfr = (Tfr) intent.getSerializableExtra( TFR_ENTRY );
        String notamId = tfr.notamId;

        int start = notamId.indexOf( ' ' );
        if ( start > 0 ) {
            notamId = notamId.substring( start+1 );
        }
        notamId = notamId.replace( "/", "_" );

        String name = "sect_"+notamId+".gif";
        File imageFile = getFile( name );

        if ( !imageFile.exists() ) {
            fetch( TFR_HOST, TFR_PATH+"/"+name, null, imageFile );
        }

        Intent result = makeResultIntent( intent.getAction() );
        if ( imageFile.exists() ) {
            result.putExtra( TFR_IMAGE_PATH, imageFile.getAbsolutePath() );
        }
        sendResultIntent( result );
    }

    protected void fetch( String host, String path, String query, File tfrFile ) {
        try {
            NetworkUtils.doHttpsGet( this, host, path, query, tfrFile, null, null, null );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "TFR: "+e.getMessage() );
        }
    }

}
