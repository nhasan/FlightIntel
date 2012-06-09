/*
 * FlightIntel for Pilots
 *
 * Copyright 2011-2012 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.text.format.DateUtils;

import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;

public class MetarService extends NoaaService {

    private final String METAR_IMAGE_NAME = "metar_%s.gif";
    private final String METAR_TEXT_QUERY = "datasource=metars&requesttype=retrieve"
    		+ "&format=xml&compression=gzip&hoursBeforeNow=%d&mostRecent=true&stationString=%s";
    private final String METAR_IMAGE_QUERY = "tools/weatherproducts/metars/default/"
    		+ "loadImage/region/%s/product/METARs/zoom/%s";
    private final long METAR_CACHE_MAX_AGE = 30*DateUtils.MINUTE_IN_MILLIS;

    private MetarParser mParser;

    public MetarService() {
        super( "metar" );
        mParser = new MetarParser();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Remove any old METAR files from cache first
        cleanupCache( DATA_DIR, METAR_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_METAR ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_TEXT ) ) {
                // Get request parameters
                String stationId = intent.getStringExtra( STATION_ID );
                int hours = intent.getIntExtra( HOURS_BEFORE, 3 );
                boolean cacheOnly = intent.getBooleanExtra( CACHE_ONLY, false );
                boolean forceRefresh = intent.getBooleanExtra( FORCE_REFRESH, false );

                File xml = new File( DATA_DIR, "METAR_"+stationId+".xml" );

                if ( forceRefresh || ( !cacheOnly && !xml.exists() ) ) {
                    try {
                        String query = String.format( METAR_TEXT_QUERY, hours, stationId );
                        fetchFromNoaa( query, xml, true );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch METAR: "+e.getMessage() );
                    }
                }

                Metar metar = new Metar();

                if ( xml.exists() ) {
                    metar.stationId = stationId;
                    mParser.parse( xml, metar );
                }

                // Broadcast the result
                Intent result = new Intent();
                result.setAction( ACTION_GET_METAR );
                result.putExtra( STATION_ID, stationId );
                result.putExtra( RESULT, metar );
                sendBroadcast( result );
            } else if ( type.equals( TYPE_IMAGE ) ) {
                String code = intent.getStringExtra( IMAGE_CODE );
                String imageName = String.format( METAR_IMAGE_NAME, code );
                File image = new File( DATA_DIR, imageName );
                if ( !image.exists() ) {
                    try {
                        boolean hiRes = getResources().getBoolean( R.bool.WxHiResImages );
                        String query = String.format( METAR_IMAGE_QUERY, code,
                                hiRes? "true" : "false" );
                        URI uri = URIUtils.createURI( "http", NOAA_HOST, 80, query, null, null );
                        fetchFromNoaa( uri, image, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch image: "+e.getMessage() );
                    }
                }

                // Broadcast the result
                Intent result = new Intent();
                result.setAction( action );
                result.putExtra( TYPE, TYPE_IMAGE );
                result.putExtra( IMAGE_CODE, code );
                if ( image.exists() ) {
                    result.putExtra( RESULT, image.getAbsolutePath() );
                }
                sendBroadcast( result );
            }
        }
    }

}
