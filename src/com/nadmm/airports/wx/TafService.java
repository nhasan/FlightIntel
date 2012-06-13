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

import android.content.Intent;
import android.text.format.DateUtils;

import com.nadmm.airports.R;
import com.nadmm.airports.utils.UiUtils;

public class TafService extends NoaaService {

    private final String TAF_IMAGE_NAME = "taf_%s.gif";
    private final String TAF_TEXT_QUERY = "dataSource=tafs&requestType=retrieve"
            +"&format=xml&compression=gzip&hoursBeforeNow=%d&mostRecent=true&stationString=%s";
    private final String TAF_IMAGE_PATH = "/tools/weatherproducts/tafs/default/"
            + "loadImage/region/%s/product/prevail/zoom/%s";

    private static final long TAF_CACHE_MAX_AGE = 2*DateUtils.HOUR_IN_MILLIS;

    protected TafParser mParser;

    public TafService() {
        super( "taf", TAF_CACHE_MAX_AGE );
        mParser = new TafParser();
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_TAF ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_TEXT ) ) {
                // Get request parameters
                String stationId = intent.getStringExtra( STATION_ID );
                int hours = intent.getIntExtra( HOURS_BEFORE, 6 );
                boolean cacheOnly = intent.getBooleanExtra( CACHE_ONLY, false );
                boolean forceRefresh = intent.getBooleanExtra( FORCE_REFRESH, false );

                File xml = getDataFile( "TAF_"+stationId+".xml" );

                if ( forceRefresh || ( !cacheOnly && !xml.exists() ) ) {
                    fetchTaf( stationId, hours, xml );
                }

                Taf taf = new Taf();

                if ( xml.exists() ) {
                    taf.stationId = stationId;
                    mParser.parse( xml, taf );
                }

                // Broadcast the result
                Intent result = makeIntent( action, type );
                result.putExtra( STATION_ID, stationId );
                result.putExtra( RESULT, taf );
                sendBroadcast( result );
            } else if ( type.equals( TYPE_IMAGE ) ) {
                String code = intent.getStringExtra( IMAGE_CODE );
                String imageName = String.format( TAF_IMAGE_NAME, code );
                File image = getDataFile( imageName );
                if ( !image.exists() ) {
                    try {
                        boolean hiRes = getResources().getBoolean( R.bool.WxHiResImages );
                        String path = String.format( TAF_IMAGE_PATH, code,
                                hiRes? "true" : "false" );
                        fetchFromNoaa( path, null, image, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch TAF image: "+e.getMessage() );
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

    protected boolean fetchTaf( String stationId, int hours, File xml ) {
        try {
            String query = String.format( TAF_TEXT_QUERY, hours, stationId );
            return fetchFromNoaa( query, xml, true );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "Unable to fetch TAF: "+e.getMessage() );
        }
        return false;
    }

}
