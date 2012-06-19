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

import android.content.Intent;
import android.text.format.DateUtils;

import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;

public class MetarService extends NoaaService {

    private final String METAR_IMAGE_NAME = "metar_%s.gif";
    private final String METAR_TEXT_QUERY = "datasource=metars&requesttype=retrieve"
    		+ "&format=xml&compression=gzip&hoursBeforeNow=%d&mostRecent=true&stationString=%s";
    private final String METAR_IMAGE_PATH = "/tools/weatherproducts/metars/default/"
    		+ "loadImage/region/%s/product/METARs/zoom/%s";

    private static final long METAR_CACHE_MAX_AGE = 30*DateUtils.MINUTE_IN_MILLIS;

    private MetarParser mParser;

    public MetarService() {
        super( "metar", METAR_CACHE_MAX_AGE );
        mParser = new MetarParser();
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

                File xmlFile = getDataFile( "METAR_"+stationId+".xml" );
                File objFile = getDataFile( "METAR_"+stationId+".obj" );
                Metar metar = null;

                if ( forceRefresh || ( !cacheOnly && !xmlFile.exists() ) ) {
                    try {
                        String query = String.format( METAR_TEXT_QUERY, hours, stationId );
                        fetchFromNoaa( query, xmlFile, true );
                        metar = parse( stationId, xmlFile, objFile );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch METAR: "+e.getMessage() );
                    }
                }

                if ( objFile.exists() ) {
                    metar = (Metar) readObject( objFile );
                } else if ( xmlFile.exists() ) {
                    metar = new Metar();
                    metar.stationId = stationId;
                    mParser.parse( xmlFile, metar );
                    writeObject( metar, objFile );
                } else {
                    metar = new Metar();
                }

                // Broadcast the result
                sendResultIntent( action, stationId, metar );
            } else if ( type.equals( TYPE_IMAGE ) ) {
                String code = intent.getStringExtra( IMAGE_CODE );
                String imageName = String.format( METAR_IMAGE_NAME, code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        boolean hiRes = getResources().getBoolean( R.bool.WxHiResImages );
                        String path = String.format( METAR_IMAGE_PATH, code,
                                hiRes? "true" : "false" );
                        fetchFromNoaa( path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch METAR image: "+e.getMessage() );
                    }
                }

                // Broadcast the result
                sendResultIntent( action, code, imageFile );
            }
        }
    }

    private Metar parse( String stationId, File xmlFile, File objFile ) {
        Metar metar = new Metar();
        if ( xmlFile.exists() ) {
            metar.stationId = stationId;
            mParser.parse( xmlFile, metar );
            writeObject( metar, objFile );
        }
        return metar;
    }

}
