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

import com.nadmm.airports.utils.GeoUtils;
import com.nadmm.airports.utils.UiUtils;

public class PirepService extends NoaaService {

    private final String PIREP_QUERY =
            "dataSource=aircraftreports&requestType=retrieve&format=xml&compression=gzip"
            + "&hoursBeforeNow=%d&radialDistance=%.0f;%.2f,%.2f";
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
        if ( !intent.getAction().equals( ACTION_GET_PIREP ) ) {
            return;
        }

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
        Intent result = new Intent();
        result.setAction( ACTION_GET_PIREP );
        result.putExtra( STATION_ID, stationId );
        result.putExtra( RESULT, pirep );
        sendBroadcast( result );
    }

    protected boolean fetchPirep( int hours, Location location, int radiusNM, File xml ) {
        try {
            String query = String.format( PIREP_QUERY, hours,
                    radiusNM*GeoUtils.STATUTE_MILES_PER_NAUTICAL_MILES,
                    location.getLongitude(), location.getLatitude() );
            return fetchFromNoaa( query, xml, true );
        } catch ( Exception e ) {
            UiUtils.showToast( this, "Unable to fetch PIREP: "+e.getMessage() );
        }
        return false;
    }

}
