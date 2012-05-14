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
import android.util.Log;

import com.nadmm.airports.utils.UiUtils;

public class AirSigmetService extends NoaaService {

    //http://weather.aero/data/airmets/airmets_ALL.gif
    private final String AIRSIGMET_TEXT_QUERY =
            "datasource=airsigmets&requesttype=retrieve&format=xml&compression=gzip"
            + "&hoursBeforeNow=%d&minLat=%.2f&maxLat=%.2f&minLon=%.2f&maxLon=%.2f";
    private final String AIRSIGMET_MAP_PATH = "/data/airmets";
    private final String AIRSIGMET_MAP_NAME = "airmets_ALL.gif";
    private final long AIRSIGMET_CACHE_MAX_AGE = 60*DateUtils.MINUTE_IN_MILLIS;

    AirSigmetParser mParser;

    public AirSigmetService() {
        super( "airsigmet" );
        mParser = new AirSigmetParser();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Remove any old files from cache first
        cleanupCache( DATA_DIR, AIRSIGMET_CACHE_MAX_AGE );
    }

    @Override
    protected void onHandleIntent( Intent intent ) {
        String action = intent.getAction();

        boolean cacheOnly = intent.getBooleanExtra( CACHE_ONLY, false );
        boolean forceRefresh = intent.getBooleanExtra( FORCE_REFRESH, false );

        Log.d( "ACTION", action );

        if ( action.equals( ACTION_GET_AIRSIGMET_TEXT ) ) {
            String stationId = intent.getStringExtra( STATION_ID );
            double[] box = intent.getDoubleArrayExtra( COORDS_BOX );
            int hours = intent.getIntExtra( HOURS_BEFORE, 3 );

            File xml = new File( DATA_DIR, "AIRSIGMET_"+stationId+".xml" );

            if ( forceRefresh || ( !cacheOnly && !xml.exists() ) ) {
                try {
                    String query = String.format( AIRSIGMET_TEXT_QUERY,
                            hours, box[ 0 ], box[ 1 ], box[ 2 ], box[ 3 ] );
                    fetchFromNoaa( query, xml, true );
                } catch ( Exception e ) {
                    UiUtils.showToast( this, "Unable to fetch AirSigmet: "+e.getMessage() );
                }
            }

            AirSigmet airSigmet = new AirSigmet();

            if ( xml.exists() ) {
                mParser.parse( xml, airSigmet );
            }

            // Broadcast the result
            Intent result = new Intent();
            result.setAction( action );
            result.putExtra( STATION_ID, stationId );
            result.putExtra( RESULT, airSigmet );
            sendBroadcast( result );
        } else if ( action.equals( ACTION_GET_AIRSIGMET_MAP ) ) {
            File map = new File( DATA_DIR, AIRSIGMET_MAP_NAME );

            if ( forceRefresh || !map.exists() ) {
                try {
                    String path = AIRSIGMET_MAP_PATH+"/"+AIRSIGMET_MAP_NAME;
                    Log.d( "FETCHING", path );
                    URI uri = URIUtils.createURI( "http", NOAA_HOST, 80, path, null, null );
                    Log.d( "FETCHING", uri.toString() );
                    fetchFromNoaa( uri, map, false );
                } catch ( Exception e ) {
                    UiUtils.showToast( this, "Unable to fetch AirSigmet: "+e.getMessage() );
                }
            }

            // Broadcast the result
            Intent result = new Intent();
            result.setAction( action );
            if ( map.exists() ) {
                Log.d( "MAP", map.getAbsolutePath() );
                result.putExtra( RESULT, map.getAbsolutePath() );
            }
            sendBroadcast( result );
        }
    }

}
