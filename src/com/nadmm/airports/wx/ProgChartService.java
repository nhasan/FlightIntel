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

package com.nadmm.airports.wx;

import java.io.File;
import java.net.URI;

import org.apache.http.client.utils.URIUtils;

import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;

import android.content.Intent;
import android.text.format.DateUtils;

public class ProgChartService extends NoaaService {

    private final String PROGCHART_IMAGE_NAME = "prog%s.gif";
    private final String PROGCHART_IMAGE_ZOOM_NAME = "prog%s_zoom.gif";
    private final String PROGCHART_IMAGE_QUERY = "/data/progs/";
    private final String PROGCHART_IMAGE_ZOOM_QUERY = "/data/progs/zoom/";
    private final long PROGCHART_CACHE_MAX_AGE = 60*DateUtils.MINUTE_IN_MILLIS;

    public ProgChartService() {
        super( "progchart" );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Remove any old files from cache first
        cleanupCache( DATA_DIR, PROGCHART_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_PROGCHART ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_IMAGE ) ) {
                String code = intent.getStringExtra( IMAGE_CODE );
                boolean hiRes = getResources().getBoolean( R.bool.WxHiResImages );
                String imageName = String.format(
                        hiRes? PROGCHART_IMAGE_ZOOM_NAME : PROGCHART_IMAGE_NAME,
                        code );
                File image = new File( DATA_DIR, imageName );
                if ( !image.exists() ) {
                    try {
                        String query = hiRes? PROGCHART_IMAGE_ZOOM_QUERY : PROGCHART_IMAGE_QUERY;
                        query += imageName;
                        URI uri = URIUtils.createURI( "http", NOAA_HOST, 80, query, null, null );
                        fetchFromNoaa( uri, image, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch PROGCHART image: "
                                +e.getMessage() );
                    }
                }

                // Broadcast the result
                Intent result = makeIntent( action, type );
                result.putExtra( IMAGE_CODE, code );
                if ( image.exists() ) {
                    result.putExtra( RESULT, image.getAbsolutePath() );
                }
                sendBroadcast( result );
            }
        }
    }

}
