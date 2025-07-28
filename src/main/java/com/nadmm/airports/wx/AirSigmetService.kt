/*
 * FlightIntel for Pilots
 *
 * Copyright 2012-2021 Nadeem Hasan <nhasan@nadmm.com>
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

import android.content.Intent;
import android.text.format.DateUtils;

import com.nadmm.airports.utils.UiUtils;

import java.io.File;
import java.util.Locale;

public class AirSigmetService extends NoaaService {

    private static final long AIRSIGMET_CACHE_MAX_AGE = 30*DateUtils.MINUTE_IN_MILLIS;

    private final AirSigmetParser mParser;

    public AirSigmetService() {
        super( "airsigmet", AIRSIGMET_CACHE_MAX_AGE );
        mParser = new AirSigmetParser();
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_AIRSIGMET ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_TEXT ) ) {
                String stationId = intent.getStringExtra( STATION_ID );
                double[] box = intent.getDoubleArrayExtra( COORDS_BOX );
                int hours = intent.getIntExtra( HOURS_BEFORE, 3 );
                boolean cacheOnly = intent.getBooleanExtra( CACHE_ONLY, false );
                boolean forceRefresh = intent.getBooleanExtra( FORCE_REFRESH, false );

                File xmlFile = getDataFile( "AIRSIGMET_"+stationId+".xml" );
                File objFile = getDataFile( "AIRSIGMET_"+stationId+".obj" );
                AirSigmet airSigmet;

                if ( forceRefresh || ( !cacheOnly && !xmlFile.exists() ) ) {
                    try {
                        String AIRSIGMET_TEXT_QUERY = "datasource=airsigmets"
                                + "&requesttype=retrieve&format=xml"
                                + "&hoursBeforeNow=%d&minLat=%.2f&maxLat=%.2f"
                                + "&minLon=%.2f&maxLon=%.2f";
                        String query = String.format( Locale.US, AIRSIGMET_TEXT_QUERY,
                                hours, box[ 0 ], box[ 1 ], box[ 2 ], box[ 3 ] );
                        fetchFromNoaa( query, xmlFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch AirSigmet: "
                                +e.getMessage() );
                    }
                }

                if ( objFile.exists() ) {
                    airSigmet = (AirSigmet) readObject( objFile );
                } else if ( xmlFile.exists() ) {
                    airSigmet = new AirSigmet();
                    mParser.parse( xmlFile, airSigmet );
                    writeObject( airSigmet, objFile );
                } else {
                    airSigmet = new AirSigmet();
                }

                // Broadcast the result
                sendSerializableResultIntent( action, stationId, airSigmet );
            } else if ( type.equals( TYPE_IMAGE ) ) {
                String code = intent.getStringExtra( IMAGE_CODE );
                 String imageName = String.format( "sigmet_%s.gif", code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        String path = "/data/products/sigmet/";
                        path += imageName;
                        fetchFromNoaa( path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch AirSigmet image: "
                                +e.getMessage() );
                    }
                }

                // Broadcast the result
                sendImageResultIntent( action, code, imageFile );
            }
        }
    }

}
