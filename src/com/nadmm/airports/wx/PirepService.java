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
    private final String PIREP_IMAGE_QUERY = "/data/pireps/";
    private final String PIREP_IMAGE_ZOOM_QUERY = "/data/pireps/zoom/";
    private final long PIREP_CACHE_MAX_AGE = 1*DateUtils.HOUR_IN_MILLIS;

    private PirepParser mParser;

    public PirepService() {
        super( "pirep" );
        mParser = new PirepParser();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Remove any old files from cache first
        cleanupCache( DATA_DIR, PIREP_CACHE_MAX_AGE );
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

                File xml = new File( DATA_DIR, "PIREP_"+stationId+".xml" );

                if ( forceRefresh || ( !cacheOnly && !xml.exists() ) ) {
                    fetchPirep( hours, location, radiusNM, xml );
                }

                Pirep pirep = new Pirep();

                if ( xml.exists() ) {
                    mParser.parse( xml, pirep, location, radiusNM );
                }

                // Broadcast the result
                Intent result = makeIntent( action, type );
                result.putExtra( STATION_ID, stationId );
                result.putExtra( RESULT, pirep );
                sendBroadcast( result );
            } else if ( type.equals( TYPE_IMAGE ) ) {
                boolean hiRes = getResources().getBoolean( R.bool.WxHiResImages );
                String code = intent.getStringExtra( IMAGE_CODE );
                String imageName = String.format(
                        hiRes? PIREP_IMAGE_ZOOM_NAME : PIREP_IMAGE_NAME,
                        code );
                File image = new File( DATA_DIR, imageName );
                if ( !image.exists() ) {
                    try {
                        String query = hiRes? PIREP_IMAGE_ZOOM_QUERY : PIREP_IMAGE_QUERY;
                        query += imageName;
                        URI uri = URIUtils.createURI( "http", NOAA_HOST, 80, query, null, null );
                        fetchFromNoaa( uri, image, false );
                    } catch ( Exception e ) {
                        UiUtils.showToast( this, "Unable to fetch PIREP image: "+e.getMessage() );
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

    protected boolean fetchPirep( int hours, Location location, int radiusNM, File xml ) {
        try {
            String query = String.format( PIREP_TEXT_QUERY, hours,
                    radiusNM*GeoUtils.STATUTE_MILES_PER_NAUTICAL_MILES,
                    location.getLongitude(), location.getLatitude() );
            return fetchFromNoaa( query, xml, true );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "Unable to fetch PIREP: "+e.getMessage() );
        }
        return false;
    }

}
