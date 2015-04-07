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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.http.client.utils.URIUtils;

import android.content.Intent;

import com.nadmm.airports.utils.UiUtils;

public class TfrService extends TfrServiceBase {

    public static final String TFR_HOST = "www.jepptech.com";
    public static final String TFR_PATH = "/tfr/Query.asp";
    public static final String TFR_QUERY = "UserID=Public&DeletedMinutes=180&ExpiredMinutes=0";

    public static final String ACTION_GET_TFR_LIST = "flightintel.tfr.action.GET_TFR_LIST";
    public static final String FORCE_REFRESH = "FORCE_REFRESH";
    public static final String TFR_LIST = "TFR_LIST";

    private static final String TFR_CACHE_NAME = "tfr.xml";

    private final TfrParser mParser;

    public TfrService() {
        super();
        mParser = new TfrParser();
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_TFR_LIST ) ) {
            getTfrList( intent );
        }
    }

    private void getTfrList( Intent intent ) {
        boolean force = intent.getBooleanExtra( FORCE_REFRESH, false );

        File tfrFile = getFile( TFR_CACHE_NAME );
        if ( !tfrFile.exists() || force ) {
            URI uri;
            try {
                uri = URIUtils.createURI( "http", TFR_HOST, 80, TFR_PATH, TFR_QUERY, null );
                fetch( uri, tfrFile );
            } catch ( URISyntaxException e ) {
                UiUtils.showToast( this, "TFR: "+e.getMessage() );
            }
        }

        TfrList tfrList = new TfrList();
        mParser.parse( tfrFile, tfrList );
        Collections.sort( tfrList.entries );

        Intent result = makeResultIntent( intent.getAction() );
        result.putExtra( TFR_LIST, tfrList );
        sendResultIntent( result );
    }

}
