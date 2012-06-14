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
import android.location.Location;
import android.text.format.DateUtils;

import com.nadmm.airports.R;
import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.UiUtils;

public class PirepService extends NoaaService {

    private final String PIREP_IMAGE_NAME = "pireps_%s.gif";
    private final String PIREP_IMAGE_ZOOM_NAME = "pireps_%s_zoom.gif";
    private final String PIREP_TEXT_QUERY =
            "dataSource=aircraftreports&requestType=retrieve&format=xml&compression=gzip"
            + "&hoursBeforeNow=%d&radialDistance=%.0f;%.2f,%.2f";
    private final String PIREP_IMAGE_PATH = "/data/pireps/";
    private final String PIREP_IMAGE_ZOOM_PATH = "/data/pireps/zoom/";

    private static final long PIREP_CACHE_MAX_AGE = 1*DateUtils.HOUR_IN_MILLIS;

    private PirepParser mParser;

    public PirepService() {
        super( "pirep", PIREP_CACHE_MAX_AGE );
        mParser = new PirepParser();
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();
        if ( action.equals( ACTION_GET_PIREP ) ) {
            String type = intent.getStringExtra( TYPE );
            if ( type.equals( TYPE_TEXT ) ) {
                // Get request parameters
                String stationId = intent.getStringExtra( STATION_ID );
                int radiusNM = intent.getIntExtra( RADIUS_NM, 50 );
                int hours = intent.getIntExtra( HOURS_BEFORE, 3 );
                Location location = intent.getParcelableExtra( LOCATION );
                boolean cacheOnly = intent.getBooleanExtra( CACHE_ONLY, false );
                boolean forceRefresh = intent.getBooleanExtra( FORCE_REFRESH, false );

                File xml = getDataFile( "PIREP_"+stationId+".xml" );

                if ( forceRefresh || ( !cacheOnly && !xml.exists() ) ) {
                    try {
                        String query = String.format( PIREP_TEXT_QUERY, hours,
                                radiusNM*GeoUtils.STATUTE_MILES_PER_NAUTICAL_MILES,
                                location.getLongitude(), location.getLatitude() );
                        fetchFromNoaa( query, xml, true );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch PIREP: "+e.getMessage() );
                    }
                }

                Pirep pirep = new Pirep();

                if ( xml.exists() ) {
                    mParser.parse( xml, pirep, location, radiusNM );
                }

                // Broadcast the result
                sendResultIntent( action, stationId, pirep );
            } else if ( type.equals( TYPE_IMAGE ) ) {
                boolean hiRes = getResources().getBoolean( R.bool.WxHiResImages );
                String code = intent.getStringExtra( IMAGE_CODE );
                String imageName = String.format(
                        hiRes? PIREP_IMAGE_ZOOM_NAME : PIREP_IMAGE_NAME,
                        code );
                File imageFile = getDataFile( imageName );
                if ( !imageFile.exists() ) {
                    try {
                        String path = hiRes? PIREP_IMAGE_ZOOM_PATH : PIREP_IMAGE_PATH;
                        path += imageName;
                        fetchFromNoaa( path, null, imageFile, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch PIREP image: "+e.getMessage() );
                    }
                }

                // Broadcast the result
                sendResultIntent( action, code, imageFile );
            }
        }
    }

}
