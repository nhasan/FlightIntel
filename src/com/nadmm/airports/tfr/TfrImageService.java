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

package com.nadmm.airports.tfr;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIUtils;

import android.content.Intent;

import com.nadmm.airports.tfr.TfrList.Tfr;
import com.nadmm.airports.utils.UiUtils;

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
            URI uri;
            try {
                uri = URIUtils.createURI( "http", TFR_HOST, 80, TFR_PATH+"/"+name, null, null );
                fetch( uri, imageFile );
            } catch ( URISyntaxException e ) {
                UiUtils.showToast( this, "TFR: "+e.getMessage() );
            }
        }

        Intent result = makeResultIntent( intent.getAction() );
        try {
            if ( imageFile.exists() ) {
                result.putExtra( TFR_IMAGE_PATH, imageFile.getCanonicalPath() );
                sendResultIntent( result );
            } else {
                UiUtils.showToast( this, "TFR: Unable to fetch graphic for TFR "+tfr.notamId );
            }
        } catch ( IOException e ) {
            UiUtils.showToast( this, "TFR: "+e.getMessage() );
        }
    }

}
