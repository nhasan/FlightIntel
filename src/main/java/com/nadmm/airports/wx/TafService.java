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

public class TafService extends NoaaService {

    private static final long TAF_CACHE_MAX_AGE = 30*DateUtils.MINUTE_IN_MILLIS;

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

                Taf taf = null;
                File objFile = getDataFile( "TAF_"+stationId+".obj" );

                if ( forceRefresh || ( !cacheOnly && !objFile.exists() ) ) {
                    File tmpFile = null;
                    try {
                        tmpFile = File.createTempFile( "taf", null );
                        String TAF_TEXT_QUERY = "dataSource=tafs&requestType=retrieve"
                                + "&format=xml&hoursBeforeNow=%d&mostRecent=true&stationString=%s";
                        String query = String.format( Locale.US, TAF_TEXT_QUERY, hours, stationId );
                        fetchFromNoaa( query, tmpFile, false );
                        taf = mParser.parse( tmpFile );
                        writeObject( taf, objFile );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch TAF: "+e.getMessage() );
                    } finally {
                        if ( tmpFile != null ) {
                            tmpFile.delete();
                        }
                    }
                }

                if ( taf == null ) {
                    if ( objFile.exists() ) {
                        taf = (Taf) readObject( objFile );
                    } else {
                        taf = new Taf();
                    }
                }

                // Broadcast the result
                sendSerializableResultIntent( action, stationId, taf );
            } else if ( type.equals( TYPE_IMAGE ) ) {
                String imgType = intent.getStringExtra( IMAGE_TYPE );
                String code = intent.getStringExtra( IMAGE_CODE ).toLowerCase();
                String imageName = String.format( "%s_taf_%s_prevail.gif", imgType, code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    String path = "/data/products/taf/";
                    try {
                        path += imageName;
                        fetchFromNoaa( path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch TAF image: "
                                +path+" "
                                +e.getMessage() );
                    }
                }

                // Broadcast the result
                sendImageResultIntent( action, code, imageFile );
            }
        }
    }

}
